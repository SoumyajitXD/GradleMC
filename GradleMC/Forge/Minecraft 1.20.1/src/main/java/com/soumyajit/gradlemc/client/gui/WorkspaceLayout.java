package com.soumyajit.gradlemc.client.gui;

/** Immutable scaled-GUI geometry shared by drawing and hit testing. */
public record WorkspaceLayout(int left, int top, int right, int bottom, int headerBottom, int footerTop,
                              int navLeft, int navWidth, int contentLeft, int contentTop,
                              int contentWidth, int contentHeight, boolean compact) {
    public static WorkspaceLayout calculate(int screenWidth, int screenHeight) {
        int outerWidth = Math.max(300, Math.min(1040, screenWidth - 12));
        int outerHeight = Math.max(210, screenHeight - 12);
        int left = Math.max(2, (screenWidth - outerWidth) / 2);
        int top = Math.max(2, (screenHeight - outerHeight) / 2);
        int right = Math.min(screenWidth - 2, left + outerWidth);
        int bottom = Math.min(screenHeight - 2, top + outerHeight);
        boolean compact = outerWidth < 690;
        // Compact navigation can wrap to a second row at the minimum supported scaled width.
        int header = compact ? 70 : 54;
        int footer = compact ? 30 : 36;
        int navWidth = compact ? 0 : 142;
        int padding = compact ? 7 : 12;
        int headerBottom = top + header;
        int footerTop = Math.max(headerBottom + 48, bottom - footer);
        int contentLeft = compact ? left + padding : left + navWidth + padding * 2;
        int contentTop = headerBottom + padding;
        int contentWidth = Math.max(120, right - contentLeft - padding);
        int contentHeight = Math.max(36, footerTop - contentTop - padding);
        return new WorkspaceLayout(left, top, right, bottom, headerBottom, footerTop, left + padding,
                navWidth, contentLeft, contentTop, contentWidth, contentHeight, compact);
    }
}
