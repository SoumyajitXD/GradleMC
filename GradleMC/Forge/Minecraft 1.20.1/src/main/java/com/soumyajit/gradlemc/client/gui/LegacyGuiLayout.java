package com.soumyajit.gradlemc.client.gui;

/** Responsive translation of the legacy layout calculation. */
public record LegacyGuiLayout(int left, int top, int right, int bottom, int headerBottom, int footerTop,
                              int mainTop, int mainHeight, int contentLeft, int contentWidth) {
    public static LegacyGuiLayout calculate(int screenWidth, int screenHeight) {
        int availableWidth = Math.max(1, screenWidth - 24);
        int width = Math.min(LegacyGuiStyle.MAX_CONTENT_WIDTH, availableWidth);
        if (availableWidth >= LegacyGuiStyle.MIN_CONTENT_WIDTH) width = Math.max(LegacyGuiStyle.MIN_CONTENT_WIDTH, width);
        int height = Math.max(1, screenHeight - 32);
        int left = (screenWidth - width) / 2, top = (screenHeight - height) / 2;
        int right = left + width, bottom = top + height;
        int headerHeight = Math.min(LegacyGuiStyle.HEADER_HEIGHT, Math.max(20, height / 3));
        int footerHeight = Math.min(LegacyGuiStyle.FOOTER_HEIGHT, Math.max(20, height / 4));
        int headerBottom = top + headerHeight, footerTop = Math.max(headerBottom, bottom - footerHeight);
        int mainTop = Math.min(footerTop, headerBottom + Math.min(LegacyGuiStyle.MARGIN, Math.max(1, height / 12)));
        int mainHeight = Math.max(1, footerTop - mainTop - Math.min(LegacyGuiStyle.MARGIN, Math.max(0, (footerTop - mainTop) / 4)));
        int contentLeft = left + LegacyGuiStyle.SIDEBAR_WIDTH + LegacyGuiStyle.MARGIN * 2;
        return new LegacyGuiLayout(left, top, right, bottom, headerBottom, footerTop, mainTop, mainHeight,
                contentLeft, Math.max(1, right - contentLeft - LegacyGuiStyle.MARGIN));
    }
    public static int columnWidth(int contentWidth, int columns) {
        if (columns < 1) throw new IllegalArgumentException("columns must be positive");
        return Math.max(1, (contentWidth - LegacyGuiStyle.GAP * (columns - 1)) / columns);
    }
    public static int clampScroll(int requested, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(requested, Math.max(0, contentHeight - viewportHeight)));
    }
}
