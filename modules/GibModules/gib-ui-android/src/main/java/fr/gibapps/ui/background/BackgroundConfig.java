// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.ui.background;

/** EXPERIMENTAL. Immutable, platform-free configuration. Colors are opaque ARGB.
 * Type and direction are independently clamped; incompatible combinations retain
 * PanOSP rendering rules (linear: top-bottom fallback; radial: outward unless 5).
 */
public final class BackgroundConfig {
    public static final int SOLID = 0, LINEAR = 1, RADIAL = 2;
    public static final int TOP_BOTTOM = 0, LEFT_RIGHT = 1, BOTTOM_TOP = 2,
            RIGHT_LEFT = 3, CENTER_OUT = 4, OUT_CENTER = 5;
    public final int type;
    public final int color1;
    public final int color2;
    public final int direction;

    public BackgroundConfig(int type, int color1, int color2, int direction) {
        this.type = Math.max(SOLID, Math.min(RADIAL, type));
        this.color1 = color1 | 0xff000000;
        this.color2 = color2 | 0xff000000;
        this.direction = Math.max(TOP_BOTTOM, Math.min(OUT_CENTER, direction));
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BackgroundConfig)) return false;
        BackgroundConfig c = (BackgroundConfig) other;
        return type == c.type && color1 == c.color1 && color2 == c.color2 && direction == c.direction;
    }

    @Override public int hashCode() {
        return ((type * 31 + color1) * 31 + color2) * 31 + direction;
    }
}
