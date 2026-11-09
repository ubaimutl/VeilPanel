package com.ubai.veilpanel.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.*;
import com.ubai.veilpanel.R;

/** Settings-only colors and surfaces; never changes the notification panel palette. */
public final class SettingsStyle {
 private SettingsStyle(){}
 private static int categoryColor(String key){
  switch(key){
   case "access": return 0xff8bdab5;
   case "gestures": case "handle": case "motion": return 0xffa8c7fa;
   case "wallpaper": case "liquid": return 0xffd0bcff;
   case "header": case "typography": return 0xffffc595;
   case "notifications": return 0xff91d7e3;
   default:return 0xffb9c4d5;
  }
 }
 public static boolean isDark(Context context){
  String mode=context.getSharedPreferences("panel_settings_ui",0).getString("appearance","system");
  return "dark".equals(mode)||("system".equals(mode)&&(context.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES);
 }
 private static int themeColor(Context context,int attr,int fallback){return com.google.android.material.color.MaterialColors.getColor(context,attr,fallback);}
 public static int panelBackground(Context context){return themeColor(context,com.google.android.material.R.attr.colorSurfaceContainer,isDark(context)?0xff1e1e24:0xffefedf5);}
 public static int surface(Context context){return themeColor(context,com.google.android.material.R.attr.colorSurfaceBright,isDark(context)?0xff292a32:0xfffffbff);}
 public static int primaryText(Context context){return themeColor(context,com.google.android.material.R.attr.colorOnSurface,isDark(context)?0xffe5e1e9:0xff1d1b20);}
 public static int secondaryText(Context context){return themeColor(context,com.google.android.material.R.attr.colorOnSurfaceVariant,isDark(context)?0xffc8c5d0:0xff49454f);}
 public static int accent(Context context){return themeColor(context,androidx.appcompat.R.attr.colorPrimary,isDark(context)?0xffbdc7ed:0xff4c5c92);}
 public static android.graphics.drawable.Drawable row(Context context,boolean first,boolean last){
  float outer=28*context.getResources().getDisplayMetrics().density,inner=4*context.getResources().getDisplayMetrics().density;
  GradientDrawable shape=new GradientDrawable();shape.setColor(surface(context));shape.setCornerRadii(new float[]{first?outer:inner,first?outer:inner,first?outer:inner,first?outer:inner,last?outer:inner,last?outer:inner,last?outer:inner,last?outer:inner});
  return new RippleDrawable(ColorStateList.valueOf(isDark(context)?0x20ffffff:0x18000000),shape,null);
 }
 public static void groupRows(android.widget.LinearLayout body){
  for(int i=0;i<body.getChildCount();i++){
   android.view.View view=body.getChildAt(i);if(view instanceof android.widget.TextView&&!(view instanceof android.widget.Button))continue;
   boolean first=i==0||isNote(body.getChildAt(i-1)),last=i==body.getChildCount()-1||isNote(body.getChildAt(i+1));view.setBackground(row(body.getContext(),first,last));
   android.widget.LinearLayout.LayoutParams p=(android.widget.LinearLayout.LayoutParams)view.getLayoutParams();p.topMargin=first?12:2;view.setLayoutParams(p);
  }
 }
 private static boolean isNote(android.view.View view){return view instanceof android.widget.TextView&&!(view instanceof android.widget.Button);}
 public static int color(Context context,String key){int color=categoryColor(key);return isDark(context)?color:PanOSPTheme.blend(0xff000000,color,.48f);}
 public static int icon(String key){
  switch(key){
   case "access":return R.drawable.ic_qs_settings;
   case "wallpaper":case "liquid":return R.drawable.ic_menu_theme;
   case "notifications":return R.drawable.ic_shortcut_notifications;
   case "header":return android.R.drawable.ic_menu_recent_history;
   case "typography":return R.drawable.ic_qs_reading;
   case "advanced":return R.drawable.ic_menu_backup;
   case "layout":return R.drawable.ic_menu_disposition;
   default:return R.drawable.ic_qs_edit;
  }
 }
 public static GradientDrawable rounded(Context context,int color,int radius){
  GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius*context.getResources().getDisplayMetrics().density);return d;
 }
 public static Drawable touch(Context context,int color,int radius){
  return new RippleDrawable(ColorStateList.valueOf(isDark(context)?0x28ffffff:0x18000000),rounded(context,color,radius),rounded(context,Color.WHITE,radius));
 }
}
