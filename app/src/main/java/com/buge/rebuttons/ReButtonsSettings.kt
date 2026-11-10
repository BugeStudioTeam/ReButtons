package com.buge.rebuttons

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ButtonPreferences(
    val panelScale: Float = 1f,
    val collapseDelayMs: Long = 3000L,
    val powerFirst: Boolean = false,
    val showVolume: Boolean = true,
    val showPower: Boolean = true,
)

class ReButtonsSettings private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _preferences = MutableStateFlow(read())
    val preferences: StateFlow<ButtonPreferences> = _preferences.asStateFlow()

    fun current(): ButtonPreferences = _preferences.value

    fun setPanelScale(value: Float) = update { it.copy(panelScale = value.coerceIn(MIN_SCALE, MAX_SCALE)) }

    fun setCollapseDelayMs(value: Long) =
        update { it.copy(collapseDelayMs = value.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)) }

    fun setPowerFirst(value: Boolean) = update { it.copy(powerFirst = value) }

    fun setShowVolume(value: Boolean) = guardAtLeastOne(volume = value)

    fun setShowPower(value: Boolean) = guardAtLeastOne(power = value)

    private fun guardAtLeastOne(volume: Boolean? = null, power: Boolean? = null) {
        val base = _preferences.value
        val nextVolume = volume ?: base.showVolume
        val nextPower = power ?: base.showPower
        if (!nextVolume && !nextPower) return
        update { it.copy(showVolume = nextVolume, showPower = nextPower) }
    }

    private fun update(transform: (ButtonPreferences) -> ButtonPreferences) {
        val next = transform(_preferences.value)
        if (next == _preferences.value) return
        prefs.edit()
            .putFloat(KEY_PANEL_SCALE, next.panelScale)
            .putLong(KEY_COLLAPSE_DELAY, next.collapseDelayMs)
            .putBoolean(KEY_POWER_FIRST, next.powerFirst)
            .putBoolean(KEY_SHOW_VOLUME, next.showVolume)
            .putBoolean(KEY_SHOW_POWER, next.showPower)
            .apply()
        _preferences.value = next
    }

    private fun read(): ButtonPreferences = ButtonPreferences(
        panelScale = prefs.getFloat(KEY_PANEL_SCALE, 1f).coerceIn(MIN_SCALE, MAX_SCALE),
        collapseDelayMs = prefs.getLong(KEY_COLLAPSE_DELAY, 3000L).coerceIn(MIN_DELAY_MS, MAX_DELAY_MS),
        powerFirst = prefs.getBoolean(KEY_POWER_FIRST, false),
        showVolume = prefs.getBoolean(KEY_SHOW_VOLUME, true),
        showPower = prefs.getBoolean(KEY_SHOW_POWER, true),
    )

    companion object {
        const val MIN_SCALE = 0.8f
        const val MAX_SCALE = 1.4f
        const val MIN_DELAY_MS = 1000L
        const val MAX_DELAY_MS = 10000L
        private const val PREFS_NAME = "rebuttons_settings"
        private const val KEY_PANEL_SCALE = "panel_scale"
        private const val KEY_COLLAPSE_DELAY = "collapse_delay"
        private const val KEY_POWER_FIRST = "power_first"
        private const val KEY_SHOW_VOLUME = "show_volume"
        private const val KEY_SHOW_POWER = "show_power"

        @Volatile
        private var instance: ReButtonsSettings? = null

        fun get(context: Context): ReButtonsSettings =
            instance ?: synchronized(this) {
                instance ?: ReButtonsSettings(context).also { instance = it }
            }
    }
}

object AccessibilityStatus {
    fun isServiceEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        val target = VolumeAccessibilityService::class.java.name
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                val id = info.id ?: return@any false
                id.startsWith(target) || info.resolveInfo.serviceInfo.name == target
            }
    }
}
