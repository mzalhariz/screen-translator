package com.example.screentranslator.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View

/**
 * A single full-screen transparent View that paints translated text chips at each source node's
 * screen bounds. We redraw one View (rather than adding/removing many) to avoid flicker.
 */
class OverlayView(context: Context) : View(context) {

    private val items = ArrayList<Pair<Rect, String>>()

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 0, 0, 0)
    }

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
    }

    private val cornerRadius = 10f
    private val padding = 10f
    private val box = RectF()

    fun setItems(newItems: List<Pair<Rect, String>>) {
        items.clear()
        items.addAll(newItems)
        invalidate()
    }

    fun clear() {
        if (items.isEmpty()) return
        items.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val maxLeft = (width - 40).toFloat().coerceAtLeast(0f)

        for ((bounds, text) in items) {
            val wrapWidth = maxOf(bounds.width(), MIN_CHIP_WIDTH).toInt()
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, wrapWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()

            val left = bounds.left.toFloat().coerceIn(0f, maxLeft)
            val top = bounds.top.toFloat()

            box.set(
                left - padding,
                top - padding,
                left + layout.width + padding,
                top + layout.height + padding,
            )
            canvas.drawRoundRect(box, cornerRadius, cornerRadius, backgroundPaint)

            canvas.save()
            canvas.translate(left, top)
            layout.draw(canvas)
            canvas.restore()
        }
    }

    private companion object {
        const val MIN_CHIP_WIDTH = 160
    }
}
