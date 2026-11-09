package com.ubai.veilpanel.ui;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
/** Optional palette-tinted material; no claim to Apple's proprietary renderer. */
public final class ExperimentalGlass {
 private ExperimentalGlass(){}
 public static boolean enabled(Context context){return context.getSharedPreferences("veil_panel",0).getBoolean("experimental_glass",false);}
 private static int alpha(int color,int value){return (color&0xffffff)|(Math.max(0,Math.min(255,value))<<24);}
 public static GradientDrawable surface(Context context,float radius,int opacity,boolean control){
  int base=PanOSPTheme.notificationBackground(context);int a=Math.round(GlassContrast.safeOpacity(context,opacity)*2.55f);
  GradientDrawable material=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{alpha(base,Math.min(245,a+25)),alpha(base,a),alpha(base,a+10)});
  material.setCornerRadius(radius);material.setStroke(Math.max(1,Math.round(context.getResources().getDisplayMetrics().density)),control?0x66FFFFFF:0x32FFFFFF);return material;
 }
 public static int iconSize(Context context){return Math.max(16,Math.min(26,context.getSharedPreferences("veil_panel",0).getInt("control_icon_size",18)));}
 public static boolean borders(Context context){return context.getSharedPreferences("veil_panel",0).getBoolean("control_borders",false);}
 public static void styleControl(View view){
  if(!borders(view.getContext()))return;
  float density=view.getResources().getDisplayMetrics().density;
  int visual=Math.max(24,Math.min(36,view.getContext().getSharedPreferences("veil_panel",0).getInt("control_surface_size",28)));
  GradientDrawable material;
  if(enabled(view.getContext()))material=surface(view.getContext(),visual*density/2,60,true);
  else{material=new GradientDrawable();material.setColor(0x00000000);material.setCornerRadius(visual*density/2);material.setStroke(Math.max(1,Math.round(density)),0x55FFFFFF);}
  int inset=Math.round((44-visual)*density/2);view.setBackground(new android.graphics.drawable.InsetDrawable(material,inset));
 }
}
