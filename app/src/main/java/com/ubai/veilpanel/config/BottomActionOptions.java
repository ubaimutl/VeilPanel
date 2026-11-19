package com.ubai.veilpanel.config;
import android.content.Context;
/** Opt-in shortcuts with a separate central hit area for the dismiss handle. */
public final class BottomActionOptions {
 private BottomActionOptions(){}
 public static final String[] VALUES={"flashlight","camera","settings","close","none"};
 public static final String[] LABELS={"Flashlight","Camera","Veil settings","Close panel","None"};
 public static boolean enabled(Context context){return context.getSharedPreferences("veil_panel",0).getBoolean("bottom_actions_enabled",false);}
 private static int number(Context c,String key,int initial,int min,int max){return Math.max(min,Math.min(max,c.getSharedPreferences("veil_panel",0).getInt(key,initial)));}
 public static int size(Context c){return number(c,"bottom_action_size",56,48,72);}
 public static int side(Context c){return number(c,"bottom_action_side",32,24,64);}
 public static int bottom(Context c){return number(c,"bottom_action_bottom",48,0,220);}
 public static boolean fade(Context c){return c.getSharedPreferences("veil_panel",0).getBoolean("bottom_actions_fade",false);}
 public static int sidePixels(Context c,int width){
  float density=c.getResources().getDisplayMetrics().density;
  return Math.min(Math.round(side(c)*density),Math.max(0,(width-Math.round((size(c)*2+64)*density))/2));
 }
 public static int handleGuardWidth(Context c,int width,int preferred){
  if(!enabled(c)||("none".equals(action(c,false))&&"none".equals(action(c,true))))return preferred;
  int handleBottom=number(c,"dismiss_handle_bottom",42,0,100),handleHeight=number(c,"dismiss_grab_height",72,48,100);
  int low=handleBottom-Math.min(handleBottom,28),high=low+handleHeight;
  if(bottom(c)>=high||bottom(c)+size(c)<=low)return preferred;
  float density=c.getResources().getDisplayMetrics().density;
  int middle=Math.max(1,width-2*(sidePixels(c,width)+Math.round(size(c)*density)));
  return Math.min(preferred,Math.min(middle,Math.max(Math.round(48*density),middle-Math.round(16*density))));
 }
 public static String action(Context c,boolean right){
  String fallback=right?"camera":"flashlight",value=c.getSharedPreferences("veil_panel",0).getString(right?"bottom_action_right":"bottom_action_left",fallback);
  for(String allowed:VALUES)if(allowed.equals(value))return value;return fallback;
 }
}
