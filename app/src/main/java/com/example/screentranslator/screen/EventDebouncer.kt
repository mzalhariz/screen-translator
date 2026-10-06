package com.example.screentranslator.screen

import android.os.Handler

/**
 * Coalesces bursts of accessibility events (scrolling and content changes fire many per second)
 * into a single deferred action. Each new event cancels the pending one, so we only refresh once
 * the screen settles — this is what keeps lazy translate-on-scroll smooth and battery-friendly.
 */
class EventDebouncer(private val handler: Handler, private val delayMs: Long = 300L) {

    private var pending: Runnable? = null

    fun schedule(action: () -> Unit) {
        cancel()
        val runnable = Runnable {
            pending = null
            action()
        }
        pending = runnable
        handler.postDelayed(runnable, delayMs)
    }

    fun cancel() {
        pending?.let { handler.removeCallbacks(it) }
        pending = null
    }
}
