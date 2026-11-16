package com.ubai.veilpanel.ui;
import android.content.Context;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
/** Uses the real status bar; never reconstructs signal or battery indicators. */
public final class PanelSystemBars {
 private PanelSystemBars(){}
 public static boolean visible(Context context){return context.getSharedPreferences("veil_panel",0).getBoolean("show_status_bar",true);}
 public static boolean transparent(Context context){return context.getSharedPreferences("veil_panel",0).getBoolean("scroll_under_status_bar",true);}
 public static int iconAppearance(Context context){
  String icons=context.getSharedPreferences("veil_panel",0).getString("status_bar_icons","light");
  boolean dark="dark".equals(icons)||("panel".equals(icons)&&android.graphics.Color.luminance(PanOSPTheme.primaryText(context))<.5f);
  return dark?WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS:0;
 }
 public static void apply(View view){
  WindowInsetsController controller=view.getWindowInsetsController();if(controller==null)return;
  controller.setSystemBarsAppearance(iconAppearance(view.getContext()),WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
  controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
  if(visible(view.getContext()))controller.show(WindowInsets.Type.statusBars());else controller.hide(WindowInsets.Type.statusBars());
 }
}
