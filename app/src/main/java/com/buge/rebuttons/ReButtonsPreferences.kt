package com.buge.rebuttons

import android.content.Context

object ReButtonsPreferences {
    const val FILE_NAME = "rebuttons_preferences"
    const val COMPONENT_SCALE = "component_scale"
    const val COLLAPSE_DELAY = "collapse_delay"
    const val POWER_ON_TOP = "power_on_top"
    const val VOLUME_ENABLED = "volume_enabled"
    const val POWER_ENABLED = "power_enabled"
    const val DEFAULT_SCALE = 1.0f
    const val DEFAULT_COLLAPSE_DELAY = 3000
    const val MIN_COLLAPSE_DELAY = 1000
    const val MAX_COLLAPSE_DELAY = 10000
    const val DEFAULT_POWER_ON_TOP = false
    const val DEFAULT_VOLUME_ENABLED = true
    const val DEFAULT_POWER_ENABLED = true

    fun get(context: Context) = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
}
