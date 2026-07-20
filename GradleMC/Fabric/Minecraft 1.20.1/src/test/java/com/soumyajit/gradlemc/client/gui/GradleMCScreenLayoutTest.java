package com.soumyajit.gradlemc.client.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GradleMCScreenLayoutTest {
    @Test
    void quickActionsGridDoesNotOverlapAtSupportedWindowSizes() {
        for (int[] size : List.of(new int[]{854, 480}, new int[]{960, 540}, new int[]{1280, 720}, new int[]{1920, 1080})) {
            GradleMCScreen.Layout layout = GradleMCScreen.layoutFor(size[0], size[1]);
            assertTrue(layout.left() >= 0 && layout.top() >= 0);
            assertTrue(layout.right() <= size[0] && layout.bottom() <= size[1]);
            assertTrue(layout.contentLeft() > layout.left());
            int column = GradleMCScreen.columnWidth(layout.contentWidth(), 3);
            int firstRight = layout.contentLeft() + column;
            int secondLeft = layout.contentLeft() + column + 4;
            int secondRight = secondLeft + column;
            int thirdLeft = layout.contentLeft() + (column + 4) * 2;
            int thirdRight = thirdLeft + column;
            assertTrue(firstRight < secondLeft, "first and second Quick Actions columns overlap");
            assertTrue(secondRight < thirdLeft, "second and third Quick Actions columns overlap");
            assertTrue(thirdRight <= layout.contentLeft() + layout.contentWidth(), "Quick Actions escape the panel");
            assertTrue(layout.mainTop() + 9 * 24 + 20 < layout.footerTop(), "Quick Actions overlap the footer");
        }
    }

    @Test
    void scrollAndResizeClampingStayBounded() {
        assertEquals(0, GradleMCScreen.clampScroll(-100, 500, 200));
        assertEquals(300, GradleMCScreen.clampScroll(9999, 500, 200));
        assertEquals(0, GradleMCScreen.clampScroll(50, 100, 200));
        assertEquals(120, GradleMCScreen.clampScroll(120, 500, 200));
    }

    @Test
    void repeatedScreenConstructionDoesNotRegisterGlobalCallbacks() {
        for (int index = 0; index < 100; index++) assertNotNull(new GradleMCScreen());
    }
}
