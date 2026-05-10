package com.anpaiss.axsratio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private lateinit var karoo: KarooSystemService
    private lateinit var prefs: Prefs
    private val consumerIds = mutableListOf<String>()
    private var windowManager: WindowManager? = null

    private data class MetricView(
        val view: TextView,
        val background: GradientDrawable,
    )

    private val metricViews = mutableMapOf<Metric, MetricView>()
    private val lastValues  = mutableMapOf<Metric, Double?>()
    @Volatile private var hrZone: Int? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var previewJob: Job? = null
    private var karooConnected = false

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key.startsWith(Prefs.KEY_SLOT_PREFIX)) {
            scope.launch { rebuild() }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        prefs = Prefs(applicationContext)
        startInForeground()

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission missing — stop service")
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        karoo = KarooSystemService(applicationContext)
        karoo.connect { connected ->
            Log.i(TAG, "Karoo connected=$connected")
            karooConnected = connected
            if (connected) scope.launch { rebuild() }
        }
        prefs.sp.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun rebuild() {
        consumerIds.forEach { runCatching { karoo.removeConsumer(it) } }
        consumerIds.clear()
        metricViews.values.forEach { mv -> runCatching { windowManager?.removeView(mv.view) } }
        metricViews.clear()
        hrZone = null

        var hrPlaced = false
        for (metric in Metric.values()) {
            val slot = prefs.slotFor(metric)
            if (slot == Slot.OFF) continue
            addMetricView(metric, slot)
            if (karooConnected) subscribeMetric(metric)
            if (metric == Metric.HR) hrPlaced = true
        }
        if (hrPlaced && karooConnected) subscribeHrZone()
    }

    private fun addMetricView(metric: Metric, slot: Slot) {
        val wm = windowManager ?: return
        val tv = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            textSize = metric.textSizeSp.toFloat()
            setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
            text = "-"
        }
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpF(6f)
            setColor(Metric.COLOR_DEFAULT)
        }
        tv.background = bg

        val params = WindowManager.LayoutParams(
            dpI(metric.widthDp),
            dpI(metric.heightDp),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = slot.gravityFlags
            x = dpI(MARGIN_DP)
            y = dpI(MARGIN_DP)
        }

        wm.addView(tv, params)
        metricViews[metric] = MetricView(tv, bg)
    }

    private fun subscribeMetric(metric: Metric) {
        consumerIds += karoo.addConsumer(
            OnStreamState.StartStreaming(metric.streamType), { _ -> }, {}
        ) { event: OnStreamState ->
            val v = (event.state as? StreamState.Streaming)?.dataPoint?.singleValue
            lastValues[metric] = v
            render(metric)
        }
    }

    private fun subscribeHrZone() {
        consumerIds += karoo.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.HR_ZONE), { _ -> }, {}
        ) { event: OnStreamState ->
            hrZone = (event.state as? StreamState.Streaming)?.dataPoint?.singleValue?.toInt()
            render(Metric.HR)
        }
    }

    private fun render(metric: Metric) {
        val mv = metricViews[metric] ?: return
        val value = lastValues[metric]
        val text  = metric.format(value)
        val color = metric.backgroundColor(value, hrZone)
        mv.view.post {
            mv.view.text = text
            mv.background.setColor(color)
        }
    }

    private fun startInForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CH_ID, "AXS Ratio Overlay", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val n: Notification = NotificationCompat.Builder(this, CH_ID)
            .setContentTitle("AXS Ratio")
            .setContentText("Overlay active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREVIEW      -> startPreview()
            ACTION_STOP_PREVIEW -> stopPreview()
        }
        return START_STICKY
    }

    private fun startPreview() {
        if (metricViews.isEmpty()) return
        previewJob?.cancel()
        previewJob = scope.launch {
            for (i in 0 until PREVIEW_STEPS) {
                for (metric in metricViews.keys) {
                    lastValues[metric] = previewValueAt(metric, i)
                    if (metric == Metric.HR) hrZone = previewHrZoneAt(i)
                    render(metric)
                }
                delay(1000)
            }
        }
    }

    private fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        for (metric in metricViews.keys) {
            lastValues[metric] = null
            if (metric == Metric.HR) hrZone = null
            render(metric)
        }
    }

    private fun previewValueAt(metric: Metric, i: Int): Double = when (metric) {
        Metric.GEAR    -> (i + 1).toDouble()
        Metric.HR      -> PREVIEW_HR[i]
        Metric.POWER   -> PREVIEW_POWER[i]
        Metric.CADENCE -> PREVIEW_CADENCE[i]
        Metric.SPEED   -> PREVIEW_SPEED_KMH[i] / 3.6  // formatter expects m/s
        Metric.GRADE   -> PREVIEW_GRADE[i]
        Metric.TEMP    -> PREVIEW_TEMP[i]
    }

    private fun previewHrZoneAt(i: Int): Int = when {
        PREVIEW_HR[i] < 100 -> 1
        PREVIEW_HR[i] < 130 -> 2
        PREVIEW_HR[i] < 150 -> 3
        PREVIEW_HR[i] < 170 -> 4
        else                -> 5
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        previewJob?.cancel()
        runCatching { prefs.sp.unregisterOnSharedPreferenceChangeListener(prefsListener) }
        scope.cancel()
        consumerIds.forEach { runCatching { karoo.removeConsumer(it) } }
        consumerIds.clear()
        runCatching { karoo.disconnect() }
        metricViews.values.forEach { mv -> runCatching { windowManager?.removeView(mv.view) } }
        metricViews.clear()
        windowManager = null
        super.onDestroy()
    }

    private fun dpF(v: Float): Float = v * resources.displayMetrics.density
    private fun dpI(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG       = "AxsRatio"
        private const val CH_ID     = "axs_ratio_overlay"
        private const val NOTIF_ID  = 1
        private const val MARGIN_DP = 12

        const val ACTION_PREVIEW      = "com.anpaiss.axsratio.PREVIEW"
        const val ACTION_STOP_PREVIEW = "com.anpaiss.axsratio.STOP_PREVIEW"

        private const val PREVIEW_STEPS = 12
        private val PREVIEW_HR        = doubleArrayOf( 60.0,  80.0, 100.0, 120.0, 140.0, 155.0, 165.0, 175.0, 160.0, 140.0, 110.0,  90.0)
        private val PREVIEW_POWER     = doubleArrayOf( 50.0, 100.0, 150.0, 200.0, 250.0, 300.0, 350.0, 400.0, 350.0, 300.0, 250.0, 200.0)
        private val PREVIEW_CADENCE   = doubleArrayOf( 60.0,  70.0,  80.0,  85.0,  90.0,  95.0, 100.0, 105.0, 100.0,  95.0,  90.0,  85.0)
        private val PREVIEW_SPEED_KMH = doubleArrayOf( 10.0,  15.0,  20.0,  25.0,  30.0,  35.0,  40.0,  45.0,  40.0,  35.0,  30.0,  25.0)
        private val PREVIEW_GRADE     = doubleArrayOf(-10.0,  -8.0,  -5.0,  -2.0,   0.0,   2.0,   5.0,   8.0,  10.0,   7.0,   3.0,  -3.0)
        private val PREVIEW_TEMP      = doubleArrayOf(  0.0,   5.0,  10.0,  15.0,  18.0,  20.0,  22.0,  25.0,  28.0,  30.0,  22.0,  15.0)

        fun start(ctx: Context) {
            val intent = Intent(ctx, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, OverlayService::class.java))
        }

        fun preview(ctx: Context) {
            ctx.startService(Intent(ctx, OverlayService::class.java).setAction(ACTION_PREVIEW))
        }

        fun stopPreview(ctx: Context) {
            ctx.startService(Intent(ctx, OverlayService::class.java).setAction(ACTION_STOP_PREVIEW))
        }
    }
}
