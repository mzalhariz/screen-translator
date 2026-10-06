package com.example.screentranslator.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.screentranslator.data.SettingsRepository
import com.example.screentranslator.screen.EventDebouncer
import com.example.screentranslator.screen.NodeCollector
import com.example.screentranslator.translate.TranslationCache
import com.example.screentranslator.translate.TranslationEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Core of the app. Receives accessibility events from the foreground app, harvests visible text
 * nodes, translates them on-device with ML Kit, and paints the result in an overlay window.
 *
 * Lazy translate-on-scroll: every relevant event is debounced; on refresh we render cached
 * translations instantly and translate the rest progressively. Scrolling cancels the in-flight
 * pass and starts a new one, so we only ever translate what's actually on screen.
 */
class TranslateAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val cache = TranslationCache()

    private lateinit var overlay: OverlayController
    private lateinit var bubble: FloatingBubble
    private lateinit var engine: TranslationEngine
    private lateinit var debouncer: EventDebouncer
    private lateinit var repository: SettingsRepository

    @Volatile private var enabled = true
    @Volatile private var targetLang = "es"
    private var refreshJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = OverlayController(this)
        engine = TranslationEngine(cache)
        debouncer = EventDebouncer(handler)
        repository = SettingsRepository(this)

        bubble = FloatingBubble(this) { toggleOverlay() }
        bubble.show()

        scope.launch {
            repository.settings.collectLatest { settings ->
                targetLang = settings.targetLang
                if (settings.enabled != enabled) {
                    enabled = settings.enabled
                    if (enabled) scheduleRefresh() else hideOverlay()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !enabled) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // Never translate our own UI (avoids feedback loops).
                if (event.packageName == packageName) return
                scheduleRefresh()
            }
        }
    }

    override fun onInterrupt() = Unit

    private fun scheduleRefresh() {
        if (this::debouncer.isInitialized) debouncer.schedule { refresh() }
    }

    private fun refresh() {
        if (!enabled) {
            hideOverlay()
            return
        }
        val nodes = NodeCollector.collect(rootInActiveWindow)
        overlay.show()
        if (nodes.isEmpty()) {
            overlay.clear()
            return
        }

        val target = targetLang
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val results = ArrayList<Pair<Rect, String>>(nodes.size)

            // 1) Paint whatever is already cached, immediately.
            for (node in nodes) {
                cache.getTranslation(node.text, target)?.let { results.add(node.bounds to it) }
            }
            overlay.render(results.toList())

            // 2) Translate the misses one-by-one, updating the overlay as each completes.
            for (node in nodes) {
                if (cache.getTranslation(node.text, target) != null) continue
                val translated = try {
                    engine.translate(node.text, target)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                if (translated != null) {
                    results.add(node.bounds to translated)
                    overlay.render(results.toList())
                }
            }
        }
    }

    private fun hideOverlay() {
        refreshJob?.cancel()
        refreshJob = null
        if (this::overlay.isInitialized) overlay.clear()
    }

    private fun toggleOverlay() {
        val newValue = !enabled
        enabled = newValue
        if (this::repository.isInitialized) {
            scope.launch { repository.setEnabled(newValue) }
        }
        if (newValue) scheduleRefresh() else hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::debouncer.isInitialized) debouncer.cancel()
        refreshJob?.cancel()
        if (this::overlay.isInitialized) overlay.remove()
        if (this::bubble.isInitialized) bubble.remove()
        scope.cancel()
    }
}
