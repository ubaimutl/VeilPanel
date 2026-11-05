package com.ubai.veilpanel.ui;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
public final class PanelStyle {
    private static Typeface regular;
    public static Typeface font(Context context) {
        android.content.SharedPreferences prefs=context.getSharedPreferences("veil_panel",0);
        String family=prefs.getString("font_family",prefs.getBoolean("josefin",false)?"josefin":"system");
        if("custom".equals(family)){Typeface selected=CustomFont.load(context);if(selected!=null)return selected;}
        if("serif".equals(family))return Typeface.create("serif",Typeface.NORMAL);
        if(!"josefin".equals(family))return Typeface.create("sans-serif",Typeface.NORMAL);
        if (regular == null) regular = new Typeface.Builder(context.getAssets(), "fonts/josefin_sans.ttf")
                .setWeight(400).setFontVariationSettings("'wght' 400").setFallback("sans-serif").build();
        return regular;
    }
    public static void apply(View view) {
        view.setVerticalScrollBarEnabled(false);view.setHorizontalScrollBarEnabled(false);
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            int weight=text.getTypeface()!=null?text.getTypeface().getWeight():400;
            text.setTypeface(Typeface.create(font(view.getContext()),weight,false));
            text.setAllCaps(false);
        }
        if (view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) apply(((ViewGroup)view).getChildAt(i));
    }
}
