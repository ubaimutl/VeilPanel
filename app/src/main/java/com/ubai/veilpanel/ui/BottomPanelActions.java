package com.ubai.veilpanel.ui;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import com.ubai.veilpanel.MainActivity;
import com.ubai.veilpanel.R;
import com.ubai.veilpanel.config.BottomActionOptions;
import com.ubai.veilpanel.system.FlashlightController;

/** Two panel-scoped buttons; no background polling or additional overlay window. */
final class BottomPanelActions extends FrameLayout {
 private final AccessibilityService service;
 private final Runnable closePanel;
 private final ImageButton left,right;
 private final String leftAction,rightAction;
 private FlashlightController flashlight;
 private boolean closed;
 static BottomPanelActions create(AccessibilityService service,Runnable close,int height,int statusHeight){
  if(!BottomActionOptions.enabled(service))return null;
  if("none".equals(BottomActionOptions.action(service,false))&&"none".equals(BottomActionOptions.action(service,true)))return null;
  float density=service.getResources().getDisplayMetrics().density;
  if((BottomActionOptions.bottom(service)+BottomActionOptions.size(service)+8)*density+statusHeight>height)return null;
  return new BottomPanelActions(service,close);
 }
 private BottomPanelActions(AccessibilityService service,Runnable close){
  super(service);this.service=service;closePanel=close;leftAction=BottomActionOptions.action(service,false);rightAction=BottomActionOptions.action(service,true);
  setTag("bottom_actions");setClipChildren(false);
  left=button(leftAction,false);right=button(rightAction,true);
  if("flashlight".equals(leftAction)||"flashlight".equals(rightAction)){
   flashlight=new FlashlightController(service);flashlight.setOnStateChangedListener(this::refresh);
  }
  refresh();
 }
 int reservedBottom(){return dp(BottomActionOptions.bottom(service)+BottomActionOptions.size(service)+16+(BottomActionOptions.fade(service)?64:0));}
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 FrameLayout.LayoutParams placement(){
  FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,dp(BottomActionOptions.size(service)),Gravity.BOTTOM);p.bottomMargin=dp(BottomActionOptions.bottom(service));return p;
 }
 private ImageButton button(String action,boolean rightSide){
  ImageButton button=new ImageButton(service);button.setTag(rightSide?"bottom_action_right":"bottom_action_left");button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);button.setPadding(dp(14),dp(14),dp(14),dp(14));button.setElevation(0);button.setStateListAnimator(null);
  int icon=switch(action){case "camera"->R.drawable.ic_qs_camera;case "settings"->R.drawable.ic_qs_settings;case "close"->R.drawable.ic_panel_close;default->R.drawable.ic_qs_flashlight;};
  button.setImageResource(icon);button.setVisibility("none".equals(action)?GONE:VISIBLE);
  button.setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View view,Outline outline){outline.setOval(0,0,view.getWidth(),view.getHeight());}});button.setClipToOutline(true);
  button.setOnClickListener(v->run(action));
  int size=dp(BottomActionOptions.size(service));FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(size,size,Gravity.CENTER_VERTICAL|(rightSide?Gravity.RIGHT:Gravity.LEFT));
  // Use physical left/right, matching the selected action names and pull-down routing.
  int available=getResources().getDisplayMetrics().widthPixels;int inset=BottomActionOptions.sidePixels(service,available);
  if(rightSide)p.rightMargin=inset;else p.leftMargin=inset;addView(button,p);return button;
 }
 private void refresh(){if(closed)return;style(left,leftAction);style(right,rightAction);}
 private void style(ImageButton button,String action){
  boolean torch="flashlight".equals(action),active=torch&&flashlight!=null&&flashlight.isEnabled();
  int foreground=active?PanOSPTheme.notificationBackground(service):PanOSPTheme.notificationPrimaryText(service);
  GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);circle.setColor(active?Color.WHITE:Color.TRANSPARENT);
  Drawable background=active?circle:new ReferenceGlassDrawable(service,dp(BottomActionOptions.size(service))/2f,80);
  GradientDrawable mask=new GradientDrawable();mask.setShape(GradientDrawable.OVAL);mask.setColor(Color.WHITE);
  button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x30FFFFFF),background,mask));button.setBackgroundTintList(null);button.setColorFilter(foreground);
  String label=switch(action){case "camera"->"Open camera";case "settings"->"Open Veil settings";case "close"->"Close notifications";case "none"->"No action";default->active?"Turn flashlight off":"Turn flashlight on";};
  button.setContentDescription(label);button.setSelected(active);button.setStateDescription(torch?(active?"On":"Off"):null);
 }
 private boolean unlocked(){KeyguardManager keyguard=service.getSystemService(KeyguardManager.class);return keyguard!=null&&!keyguard.isKeyguardLocked()&&!keyguard.isDeviceLocked();}
 private void run(String action){
  if(closed||!unlocked())return;
  if("flashlight".equals(action)){
   if(flashlight==null)return;
   if(!flashlight.hasPermission()){openSettings(true);return;}
   if(!flashlight.toggle())Toast.makeText(service,"Flashlight is unavailable. Another app may be using the camera.",Toast.LENGTH_SHORT).show();
   return;
  }
  if("close".equals(action)){closePanel.run();return;}
  if("settings".equals(action)){openSettings(false);return;}
  if("camera".equals(action))try{
   service.startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));closePanel.run();
  }catch(ActivityNotFoundException unavailable){Toast.makeText(service,"No camera app is available",Toast.LENGTH_SHORT).show();}
  catch(SecurityException denied){Toast.makeText(service,"Camera could not be opened. Check camera access in Veil settings.",Toast.LENGTH_SHORT).show();}
 }
 private void openSettings(boolean permission){
  try{
   service.startActivity(new Intent(service,MainActivity.class).putExtra("open_bottom_actions_settings",true).putExtra("request_flashlight_permission",permission).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));closePanel.run();
  }catch(RuntimeException unavailable){Toast.makeText(service,"Open Bottom actions in Veil settings to allow flashlight access.",Toast.LENGTH_SHORT).show();}
 }
 void close(){closed=true;if(flashlight!=null){flashlight.setOnStateChangedListener(null);flashlight.close();flashlight=null;}}
}
