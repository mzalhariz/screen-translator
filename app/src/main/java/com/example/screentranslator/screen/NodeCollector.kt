package com.example.screentranslator.screen

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Walks the accessibility node tree and collects the currently-visible text nodes together with
 * their on-screen bounds. This is what makes "translate the whole page" possible: we don't rely
 * on what's focused, we harvest every visible string, and re-harvest on scroll.
 */
object NodeCollector {

    data class TextNode(val text: String, val bounds: Rect)

    private const val MAX_NODES = 60
    private const val MAX_TEXT_LENGTH = 1500

    fun collect(root: AccessibilityNodeInfo?): List<TextNode> {
        if (root == null) return emptyList()
        val result = ArrayList<TextNode>()
        val seen = HashSet<String>()
        val bounds = Rect()
        traverse(root, result, seen, bounds)
        return result
    }

    private fun traverse(
        node: AccessibilityNodeInfo,
        out: MutableList<TextNode>,
        seen: MutableSet<String>,
        bounds: Rect,
    ) {
        if (out.size >= MAX_NODES) return

        if (node.isVisibleToUser && !node.isPassword) {
            val raw = node.text
            if (!raw.isNullOrBlank()) {
                val text = raw.toString().trim()
                if (text.isNotEmpty() && text.length <= MAX_TEXT_LENGTH) {
                    node.getBoundsInScreen(bounds)
                    if (bounds.width() > 0 && bounds.height() > 0) {
                        // Deduplicate identical text at identical positions (trees often repeat).
                        if (seen.add(text + '@' + bounds.toShortString())) {
                            out.add(TextNode(text, Rect(bounds)))
                        }
                    }
                }
            }
        }

        for (i in 0 until node.childCount) {
            if (out.size >= MAX_NODES) return
            val child = node.getChild(i) ?: continue
            traverse(child, out, seen, bounds)
        }
    }
}
