package com.ubai.veilpanel.config;
import android.content.Context;
/** The side is fixed from the initial DOWN, never changed while dragging. */
public final class PanelRouting {
    public static int split(Context c) { return Math.max(20, Math.min(80, c.getSharedPreferences("veil_panel",0).getInt("split",50))); }
    public static void setSplit(Context c, int value) { c.getSharedPreferences("veil_panel",0).edit().putInt("split",Math.max(20,Math.min(80,value))).apply(); }
    public static boolean isQuickSettings(float x, float width, int split) { return width > 0 && x >= width * Math.max(20,Math.min(80,split)) / 100f; }
}
