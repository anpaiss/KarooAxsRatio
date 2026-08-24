package com.anpaiss.axsratio

/** Colori risolti per una tile: sfondo, testo e bordo opzionale. */
data class TileColors(
    val bg: Int,
    val text: Int,
    val border: Int = 0,
    val borderWidthDp: Int = 0,
)

/**
 * Preset di stile delle tile, selezionabile dalle impostazioni.
 * Ogni abbinamento testo/sfondo garantisce contrasto WCAG >= 4.5:1
 * (i rapporti sono documentati nel commento di ciascun colore).
 */
enum class TileStyle(val label: String) {
    VIVID("Vivid"),
    OUTLINE("Outline"),
    PASTEL("Pastel"),
    INK("Ink"),
    ;

    /**
     * Colori per [metric] dato il valore corrente e (per HR) la zona.
     * value/hrZone null = tile senza dati, usa i colori di default dello stile.
     */
    fun colorsFor(metric: Metric, value: Double?, hrZone: Int?): TileColors {
        val zone     = if (metric == Metric.HR) hrZone else null
        val gearHigh = metric == Metric.GEAR && (value?.toInt() ?: 0) >= 10

        return when (this) {
            VIVID -> when {
                gearHigh     -> TileColors(BLACK, WHITE)
                zone == 1    -> TileColors(0xFF9E9E9E.toInt(), BLACK)  // 7.8:1
                zone == 2    -> TileColors(0xFF1976D2.toInt(), WHITE)  // 4.6:1
                zone == 3    -> TileColors(0xFF4CAF50.toInt(), BLACK)  // 7.6:1
                zone == 4    -> TileColors(0xFFFF9800.toInt(), BLACK)  // 9.7:1
                zone == 5    -> TileColors(0xFFD32F2F.toInt(), WHITE)  // 5.0:1
                else         -> TileColors(ACCENT, BLACK)              // 6.7:1
            }

            // Testo sempre bianco su nero (19:1); la zona parla attraverso il bordo.
            OUTLINE -> {
                val border = when {
                    gearHigh  -> WHITE
                    zone != null -> zoneAccent(zone)
                    else      -> ACCENT
                }
                TileColors(0xFF101010.toInt(), WHITE, border, borderWidthDp = 3)
            }

            PASTEL -> when {
                gearHigh     -> TileColors(BLACK, WHITE)
                zone == 1    -> TileColors(0xFFE0E0E0.toInt(), BLACK)  // 13.9:1
                zone == 2    -> TileColors(0xFF90CAF9.toInt(), BLACK)  // 12.0:1
                zone == 3    -> TileColors(0xFFA5D6A7.toInt(), BLACK)  // 12.8:1
                zone == 4    -> TileColors(0xFFFFCC80.toInt(), BLACK)  // 14.2:1
                zone == 5    -> TileColors(0xFFEF9A9A.toInt(), BLACK)  //  9.8:1
                else         -> TileColors(0xFFFFAB91.toInt(), BLACK)  // 11.5:1
            }

            // Valore nel colore della zona su fondo nero (min 5.7:1 in Z5);
            // bordo sottile per staccare la tile dalle mappe scure.
            INK -> {
                val text = when {
                    gearHigh  -> WHITE
                    zone != null -> zoneAccent(zone)
                    else      -> ACCENT
                }
                TileColors(0xFF0A0A0A.toInt(), text, 0xFF3A3E45.toInt(), borderWidthDp = 1)
            }
        }
    }

    companion object {
        const val ACCENT = 0xFFFF5823.toInt()
        private const val BLACK = 0xFF000000.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()

        /** Colori pieni delle zone HR (usati come bordo/testo negli stili scuri). */
        private fun zoneAccent(zone: Int): Int = when (zone) {
            1    -> 0xFF9E9E9E.toInt()
            2    -> 0xFF2196F3.toInt()
            3    -> 0xFF4CAF50.toInt()
            4    -> 0xFFFF9800.toInt()
            5    -> 0xFFF44336.toInt()
            else -> ACCENT
        }
    }
}
