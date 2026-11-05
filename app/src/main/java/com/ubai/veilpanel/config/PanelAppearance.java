package com.ubai.veilpanel.config;
import android.content.Context;
public final class PanelAppearance {
 public static final class Option {
  public final String key,label;public final int min,max,initial;
  public Option(String key,String label,int min,int max,int initial){this.key=key;this.label=label;this.min=min;this.max=max;this.initial=initial;}
 }
 public static final Option[] OPTIONS={
  new Option("icon_size","Icon size",24,64,40),new Option("icon_corner","Icon corner radius",0,24,10),
  new Option("headline_size","Title font size",12,24,16),new Option("body_size","Message font size",12,24,15),
  new Option("time_size","Timestamp font size",10,18,12),new Option("font_weight","Title font weight",400,700,600),
  new Option("card_height","Minimum card height",64,180,84),new Option("card_width","Card width (%)",75,100,100),
  new Option("side_margin","Panel side margin",8,36,16),new Option("card_padding","Card padding",8,28,14),
  new Option("icon_gap","Icon and text gap",6,28,12),new Option("card_gap","Card spacing",4,28,10),
  new Option("card_radius","Card corner radius",8,40,24),new Option("preview_lines","Preview lines",1,5,2),
  new Option("expanded_preview_lines","Expanded preview lines",6,20,10),
  new Option("clock_size","Clock font size",24,120,36),new Option("header_gap","Space below header",8,64,24)
 };
 public static int get(Context c,String key){for(Option o:OPTIONS)if(o.key.equals(key))return Math.max(o.min,Math.min(o.max,c.getSharedPreferences("veil_panel",0).getInt(key,o.initial)));throw new IllegalArgumentException(key);}
 public static void reset(Context c){android.content.SharedPreferences.Editor edit=c.getSharedPreferences("veil_panel",0).edit();for(Option o:OPTIONS)edit.remove(o.key);edit.apply();}
}
