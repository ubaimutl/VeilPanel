package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import com.ubai.veilpanel.config.PanelAppearance;

/** The same card geometry and base material for notifications and their reply sheet. */
public final class NotificationCardSurface {
 private NotificationCardSurface(){}
 public static float radius(Context context){return PanelAppearance.get(context,"card_radius")*context.getResources().getDisplayMetrics().density;}
 public static int width(Context context,int available){
  float density=context.getResources().getDisplayMetrics().density;
  return Math.max(1,Math.round((available-Math.round(2*PanelAppearance.get(context,"side_margin")*density))*PanelAppearance.get(context,"card_width")/100f));
 }
 public static Drawable create(Context context){
  return create(context,context.getSharedPreferences("veil_panel",0).getInt("glass_opacity",66));
 }
 public static Drawable opaque(Context context){return create(context,100);}
 private static Drawable create(Context context,int opacity){
  float radius=radius(context);var prefs=context.getSharedPreferences("veil_panel",0);
  // The lens uses this same frosted fallback when its wallpaper backdrop is unavailable.
  if(prefs.getBoolean("reference_glass",false)||LiquidGlassOptions.enabled(context))return new ReferenceGlassDrawable(context,radius,opacity);
  if(ExperimentalGlass.enabled(context))return ExperimentalGlass.surface(context,radius,opacity,false);
  GradientDrawable background=new GradientDrawable();int colour=PanOSPTheme.notificationBackground(context);
  background.setColor((colour&0xffffff)|(Math.round(GlassContrast.safeOpacity(context,opacity)*2.55f)<<24));
  background.setCornerRadius(radius);background.setStroke(Math.round(context.getResources().getDisplayMetrics().density),0x20FFFFFF);return background;
 }
}
