package com.anpaiss.axsratio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var statusText: TextView
    private lateinit var permissionBtn: Button
    private lateinit var toggleBtn: Button
    private lateinit var previewBtn: Button

    private val spinners = mutableMapOf<Metric, Spinner>()
    private val slotValues = Slot.values()
    private val slotLabels = slotValues.map { it.label }

    private var previewing = false
    private val previewAutoStop = Runnable {
        previewing = false
        refresh()
    }

    private var suppressSpinnerCallbacks = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs         = Prefs(this)
        statusText    = findViewById(R.id.tv_status)
        permissionBtn = findViewById(R.id.btn_permission)
        toggleBtn     = findViewById(R.id.btn_toggle)
        previewBtn    = findViewById(R.id.btn_preview)

        bindSpinner(Metric.GEAR,    R.id.sp_gear)
        bindSpinner(Metric.HR,      R.id.sp_hr)
        bindSpinner(Metric.POWER,   R.id.sp_power)
        bindSpinner(Metric.CADENCE, R.id.sp_cadence)
        bindSpinner(Metric.SPEED,   R.id.sp_speed)
        bindSpinner(Metric.GRADE,     R.id.sp_grade)
        bindSpinner(Metric.TEMP,      R.id.sp_temp)
        bindSpinner(Metric.DIST_TURN, R.id.sp_dist_turn)

        permissionBtn.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"),
                )
            )
        }

        toggleBtn.setOnClickListener {
            if (prefs.enabled) {
                prefs.enabled = false
                OverlayService.stop(this)
            } else {
                prefs.enabled = true
                OverlayService.start(this)
            }
            refresh()
        }

        previewBtn.setOnClickListener {
            if (previewing) {
                OverlayService.stopPreview(this)
                previewBtn.removeCallbacks(previewAutoStop)
                previewing = false
            } else {
                OverlayService.preview(this)
                previewing = true
                previewBtn.postDelayed(previewAutoStop, 12_500L)
            }
            refresh()
        }
    }

    private fun bindSpinner(metric: Metric, viewId: Int) {
        val sp = findViewById<Spinner>(viewId)
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, slotLabels)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSpinnerCallbacks) return
                val newSlot = slotValues[position]
                if (prefs.slotFor(metric) == newSlot) return
                prefs.setSlotFor(metric, newSlot)
                refreshSpinners()
                refresh()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinners[metric] = sp
    }

    override fun onResume() {
        super.onResume()
        refreshSpinners()
        refresh()
    }

    private fun refreshSpinners() {
        suppressSpinnerCallbacks = true
        for ((metric, sp) in spinners) {
            val ord = prefs.slotFor(metric).ordinal
            if (sp.selectedItemPosition != ord) sp.setSelection(ord, false)
        }
        suppressSpinnerCallbacks = false
    }

    private fun refresh() {
        val canDraw = Settings.canDrawOverlays(this)
        permissionBtn.isEnabled = !canDraw
        permissionBtn.text = if (canDraw) "Overlay permission: OK" else "Grant overlay permission"

        toggleBtn.isEnabled = canDraw
        toggleBtn.text = if (prefs.enabled) "Disable overlay" else "Enable overlay"

        val anyPlaced = Metric.values().any { prefs.slotFor(it) != Slot.OFF }
        previewBtn.isEnabled = canDraw && prefs.enabled && anyPlaced
        previewBtn.text = if (previewing) "Stop preview" else "Preview"

        statusText.text = when {
            !canDraw       -> "1) Grant permission to draw over other apps."
            !prefs.enabled -> "2) Enable the overlay, then place metrics in the corners below."
            previewing     -> "Previewing all active metrics. Move this app to background."
            else           -> "Overlay active. Picking the same corner for two metrics moves the previous one to Off."
        }
    }
}
