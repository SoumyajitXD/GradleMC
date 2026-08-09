package com.soumyajit.gradlemc.client.gui

/** Pure responsive layout calculations kept outside the screen for easy unit testing. */
data class GradleMcGuiLayout(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val headerBottom: Int,
    val footerTop: Int,
    val contentLeft: Int,
    val contentWidth: Int,
    val contentTop: Int,
    val contentBottom: Int,
) {
    data class TabBounds(val x: Int, val y: Int, val width: Int, val height: Int)

    fun navigationTabs(count: Int): List<TabBounds> {
        require(count >= 0) { "count must be nonnegative" }
        if (count == 0) return emptyList()
        val available = (footerTop - contentTop).coerceAtLeast(0)
        if (available < count) return emptyList()
        val gap = when {
            available >= count * 18 + (count - 1) * 4 -> 4
            available >= count * 12 + (count - 1) * 2 -> 2
            else -> 1
        }
        val height = ((available - (count - 1) * gap) / count).coerceAtLeast(1)
        val x = left + MARGIN
        val width = (contentLeft - left - MARGIN * 2 - 8).coerceAtLeast(1)
        return List(count) { index -> TabBounds(x, contentTop + index * (height + gap), width, height) }
            .filter { it.y + it.height <= footerTop }
    }

    companion object {
        const val MARGIN = 14
        private const val SIDE_BAR_WIDTH = 128
        private const val MAX_WIDTH = 780
        private const val MAX_HEIGHT = 430

        fun calculate(screenWidth: Int, screenHeight: Int): GradleMcGuiLayout {
            val width = (screenWidth - 24).coerceIn(1, MAX_WIDTH)
            val height = (screenHeight - 24).coerceIn(1, MAX_HEIGHT)
            val left = (screenWidth - width) / 2
            val top = (screenHeight - height) / 2
            val right = left + width
            val bottom = top + height
            val headerBottom = top + minOf(58, (height / 3).coerceAtLeast(20))
            val footerTop = maxOf(headerBottom, bottom - minOf(34, (height / 4).coerceAtLeast(20)))
            val contentTop = minOf(footerTop, headerBottom + minOf(MARGIN, (height / 12).coerceAtLeast(1)))
            val contentLeft = left + minOf(SIDE_BAR_WIDTH + MARGIN * 2, (width / 2).coerceAtLeast(MARGIN))
            return GradleMcGuiLayout(left, top, right, bottom, headerBottom, footerTop, contentLeft,
                (right - contentLeft - MARGIN).coerceAtLeast(1), contentTop, footerTop - MARGIN)
        }
    }
}
