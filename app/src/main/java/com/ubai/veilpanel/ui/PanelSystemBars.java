package com.ubai.veilpanel.ui;
import android.content.Context;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
/** Uses the real status bar; never reconstructs signal or battery indicators. */
public final class PanelSystemBars {
 private PanelSystemBars(){}
 public static boolean visible(Context context){return context.getSharedPreferences("veil_panel",0).getBoolean("show_status_bar",true);}
 public static void apply(View view){
  WindowInsetsController controller=view.getWindowInsetsController();if(controller==null)return;
  controller.setSystemBarsAppearance(0,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
  controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT);
  if(visible(view.getContext()))controller.show(WindowInsets.Type.statusBars());else controller.hide(WindowInsets.Type.statusBars());
 }
}
