package com.example.screentranslator.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.example.screentranslator.R
import kotlin.math.abs

/**
 * A small draggable floating button that toggles the translation overlay. Like the overlay, it is
 * an accessibility-overlay window, but it IS touchable so the user can tap/drag it.
 */
class FloatingBubble(
    private val context: Context,
    private val onToggle: () -> Unit,
) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var bubbleView: ImageView? = null

    private val layoutParams = WindowManager.LayoutParams(
        SIZE_PX,
        SIZE_PX,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 40
        y = 320
    }

    fun show() {
        if (bubbleView != null) return
        val view = ImageView(context).apply {
            setImageResource(R.drawable.ic_bubble)
            contentDescription = context.getString(R.string.app_name)
            setOnTouchListener(DragTouchListener())
        }
        bubbleView = view
        runCatching { windowManager.addView(view, layoutParams) }
    }

    fun remove() {
        bubbleView?.let { view -> runCatching { windowManager.removeView(view) } }
        bubbleView = null
    }

    private inner class DragTouchListener : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var downRawX = 0f
        private var downRawY = 0f
        private var dragged = false

        @Suppress("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    downRawX = event.rawX
                    downRawY = event.rawY
                    dragged = false
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > TOUCH_SLOP || abs(dy) > TOUCH_SLOP) dragged = true
                    layoutParams.x = initialX + dx.toInt()
                    layoutParams.y = initialY + dy.toInt()
                    bubbleView?.let {
                        runCatching { windowManager.updateViewLayout(it, layoutParams) }
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    if (!dragged) onToggle()
                    return true
                }
            }
            return false
        }
    }

    private companion object {
        const val SIZE_PX = 140
        const val TOUCH_SLOP = 12f
    }
}
