package com.ubai.veilpanel.ui;

import android.content.Context;

/** Independent experimental preferences; existing card appearance is retained. */
public final class LiquidGlassOptions {
    private LiquidGlassOptions() {}
    public static boolean enabled(Context c){return c.getSharedPreferences("veil_panel",0).getBoolean("kyant_liquid_glass",false);}
    public static int get(Context c,String key){
        int min,max,value;
        switch(key){
            case "liquid_refraction":min=0;max=32;value=12;break;
            case "liquid_edge":min=2;max=36;value=18;break;
            case "liquid_blur":min=0;max=64;value=20;break;
            case "liquid_tint":min=0;max=75;value=24;break;
            case "liquid_highlight":min=0;max=100;value=45;break;
            default:throw new IllegalArgumentException("Unknown liquid-glass option");
        }
        return Math.max(min,Math.min(max,c.getSharedPreferences("veil_panel",0).getInt(key,value)));
    }
    public static boolean dispersion(Context c){return c.getSharedPreferences("veil_panel",0).getBoolean("liquid_dispersion",true);}
    public static boolean depth(Context c){return c.getSharedPreferences("veil_panel",0).getBoolean("liquid_depth",true);}
}
