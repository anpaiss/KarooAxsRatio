package com.anpaiss.axsratio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var statusText: TextView
    private lateinit var permissionBtn: Button
    private lateinit var toggleBtn: Button
    private lateinit var previewBtn: Button
    private var previewing = false
    private val previewAutoStop = Runnable {
        previewing = false
        refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs         = Prefs(this)
        statusText    = findViewById(R.id.tv_status)
        permissionBtn = findViewById(R.id.btn_permission)
        toggleBtn     = findViewById(R.id.btn_toggle)
        previewBtn    = findViewById(R.id.btn_preview)

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

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val canDraw = Settings.canDrawOverlays(this)
        permissionBtn.isEnabled = !canDraw
        permissionBtn.text = if (canDraw) "Overlay permission: OK" else "Grant overlay permission"

        toggleBtn.isEnabled = canDraw
        toggleBtn.text = if (prefs.enabled) "Disable overlay" else "Enable overlay"

        previewBtn.isEnabled = canDraw && prefs.enabled
        previewBtn.text = if (previewing) "Stop preview" else "Preview gears 1-12"

        statusText.text = when {
            !canDraw       -> "1) Grant permission to draw over other apps."
            !prefs.enabled -> "2) Enable the overlay. Current rear gear will show in the top-left corner."
            else           -> "Overlay active. Leave this app — the indicator stays visible over Karoo pages."
        }
    }
}
