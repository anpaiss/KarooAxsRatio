package com.anpaiss.axsratio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return
        if (!Prefs(context).enabled) return
        if (!Settings.canDrawOverlays(context)) return
        OverlayService.start(context)
    }
}
