// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.ui.navigation;
import org.junit.Test;
import static org.junit.Assert.*;

public class SectionStateTest {
    @Test public void emptyStateRejectsSelection() {
        SectionState s = new SectionState();
        assertEquals(-1, s.getSelected()); assertEquals(-1, s.adjacent(1));
        assertFalse(s.select(0));
    }
    @Test public void variableCountAndBoundedEdges() {
        SectionState s = new SectionState();
        for (int i = 0; i < 5; i++) s.addSection();
        assertEquals(5, s.getCount()); assertEquals(0, s.getSelected());
        assertEquals(0, s.adjacent(-1)); assertTrue(s.select(4));
        assertEquals(4, s.adjacent(1));
        assertFalse(s.select(5)); assertFalse(s.select(-1)); assertFalse(s.select(4));
        assertEquals(4, s.getSelected());
    }
    @Test public void onePageRemainsSelected() {
        SectionState s = new SectionState(); s.addSection();
        assertEquals(0, s.adjacent(-1)); assertEquals(0, s.adjacent(1));
    }
    @Test public void intentThresholdAndVerticalDominance() {
        assertFalse(SectionState.isHorizontalSwipe(23, 0, 24));
        assertTrue(SectionState.isHorizontalSwipe(-24, 0, 24));
        assertFalse(SectionState.isHorizontalSwipe(50, 50, 24));
        assertFalse(SectionState.isHorizontalSwipe(20, 90, 24));
    }
    @Test public void overflowOnlyWhenContentHidden() {
        assertFalse(SectionState.hasHiddenRight(0, 0, 0));
        assertFalse(SectionState.hasHiddenRight(300, 400, 0));
        assertTrue(SectionState.hasHiddenRight(500, 300, 0));
        assertFalse(SectionState.hasHiddenRight(500, 300, 200));
    }
}
