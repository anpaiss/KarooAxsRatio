package com.anpaiss.axsratio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.StreamState

class OverlayService : Service() {

    private lateinit var karoo: KarooSystemService
    private val consumerIds = mutableListOf<String>()

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var textView: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        startInForeground()

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Permesso overlay mancante — stop service")
            stopSelf()
            return
        }

        addOverlay()

        karoo = KarooSystemService(applicationContext)
        karoo.connect { connected ->
            Log.i(TAG, "Karoo connected=$connected")
            if (!connected) return@connect

            consumerIds += karoo.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.SHIFTING_REAR_GEAR), { _ -> }, {}
            ) { event: OnStreamState ->
                val gear = (event.state as? StreamState.Streaming)?.dataPoint?.singleValue?.toInt()
                renderGear(gear)
            }
        }
    }

    private fun addOverlay() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_gear, null, false)
        val tv = view.findViewById<TextView>(R.id.tv_gear)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = MARGIN_DP
            y = MARGIN_DP
        }

        wm.addView(view, params)
        windowManager = wm
        overlayView = view
        textView = tv
        renderGear(null)
    }

    private fun renderGear(gear: Int?) {
        val tv = textView ?: return
        tv.post {
            if (gear == null || gear <= 0) {
                tv.text = "-"
                tv.setBackgroundResource(R.drawable.bg_orange)
            } else if (gear < 10) {
                tv.text = gear.toString()
                tv.setBackgroundResource(R.drawable.bg_orange)
            } else {
                tv.text = (gear % 10).toString()
                tv.setBackgroundResource(R.drawable.bg_black)
            }
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        consumerIds.forEach { runCatching { karoo.removeConsumer(it) } }
        consumerIds.clear()
        runCatching { karoo.disconnect() }
        overlayView?.let { runCatching { windowManager?.removeView(it) } }
        overlayView = null
        textView = null
        windowManager = null
        super.onDestroy()
    }

    companion object {
        private const val TAG       = "AxsRatio"
        private const val CH_ID     = "axs_ratio_overlay"
        private const val NOTIF_ID  = 1
        private const val MARGIN_DP = 12

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
    }
}
