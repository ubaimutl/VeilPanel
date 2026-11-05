package com.ubai.veilpanel.ui;

import android.content.*;
import android.graphics.Color;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import com.ubai.veilpanel.image.PowerSaverBackdrop;
import java.util.function.Consumer;

/** A non-activity wallpaper host below SystemUI. Never changes the resumed task. */
public final class PanelBackdropWindow {
 private final Context context;
 private final WindowManager manager;
 private FrameLayout view;
 private ImageView image;
 private WindowManager.LayoutParams params;
 private boolean registered,blurRegistered;
 private int generation;
 private ImageView cover;
 private StatusBarContentUnderlay statusContent;
 private static final java.util.concurrent.ExecutorService IMAGES=java.util.concurrent.Executors.newSingleThreadExecutor();
 private final Consumer<Boolean> blurChanged=enabled->refresh();
 private final BroadcastReceiver powerChanged=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){refresh();}};
 public PanelBackdropWindow(Context service){
  android.view.Display display=service.getSystemService(android.hardware.display.DisplayManager.class).getDisplay(android.view.Display.DEFAULT_DISPLAY);
  context=service.createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,null);
  manager=context.getSystemService(WindowManager.class);
 }
 public boolean show(){
  if(view!=null)return true;
  if(!Settings.canDrawOverlays(context))return false;
  view=new FrameLayout(context);view.setBackgroundColor(PowerSaverBackdrop.available(context)?PanOSPTheme.notificationBackground(context):Color.TRANSPARENT);
  image=new ImageView(context);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setVisibility(View.GONE);view.addView(image,new FrameLayout.LayoutParams(-1,-1));
  View scrim=new View(context);scrim.setBackgroundColor(0x33000000);view.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
  params=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER|WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,android.graphics.PixelFormat.TRANSLUCENT);
  params.gravity=Gravity.TOP|Gravity.LEFT;params.setFitInsetsTypes(0);params.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;params.setTitle("VeilPanelBackdrop");
  try{manager.addView(view,params);}catch(RuntimeException denied){view=null;image=null;return false;}
  refresh();
  try{manager.addCrossWindowBlurEnabledListener(context.getMainExecutor(),blurChanged);blurRegistered=true;}catch(RuntimeException unsupported){}
  context.registerReceiver(powerChanged,new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),Context.RECEIVER_NOT_EXPORTED);registered=true;
  return true;
 }
 private void refresh(){
  if(view==null)return;
  PowerManager power=context.getSystemService(PowerManager.class);boolean saving=power!=null&&power.isPowerSaveMode(),nativeBlur=false;
  try{nativeBlur=manager.isCrossWindowBlurEnabled();}catch(RuntimeException unsupported){}
  boolean available=PowerSaverBackdrop.available(context);
  boolean saved=available,blur=nativeBlur&&!saving&&!saved;
  params.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
  if(blur)params.flags|=WindowManager.LayoutParams.FLAG_BLUR_BEHIND;else params.flags&=~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
  params.setBlurBehindRadius(blur?Math.round(Math.max(0,Math.min(64,context.getSharedPreferences("veil_panel",0).getInt("blur",24)))*context.getResources().getDisplayMetrics().density):0);
  params.dimAmount=saved?0:blur?.20f:.38f;manager.updateViewLayout(view,params);
  int request=++generation;image.setVisibility(View.GONE);
  if(saved)IMAGES.execute(()->{android.graphics.Bitmap bitmap=PowerSaverBackdrop.loadAtBlur(context,context.getSharedPreferences("veil_panel",0).getInt("blur",24));context.getMainExecutor().execute(()->{if(view!=null&&request==generation&&bitmap!=null){image.setImageBitmap(bitmap);image.setVisibility(View.VISIBLE);}});});
 }
 /** Opaque saved wallpaper in the trusted content layer also covers stock
  * heads-up windows, which otherwise sit above the live-wallpaper host. */
 public void addPopupCover(FrameLayout root,int statusHeight){
  if(!PowerSaverBackdrop.available(context))return;
  cover=new ImageView(root.getContext());cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setBackgroundColor(PanOSPTheme.notificationBackground(context));cover.setVisibility(View.VISIBLE);
  FrameLayout.LayoutParams layout=new FrameLayout.LayoutParams(-1,-1);root.addView(cover,layout);
  cover.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->v.setClipBounds(new android.graphics.Rect(0,PanelSystemBars.visible(context)?statusHeight:0,r-l,b-t)));
  IMAGES.execute(()->{android.graphics.Bitmap bitmap=PowerSaverBackdrop.loadAtBlur(context,context.getSharedPreferences("veil_panel",0).getInt("blur",24));context.getMainExecutor().execute(()->{if(view!=null&&cover!=null&&bitmap!=null){cover.setImageBitmap(bitmap);cover.setColorFilter(0x33000000,android.graphics.PorterDuff.Mode.SRC_ATOP);cover.setVisibility(View.VISIBLE);}});});
 }
 public void mirrorStatusContent(PanelContentViewport content,int statusHeight){
  if(view==null||!PanelSystemBars.visible(context))return;
  statusContent=new StatusBarContentUnderlay(content,statusHeight);
  view.addView(statusContent,new FrameLayout.LayoutParams(-1,-1));
 }
 public void close(){
  if(statusContent!=null){statusContent.close();statusContent=null;}
  generation++;
  if(cover!=null){if(cover.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)cover.getParent()).removeView(cover);cover=null;}
  if(registered){context.unregisterReceiver(powerChanged);registered=false;}
  if(blurRegistered){manager.removeCrossWindowBlurEnabledListener(blurChanged);blurRegistered=false;}
  if(view!=null){manager.removeViewImmediate(view);view=null;image=null;params=null;}
 }
 /** Paint the same saved wallpaper crop into the stationary recovery guard.
  * No screenshot, extra decode, or per-frame bitmap allocation is needed. */
 public void drawStatusCover(android.graphics.Canvas canvas,int width,int height){
  canvas.drawColor(PanOSPTheme.notificationBackground(context));
  android.graphics.drawable.Drawable drawable=cover!=null?cover.getDrawable():null;
  if(!(drawable instanceof android.graphics.drawable.BitmapDrawable))return;
  android.graphics.Bitmap bitmap=((android.graphics.drawable.BitmapDrawable)drawable).getBitmap();
  float scale=Math.max((float)width/bitmap.getWidth(),(float)height/bitmap.getHeight());
  float left=(width-bitmap.getWidth()*scale)/2,top=(height-bitmap.getHeight()*scale)/2;
  android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
  canvas.drawBitmap(bitmap,null,new android.graphics.RectF(left,top,left+bitmap.getWidth()*scale,top+bitmap.getHeight()*scale),paint);
  canvas.drawColor(0x33000000);
 }
 public void setCurtainOffset(float offset){if(statusContent!=null)statusContent.setCurtainOffset(offset);if(view!=null)view.setTranslationY(offset);}
}
