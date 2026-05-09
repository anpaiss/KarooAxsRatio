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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs         = Prefs(this)
        statusText    = findViewById(R.id.tv_status)
        permissionBtn = findViewById(R.id.btn_permission)
        toggleBtn     = findViewById(R.id.btn_toggle)

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
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val canDraw = Settings.canDrawOverlays(this)
        permissionBtn.isEnabled = !canDraw
        permissionBtn.text = if (canDraw) "Permesso overlay: OK" else "Concedi permesso overlay"

        toggleBtn.isEnabled = canDraw
        toggleBtn.text = if (prefs.enabled) "Disattiva overlay" else "Attiva overlay"

        statusText.text = when {
            !canDraw       -> "1) Concedi il permesso di disegnare sopra le altre app."
            !prefs.enabled -> "2) Attiva l'overlay. Mostrerà il pignone corrente in basso a destra."
            else           -> "Overlay attivo. Esci dall'app — il rettangolo rimane visibile sopra le pagine Karoo."
        }
    }
}
