package com.ubai.veilpanel;
import android.app.Activity;
import android.app.KeyguardManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;
import com.ubai.veilpanel.image.PowerSaverBackdrop;
/** Live wallpaper by default; a user-selected, preblurred image when system blur is unavailable. */
public final class WallpaperBackdropActivity extends Activity {
 private static WeakReference<WallpaperBackdropActivity> active=new WeakReference<>(null);
 private static boolean wanted;
 private static final java.util.concurrent.ExecutorService IMAGES=java.util.concurrent.Executors.newSingleThreadExecutor();
 private ImageView savedWallpaper;private View scrim;private boolean registered,blurRegistered;private int generation;
 private final Consumer<Boolean> blurChanged=enabled->refreshBackdrop();
 private final BroadcastReceiver powerChanged=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){refreshBackdrop();}};
 public static void begin(){wanted=true;}
 public static void close(){wanted=false;WallpaperBackdropActivity activity=active.get();if(activity!=null)activity.finish();}
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);if(!wanted){finish();return;}
  KeyguardManager guard=getSystemService(KeyguardManager.class);if(guard==null||guard.isKeyguardLocked()){finish();return;}active=new WeakReference<>(this);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
  getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));getWindow().setNavigationBarContrastEnforced(false);getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);
  FrameLayout backdrop=new FrameLayout(this);savedWallpaper=new ImageView(this);savedWallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);savedWallpaper.setVisibility(View.GONE);backdrop.addView(savedWallpaper,new FrameLayout.LayoutParams(-1,-1));scrim=new View(this);scrim.setBackgroundColor(0x33000000);backdrop.addView(scrim,new FrameLayout.LayoutParams(-1,-1));backdrop.setOnClickListener(v->requestClose());setContentView(backdrop);
  getWindow().getDecorView().post(()->com.ubai.veilpanel.ui.PanelSystemBars.apply(getWindow().getDecorView()));
  refreshBackdrop();
  try{getWindowManager().addCrossWindowBlurEnabledListener(getMainExecutor(),blurChanged);blurRegistered=true;}catch(RuntimeException unsupported){/* The tinted backdrop remains available. */}
  registerReceiver(powerChanged,new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),Context.RECEIVER_NOT_EXPORTED);registered=true;
  getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::requestClose);
 }
 public static boolean shouldUseImage(boolean systemBlur,boolean powerSaver,boolean preview,boolean imageAvailable){return imageAvailable&&(!systemBlur||powerSaver||preview);}
 private void refreshBackdrop(){
  if(savedWallpaper==null||isFinishing()||isDestroyed())return;
  PowerManager power=getSystemService(PowerManager.class);boolean saving=power!=null&&power.isPowerSaveMode();boolean nativeBlur=false;
  try{nativeBlur=getWindowManager().isCrossWindowBlurEnabled();}catch(RuntimeException unsupported){}
  boolean available=PowerSaverBackdrop.available(this),preview=getSharedPreferences("veil_panel",0).getBoolean("fallback_always",false);
  boolean image=shouldUseImage(nativeBlur,saving,preview,available);boolean useNative=nativeBlur&&!saving&&!image;
  WindowManager.LayoutParams visual=getWindow().getAttributes();visual.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
  if(useNative)visual.flags|=WindowManager.LayoutParams.FLAG_BLUR_BEHIND;else visual.flags&=~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
  visual.setBlurBehindRadius(useNative?Math.round(Math.max(0,Math.min(64,getSharedPreferences("veil_panel",0).getInt("blur",24)))*getResources().getDisplayMetrics().density):0);visual.dimAmount=useNative||image?.20f:.38f;getWindow().setAttributes(visual);
  int request=++generation;savedWallpaper.setVisibility(View.GONE);
  if(image)IMAGES.execute(()->{android.graphics.Bitmap bitmap=PowerSaverBackdrop.load(getApplicationContext());runOnUiThread(()->{if(!isDestroyed()&&!isFinishing()&&request==generation&&bitmap!=null){savedWallpaper.setImageBitmap(bitmap);savedWallpaper.setVisibility(View.VISIBLE);}});});
 }
 private void requestClose(){sendBroadcast(new Intent(getPackageName()+".CLOSE_PANEL").setPackage(getPackageName()));close();}
 @Override protected void onStop(){super.onStop();if(!isChangingConfigurations()&&!isFinishing()&&active.get()==this)requestClose();}
 @Override protected void onDestroy(){generation++;if(registered)unregisterReceiver(powerChanged);if(blurRegistered)getWindowManager().removeCrossWindowBlurEnabledListener(blurChanged);if(active.get()==this)active.clear();super.onDestroy();}
}
