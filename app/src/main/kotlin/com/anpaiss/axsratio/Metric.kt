package com.anpaiss.axsratio

import io.hammerhead.karooext.models.DataType
import kotlin.math.roundToInt

enum class Metric(
    val displayName: String,
    val streamType:  String,
    /** The field of the stream that holds the value: a stream can carry several (DataPoint.singleValue is just the first). */
    val field:       String,
    val widthDp:     Int,
    val heightDp:    Int,
    val textSizeSp:  Int,
) {
    GEAR   ("Gear",    DataType.Type.SHIFTING_REAR_GEAR, DataType.Field.SHIFTING_REAR_GEAR, 44, 40, 30),
    HR     ("HR",      DataType.Type.HEART_RATE,         DataType.Field.HEART_RATE, 64, 40, 24),
    POWER  ("Power",   DataType.Type.POWER,              DataType.Field.POWER, 64, 40, 24),
    CADENCE("Cadence", DataType.Type.CADENCE,            DataType.Field.CADENCE, 56, 40, 24),
    SPEED  ("Speed",   DataType.Type.SPEED,              DataType.Field.SPEED, 52, 40, 24),
    GRADE    ("Grade",       DataType.Type.ELEVATION_GRADE,       DataType.Field.ELEVATION_GRADE, 64, 40, 20),
    TEMP     ("Temp",        DataType.Type.TEMPERATURE,           DataType.Field.TEMPERATURE, 52, 40, 22),
    DIST_TURN("To next turn",DataType.Type.DISTANCE_TO_NEXT_TURN, DataType.Field.DISTANCE_TO_NEXT_TURN, 72, 40, 20),
    ;

    fun format(value: Double?): String = when (this) {
        // one digit on purpose, to keep the tile small: 10 to 13 show 0 to 3 on a black tile, the legs tell 3 from 13
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

}
