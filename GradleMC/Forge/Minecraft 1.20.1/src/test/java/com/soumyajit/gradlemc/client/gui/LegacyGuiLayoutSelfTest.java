package com.soumyajit.gradlemc.client.gui;

/** Deterministic release checks for the donor geometry contract. */
public final class LegacyGuiLayoutSelfTest {
    private LegacyGuiLayoutSelfTest() { }
    public static void run() {
        check(LegacyGuiStyle.HEADER_HEIGHT == 58, "header");
        check(LegacyGuiStyle.SIDEBAR_WIDTH == 128, "sidebar");
        check(LegacyGuiStyle.FOOTER_HEIGHT == 34, "footer");
        check(LegacyGuiStyle.BUTTON_HEIGHT == 20, "button");
        check(LegacyGuiStyle.GAP == 8, "gap");
        check(LegacyGuiStyle.BACKGROUND == 0xE010141C, "palette");
        for (int[] size : new int[][] {{854,480}, {1280,720}, {1920,1080}}) {
            LegacyGuiLayout layout = LegacyGuiLayout.calculate(size[0], size[1]);
            LegacyGradleMCScreen.Layout donorLayout = LegacyGradleMCScreen.layoutFor(size[0], size[1]);
            check(layout.left() >= 0 && layout.top() >= 0 && layout.right() <= size[0] && layout.bottom() <= size[1], "panel containment");
            check(layout.headerBottom() <= layout.mainTop(), "header/main bounds");
            check(layout.mainTop() + layout.mainHeight() <= layout.footerTop(), "footer overlap");
            check(donorLayout.left() >= 0 && donorLayout.top() >= 0 && donorLayout.right() <= size[0] && donorLayout.bottom() <= size[1], "donor shell containment");
            check(donorLayout.headerBottom() <= donorLayout.mainTop(), "donor header bounds");
            check(donorLayout.mainTop() + donorLayout.mainHeight() <= donorLayout.footerTop(), "donor footer overlap");
            check(donorLayout.contentLeft() >= donorLayout.left() && donorLayout.contentLeft() + donorLayout.contentWidth() <= donorLayout.right(), "main content bounds");
            int column = LegacyGuiLayout.columnWidth(layout.contentWidth(), 3);
            check(layout.contentLeft() + (column + LegacyGuiStyle.GAP) * 2 + column <= layout.contentLeft() + layout.contentWidth(), "quick action columns");
            int donorColumn = LegacyGradleMCScreen.columnWidth(donorLayout.contentWidth(), 3);
            check(donorLayout.contentLeft() + (donorColumn + LegacyGuiStyle.GAP) * 2 + donorColumn <= donorLayout.contentLeft() + donorLayout.contentWidth(), "donor button hitboxes");
            check(LegacyGradleMCScreen.clampScroll(Integer.MAX_VALUE, donorLayout.mainHeight() * 3, donorLayout.mainHeight()) == donorLayout.mainHeight() * 2, "donor scroll range");
            check(LegacyGradleMCScreen.clampScroll(-9, donorLayout.mainHeight(), donorLayout.mainHeight()) == 0, "tooltip/scroll clamp");
        }
        check(LegacyGuiLayout.clampScroll(-1, 400, 200) == 0, "negative scroll");
        check(LegacyGuiLayout.clampScroll(9999, 400, 200) == 200, "max scroll");
        check(GradleMCGuiSection.values().length == 7, "selected navigation inventory");
        check(!GradleMCGuiSection.QUICK_ACTIONS.label().getString().isBlank(), "selected navigation label");
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
}
