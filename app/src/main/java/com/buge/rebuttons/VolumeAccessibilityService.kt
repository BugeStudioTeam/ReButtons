package com.buge.rebuttons

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
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
    private lateinit var settings: ReButtonsSettings
    private var overlay: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var preferRightEdge = true
    private val isRightEdge = mutableStateOf(true)
    private val preferences = mutableStateOf(ButtonPreferences())

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            preferences.value = settings.current()
            applyWindowMetrics()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settings = ReButtonsSettings.get(this)
        preferences.value = settings.current()
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels
        showOverlay()
        ContextCompat.registerReceiver(
            this,
            refreshReceiver,
            IntentFilter(SettingsActivity.ACTION_REFRESH),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun expandedWidth(): Int = (76 * resources.displayMetrics.density * preferences.value.panelScale).roundToInt()

    private fun expandedHeight(): Int = (240 * resources.displayMetrics.density * preferences.value.panelScale).roundToInt()

    private fun shrunkWidth(): Int = (14 * resources.displayMetrics.density).roundToInt()

    private fun shrunkHeight(): Int = (228 * resources.displayMetrics.density * preferences.value.panelScale).roundToInt()

    private fun showOverlay() {
        if (overlay != null) return
        val composeView = ComposeView(this)
        val owner = OverlayLifecycleOwner()
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        composeView.setViewTreeLifecycleOwner(owner)
        composeView.setViewTreeSavedStateRegistryOwner(owner)
        composeView.setContent {
            val prefs = preferences.value
            VolumePanel(
                preferences = prefs,
                onVolumeUp = { dispatchVolume(KeyEvent.KEYCODE_VOLUME_UP) },
                onVolumeDown = { dispatchVolume(KeyEvent.KEYCODE_VOLUME_DOWN) },
                onPowerDialog = { performGlobalAction(GLOBAL_ACTION_POWER_DIALOG) },
                onDrag = { deltaX, deltaY -> moveOverlay(deltaX, deltaY) },
                onDragEnd = { snapToNearestEdge() },
                isRightEdge = isRightEdge.value,
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
            x = if (preferRightEdge) screenWidth - w else 0
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
        val w = expandedWidth()
        val h = expandedHeight()
        params.width = w
        params.height = h
        params.x = if (preferRightEdge) screenWidth - w else 0
        params.y = (screenHeight - h) / 2
        overlay?.let { windowManager.updateViewLayout(it, params) }
    }

    private fun setWindowShrunk(shrunk: Boolean) {
        val params = layoutParams ?: return
        val w = if (shrunk) shrunkWidth() else expandedWidth()
        val h = if (shrunk) shrunkHeight() else expandedHeight()
        if (params.width == w && params.height == h) return
        params.width = w
        params.height = h
        params.x = if (isRightEdge.value) screenWidth - w else 0
        params.y = (screenHeight - h) / 2
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
        preferRightEdge = toRight
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
        runCatching { unregisterReceiver(refreshReceiver) }
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
