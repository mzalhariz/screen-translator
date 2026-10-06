package com.example.screentranslator.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.WindowManager

/**
 * Owns the translation overlay window. Uses TYPE_ACCESSIBILITY_OVERLAY, which an
 * AccessibilityService may add WITHOUT the SYSTEM_ALERT_WINDOW runtime permission. The window is
 * non-touchable and non-focusable so it never steals input from the app underneath.
 */
class OverlayController(context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: OverlayView? = null

    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    fun show() {
        if (overlayView != null) return
        val view = OverlayView(context)
        overlayView = view
        runCatching { windowManager.addView(view, layoutParams) }
    }

    fun render(items: List<Pair<Rect, String>>) {
        overlayView?.setItems(items)
    }

    fun clear() {
        overlayView?.clear()
    }

    fun remove() {
        overlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayView = null
    }
}
