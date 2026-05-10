package com.anpaiss.axsratio

import io.hammerhead.karooext.models.DataType
import kotlin.math.roundToInt

enum class Metric(
    val displayName: String,
    val streamType:  String,
    val widthDp:     Int,
    val heightDp:    Int,
    val textSizeSp:  Int,
) {
    GEAR   ("Gear",    DataType.Type.SHIFTING_REAR_GEAR, 44, 40, 30),
    HR     ("HR",      DataType.Type.HEART_RATE,         64, 40, 24),
    POWER  ("Power",   DataType.Type.POWER,              64, 40, 24),
    CADENCE("Cadence", DataType.Type.CADENCE,            56, 40, 24),
    SPEED  ("Speed",   DataType.Type.SPEED,              52, 40, 24),
    GRADE    ("Grade",       DataType.Type.ELEVATION_GRADE,       64, 40, 20),
    TEMP     ("Temp",        DataType.Type.TEMPERATURE,           52, 40, 22),
    DIST_TURN("To next turn",DataType.Type.DISTANCE_TO_NEXT_TURN, 72, 40, 20),
    ;

    fun format(value: Double?): String = when (this) {
        GEAR -> when {
            value == null || value <= 0 -> "-"
            value < 10                  -> value.toInt().toString()
            else                        -> (value.toInt() % 10).toString()
        }
        HR, POWER, CADENCE -> value?.roundToInt()?.toString() ?: "-"
        SPEED              -> value?.let { (it * 3.6).roundToInt().toString() } ?: "-"
        GRADE              -> value?.let { String.format("%+.0f%%", it) } ?: "-"
        TEMP               -> value?.let { "${it.roundToInt()}°" } ?: "-"
        DIST_TURN          -> value?.let {
            val m = it.roundToInt()
            if (m < 1000) "${m}m" else String.format("%.1fk", m / 1000.0)
        } ?: "-"
    }

    /** ARGB background. For HR pass the latest [hrZone]; ignored otherwise. */
    fun backgroundColor(value: Double?, hrZone: Int?): Int = when (this) {
        GEAR -> {
            val g = value?.toInt() ?: 0
            if (g >= 10) 0xFF000000.toInt() else COLOR_DEFAULT
        }
        HR -> when (hrZone) {
            1    -> 0xFF9E9E9E.toInt()
            2    -> 0xFF2196F3.toInt()
            3    -> 0xFF4CAF50.toInt()
            4    -> 0xFFFF9800.toInt()
            5    -> 0xFFF44336.toInt()
            else -> COLOR_DEFAULT
        }
        else -> COLOR_DEFAULT
    }

    companion object {
        const val COLOR_DEFAULT = 0xFFFF5823.toInt()
    }
}
