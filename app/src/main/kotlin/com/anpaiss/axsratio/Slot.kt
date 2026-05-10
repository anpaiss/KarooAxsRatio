package com.anpaiss.axsratio

import android.view.Gravity

enum class Slot(val label: String, val gravityFlags: Int) {
    OFF("Off",          Gravity.NO_GRAVITY),
    TL ("Top-left",     Gravity.TOP    or Gravity.START),
    TR ("Top-right",    Gravity.TOP    or Gravity.END),
    BL ("Bottom-left",  Gravity.BOTTOM or Gravity.START),
    BR ("Bottom-right", Gravity.BOTTOM or Gravity.END),
}
