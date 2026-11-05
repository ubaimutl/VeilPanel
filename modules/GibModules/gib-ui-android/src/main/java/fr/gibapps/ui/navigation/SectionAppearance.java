// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.ui.navigation;

/** Immutable Android ARGB colors only. No dependency on any visual-style library. */
public final class SectionAppearance {
    public static final SectionAppearance DEFAULT = new SectionAppearance(
            0xff101216, 0xff9dc9c2, 0xffeceff4, 0xff283c3b, 0xff101216);
    public final int backgroundColor;
    public final int activeTextColor;
    public final int inactiveTextColor;
    public final int activeTabColor;
    public final int inactiveTabColor;

    public SectionAppearance(int backgroundColor, int activeTextColor, int inactiveTextColor,
            int activeTabColor, int inactiveTabColor) {
        this.backgroundColor = backgroundColor;
        this.activeTextColor = activeTextColor;
        this.inactiveTextColor = inactiveTextColor;
        this.activeTabColor = activeTabColor;
        this.inactiveTabColor = inactiveTabColor;
    }
}
