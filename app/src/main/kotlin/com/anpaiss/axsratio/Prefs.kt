package com.anpaiss.axsratio

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_ENABLED, value).apply()

    companion object {
        private const val NAME = "axs_ratio_prefs"
        private const val KEY_ENABLED = "enabled"
    }
}
