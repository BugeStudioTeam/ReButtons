package com.buge.rebuttons

import android.content.Context

object ReButtonsPreferences {
    const val FILE_NAME = "rebuttons_preferences"
    const val COMPONENT_SCALE = "component_scale"
    const val HIDE_TIME = "hide_time"
    const val DEFAULT_SCALE = 1.0f

    fun get(context: Context) = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
}
