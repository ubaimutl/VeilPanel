// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.ui.navigation;

/** Platform-free selection state. Adjacent navigation is bounded; empty selection is -1. */
public final class SectionState {
    private int count;
    private int selected = -1;

    public int getCount() { return count; }
    public int getSelected() { return selected; }

    public void addSection() {
        count++;
        if (selected < 0) selected = 0;
    }

    public boolean select(int index) {
        if (index < 0 || index >= count || index == selected) return false;
        selected = index;
        return true;
    }

    public int adjacent(int direction) {
        if (count == 0) return -1;
        int candidate = selected + Integer.signum(direction);
        return candidate >= 0 && candidate < count ? candidate : selected;
    }

    /** Keep vertical scrolling distinct from intentional horizontal paging. */
    public static boolean isHorizontalSwipe(float dx, float dy, float threshold) {
        return Math.abs(dx) >= threshold && Math.abs(dx) > Math.abs(dy) * 1.15f;
    }

    public static boolean hasHiddenRight(int contentWidth, int viewportWidth, int scrollX) {
        return viewportWidth > 0 && contentWidth > scrollX + viewportWidth;
    }
}
