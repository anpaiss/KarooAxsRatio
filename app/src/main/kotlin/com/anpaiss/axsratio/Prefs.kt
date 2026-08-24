package com.anpaiss.axsratio

import android.content.Context
import android.content.SharedPreferences

class Prefs(ctx: Context) {

    val sp: SharedPreferences = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, false)
        set(v) = sp.edit().putBoolean(KEY_ENABLED, v).apply()

    var tileStyle: TileStyle
        get() {
            val raw = sp.getString(KEY_TILE_STYLE, null) ?: return TileStyle.VIVID
            return runCatching { TileStyle.valueOf(raw) }.getOrDefault(TileStyle.VIVID)
        }
        set(v) = sp.edit().putString(KEY_TILE_STYLE, v.name).apply()

    fun slotFor(metric: Metric): Slot {
        val raw = sp.getString(slotKey(metric), null) ?: return defaultSlot(metric)
        return runCatching { Slot.valueOf(raw) }.getOrDefault(Slot.OFF)
    }

    /** Sets [metric] to [slot]. If [slot] != OFF, any other metric on the same corner is reset to OFF. */
    fun setSlotFor(metric: Metric, slot: Slot) {
        val edit = sp.edit()
        if (slot != Slot.OFF) {
            Metric.values()
                .filter { it != metric && slotFor(it) == slot }
                .forEach { edit.putString(slotKey(it), Slot.OFF.name) }
        }
        edit.putString(slotKey(metric), slot.name).apply()
    }

    private fun slotKey(m: Metric) = "$KEY_SLOT_PREFIX${m.name}"

    private fun defaultSlot(m: Metric): Slot = if (m == Metric.GEAR) Slot.TL else Slot.OFF

    companion object {
        private const val NAME             = "axs_ratio_prefs"
        private const val KEY_ENABLED      = "enabled"
        const val         KEY_SLOT_PREFIX  = "slot_"
        const val         KEY_TILE_STYLE   = "tile_style"
    }
}
