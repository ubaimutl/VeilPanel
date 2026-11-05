package com.ubai.veilpanel.config;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

/** Portable visual profiles. No device-specific settings, routing, permissions,
 * wallpaper files, recovery behavior, or saved font files are changed. */
public final class PanelThemePresets {
    private PanelThemePresets(){}
    public static final String[] IDS={"minimal","frosted","glass","readable"};
    public static final String[] NAMES={"Minimal","Frosted","Glass","Readable"};
    public static final String[] DESCRIPTIONS={"Compact, clean cards","Soft frost and rounded cards","Translucent, iOS-inspired glass","Larger text and stronger contrast"};
    private static Map<String,Object> base(){
        Map<String,Object> values=new LinkedHashMap<>();
        for(PanelAppearance.Option option:PanelAppearance.OPTIONS)values.put(option.key,option.initial);
        values.put("font_family","system");values.put("dynamic",true);
        values.put("blur",24);values.put("glass_opacity",72);
        values.put("reference_glass",false);values.put("kyant_liquid_glass",false);values.put("experimental_glass",false);
        values.put("description_brightness",30);
        values.put("header_clock_visible",true);values.put("header_date_visible",true);
        values.put("header_battery_visible",false);values.put("header_settings_visible",true);values.put("header_close_visible",true);
        values.put("header_clock_alignment","start");values.put("header_date_alignment","start");
        values.put("header_clock_style","plain");values.put("header_clock_leading_zero",false);
        values.put("header_clock_weight",500);values.put("header_clock_spacing",40);
        values.put("header_clock_top_space",0);values.put("header_clock_bottom_space",20);
        values.put("header_date_size",17);values.put("header_date_weight",400);
        values.put("clock_glass_frost",60);values.put("clock_glass_highlight",65);values.put("clock_glass_refraction",6);values.put("clock_glass_bevel",2);
        values.put("liquid_refraction",10);values.put("liquid_edge",16);values.put("liquid_blur",20);
        values.put("liquid_tint",28);values.put("liquid_highlight",65);values.put("liquid_dispersion",false);values.put("liquid_depth",true);
        return values;
    }
    public static Map<String,Object> values(String id){
        Map<String,Object> v=base();
        switch(id){
            case "minimal":
                v.put("blur",12);v.put("glass_opacity",94);v.put("card_radius",18);v.put("card_padding",12);v.put("card_gap",8);v.put("icon_size",36);v.put("clock_size",32);break;
            case "frosted":
                v.put("reference_glass",true);v.put("card_radius",28);v.put("clock_size",48);v.put("header_clock_alignment","center");v.put("header_date_alignment","center");break;
            case "glass":
                v.put("reference_glass",true);v.put("kyant_liquid_glass",true);v.put("card_radius",28);v.put("glass_opacity",66);
                v.put("clock_size",88);v.put("header_clock_style","liquid");v.put("header_clock_weight",600);
                v.put("header_clock_top_space",-20);v.put("header_clock_alignment","center");v.put("header_date_alignment","center");break;
            case "readable":
                v.put("glass_opacity",98);v.put("headline_size",20);v.put("body_size",18);v.put("time_size",14);v.put("icon_size",48);
                v.put("card_height",104);v.put("card_padding",18);v.put("card_gap",12);v.put("clock_size",48);v.put("description_brightness",85);break;
            default:throw new IllegalArgumentException("Unknown theme");
        }
        return v;
    }
    private static SharedPreferences history(Context context){return context.getSharedPreferences("panel_theme_history",0);}
    private static void put(SharedPreferences.Editor edit,String key,Object value){
        if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);
        else if(value instanceof Integer)edit.putInt(key,(Integer)value);
        else if(value instanceof String)edit.putString(key,(String)value);
        else throw new IllegalArgumentException("Unsupported theme value");
    }
    public static void apply(Context context,String id){
        Map<String,Object> values=values(id);
        SharedPreferences prefs=context.getSharedPreferences("veil_panel",0);
        JSONObject previous=new JSONObject();
        try{
            Map<String,?> current=prefs.getAll();
            for(String key:values.keySet())previous.put(key,current.containsKey(key)?current.get(key):JSONObject.NULL);
        }catch(org.json.JSONException invalid){throw new IllegalStateException(invalid);}
        if(!history(context).edit().putString("previous",previous.toString()).commit())throw new IllegalStateException("Cannot save previous look");
        SharedPreferences.Editor edit=prefs.edit();for(var value:values.entrySet())put(edit,value.getKey(),value.getValue());edit.apply();
    }
    public static boolean canUndo(Context context){return history(context).contains("previous");}
    public static boolean undo(Context context){
        String saved=history(context).getString("previous",null);if(saved==null)return false;
        try{
            JSONObject previous=new JSONObject(saved);Map<String,Object> allowed=base();
            // Validate completely before writing; restore absent keys as absent.
            Map<String,Object> restored=new LinkedHashMap<>();
            for(String key:allowed.keySet()){
                if(!previous.has(key))return false;
                Object value=previous.get(key);
                if(value!=JSONObject.NULL&&!allowed.get(key).getClass().isInstance(value))return false;
                restored.put(key,value);
            }
            SharedPreferences.Editor edit=context.getSharedPreferences("veil_panel",0).edit();
            for(var value:restored.entrySet())if(value.getValue()==JSONObject.NULL)edit.remove(value.getKey());else put(edit,value.getKey(),value.getValue());
            edit.apply();history(context).edit().remove("previous").apply();return true;
        }catch(org.json.JSONException|IllegalArgumentException invalid){return false;}
    }
}
