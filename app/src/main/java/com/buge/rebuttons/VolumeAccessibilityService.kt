package com.buge.rebuttons

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.media.AudioManager
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.math.roundToInt

class VolumeAccessibilityService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private lateinit var preferences: SharedPreferences
    private var overlay: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private val isRightEdge = mutableStateOf(true)
    private val scaleState = mutableStateOf(ReButtonsPreferences.DEFAULT_SCALE)
    private val collapseDelayState = mutableStateOf(ReButtonsPreferences.DEFAULT_COLLAPSE_DELAY)
    private val powerOnTopState = mutableStateOf(ReButtonsPreferences.DEFAULT_POWER_ON_TOP)
    private val volumeEnabledState = mutableStateOf(ReButtonsPreferences.DEFAULT_VOLUME_ENABLED)
    private val powerEnabledState = mutableStateOf(ReButtonsPreferences.DEFAULT_POWER_ENABLED)
    private var windowShrunk = false

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> loadPreferences() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels
        preferences = ReButtonsPreferences.get(this)
        loadPreferences()
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        showOverlay()
    }

    private fun loadPreferences() {
        scaleState.value = preferences.getFloat(
            ReButtonsPreferences.COMPONENT_SCALE,
            ReButtonsPreferences.DEFAULT_SCALE,
        )
        collapseDelayState.value = preferences.getInt(
            ReButtonsPreferences.COLLAPSE_DELAY,
            ReButtonsPreferences.DEFAULT_COLLAPSE_DELAY,
        )
        powerOnTopState.value = preferences.getBoolean(
            ReButtonsPreferences.POWER_ON_TOP,
            ReButtonsPreferences.DEFAULT_POWER_ON_TOP,
        )
        volumeEnabledState.value = preferences.getBoolean(
            ReButtonsPreferences.VOLUME_ENABLED,
            ReButtonsPreferences.DEFAULT_VOLUME_ENABLED,
        )
        powerEnabledState.value = preferences.getBoolean(
            ReButtonsPreferences.POWER_ENABLED,
            ReButtonsPreferences.DEFAULT_POWER_ENABLED,
        )
        applyWindowMetrics()
    }

    private fun density() = resources.displayMetrics.density

    private fun expandedWidth() = ((68f * scaleState.value + 8f) * density()).roundToInt()

    private fun expandedHeight() = ((68f * 3 + 12f + 24f) * scaleState.value * density()).roundToInt()

    private fun shrunkWidth() = (14 * density() * scaleState.value).roundToInt().coerceAtLeast(1)

    private fun shrunkHeight() = ((68f * 3 + 12f + 24f) * scaleState.value * density()).roundToInt()

    private fun showOverlay() {
        if (overlay != null) return
        val composeView = ComposeView(this)
        val owner = OverlayLifecycleOwner()
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        composeView.setViewTreeLifecycleOwner(owner)
        composeView.setViewTreeSavedStateRegistryOwner(owner)
        composeView.setContent {
            VolumePanel(
                onVolumeUp = { dispatchVolume(KeyEvent.KEYCODE_VOLUME_UP) },
                onVolumeDown = { dispatchVolume(KeyEvent.KEYCODE_VOLUME_DOWN) },
                onPowerDialog = { performGlobalAction(GLOBAL_ACTION_POWER_DIALOG) },
                onDrag = { deltaX, deltaY -> moveOverlay(deltaX, deltaY) },
                onDragEnd = { snapToNearestEdge() },
                isRightEdge = isRightEdge.value,
                scale = scaleState.value,
                collapseDelayMs = collapseDelayState.value,
                powerOnTop = powerOnTopState.value,
                volumeEnabled = volumeEnabledState.value,
                powerEnabled = powerEnabledState.value,
                onCollapsed = { setWindowShrunk(true) },
                onExpandStart = { setWindowShrunk(false) },
            )
        }
        val w = expandedWidth()
        val h = expandedHeight()
        val params = WindowManager.LayoutParams(
            w,
            h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - w
            y = (screenHeight - h) / 2
        }
        windowManager.addView(composeView, params)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        overlay = composeView
        layoutParams = params
    }

    private fun applyWindowMetrics() {
        val params = layoutParams ?: return
        if (windowShrunk) return
        val w = expandedWidth()
        val h = expandedHeight()
        if (params.width == w && params.height == h) return
        val anchoredRight = params.x + params.width / 2 >= screenWidth / 2
        params.width = w
        params.height = h
        params.x = if (anchoredRight) screenWidth - w else 0
        params.y = (screenHeight - h) / 2
        overlay?.let { windowManager.updateViewLayout(it, params) }
    }

    private fun setWindowShrunk(shrunk: Boolean) {
        val params = layoutParams ?: return
        val w = if (shrunk) shrunkWidth() else expandedWidth()
        val h = if (shrunk) shrunkHeight() else expandedHeight()
        if (params.width == w && params.height == h) return
        val leftInset = params.x
        val rightInset = screenWidth - (params.x + params.width)
        val anchoredRight = params.x + params.width / 2 >= screenWidth / 2
        windowShrunk = shrunk
        params.width = w
        params.height = h
        params.x = if (anchoredRight) screenWidth - rightInset - w else leftInset
        params.x = params.x.coerceIn(0, screenWidth - w)
        params.y = params.y.coerceIn(0, screenHeight - h)
        overlay?.let { windowManager.updateViewLayout(it, params) }
    }

    private fun dispatchVolume(keyCode: Int) {
        val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            AudioManager.ADJUST_RAISE
        } else {
            AudioManager.ADJUST_LOWER
        }
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    private fun moveOverlay(deltaX: Float, deltaY: Float) {
        val params = layoutParams ?: return
        params.x = (params.x + deltaX.roundToInt()).coerceIn(0, screenWidth - params.width)
        params.y = (params.y + deltaY.roundToInt()).coerceIn(0, screenHeight - params.height)
        isRightEdge.value = params.x + params.width / 2 >= screenWidth / 2
        overlay?.let { windowManager.updateViewLayout(it, params) }
    }

    private fun snapToNearestEdge() {
        val params = layoutParams ?: return
        val toRight = params.x + params.width / 2 >= screenWidth / 2
        val target = if (toRight) screenWidth - params.width else 0
        isRightEdge.value = toRight
        val start = params.x
        ValueAnimator.ofInt(start, target).apply {
            duration = 260
            addUpdateListener {
                params.x = it.animatedValue as Int
                overlay?.let { view -> windowManager.updateViewLayout(view, params) }
            }
            start()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        overlay?.let { windowManager.removeView(it) }
        overlay = null
        super.onDestroy()
    }

    private class OverlayLifecycleOwner : SavedStateRegistryOwner {
        val registry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        init {
            savedStateController.performAttach()
            savedStateController.performRestore(null)
        }

        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry get() = savedStateController.savedStateRegistry
    }
}
