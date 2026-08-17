package com.soumyajit.gradlemc.client

/** Responsive geometry for the diagnostics screen, including a compact tab grid for small GUI scales. */
internal data class GradleMcGuiLayout(
    val left: Int, val top: Int, val right: Int, val bottom: Int,
    val headerBottom: Int, val footerTop: Int,
    val contentLeft: Int, val contentWidth: Int, val contentTop: Int, val contentBottom: Int,
    val compactNavigation: Boolean,
) {
    data class TabBounds(val x: Int, val y: Int, val width: Int, val height: Int)

    fun navigationTabs(count: Int): List<TabBounds> {
        if (count <= 0) return emptyList()
        return if (compactNavigation) {
            val columns = minOf(3, count)
            val rows = (count + columns - 1) / columns
            val gap = 3
            val tabWidth = ((right - left - MARGIN * 2 - gap * (columns - 1)) / columns).coerceAtLeast(1)
            val tabHeight = ((contentTop - headerBottom - 8 - gap * (rows - 1)) / rows).coerceAtLeast(12)
            List(count) { index ->
                TabBounds(left + MARGIN + (index % columns) * (tabWidth + gap), headerBottom + 5 + (index / columns) * (tabHeight + gap), tabWidth, tabHeight)
            }
        } else {
            val gap = 3
            val tabHeight = ((footerTop - contentTop - gap * (count - 1)) / count).coerceAtLeast(14)
            val tabWidth = (contentLeft - left - MARGIN * 2 - 7).coerceAtLeast(1)
            List(count) { index -> TabBounds(left + MARGIN, contentTop + index * (tabHeight + gap), tabWidth, tabHeight) }
        }
    }

    companion object {
        private const val MARGIN = 14

        fun calculate(screenWidth: Int, screenHeight: Int): GradleMcGuiLayout {
            val panelWidth = (screenWidth - 20).coerceIn(1, 820)
            val panelHeight = (screenHeight - 20).coerceIn(1, 460)
            val left = (screenWidth - panelWidth) / 2
            val top = (screenHeight - panelHeight) / 2
            val right = left + panelWidth
            val bottom = top + panelHeight
            val headerBottom = top + minOf(54, (panelHeight / 3).coerceAtLeast(26))
            val footerTop = maxOf(headerBottom + 20, bottom - minOf(34, (panelHeight / 4).coerceAtLeast(26)))
            val compact = panelWidth < 500 || panelHeight < 270
            val contentTop = if (compact) (headerBottom + 68).coerceAtMost(footerTop - 8) else headerBottom + 12
            val contentLeft = if (compact) left + MARGIN else left + minOf(166, (panelWidth / 3).coerceAtLeast(130))
            return GradleMcGuiLayout(
                left, top, right, bottom, headerBottom, footerTop,
                contentLeft, (right - contentLeft - MARGIN).coerceAtLeast(1),
                contentTop, (footerTop - MARGIN).coerceAtLeast(contentTop), compact,
            )
        }
    }
}
