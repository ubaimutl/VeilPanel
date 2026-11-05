package com.ubai.veilpanel.ui;
import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Context;
import android.view.*;
import android.widget.*;
import com.ubai.veilpanel.config.PanelAppearance;
import com.ubai.veilpanel.system.SystemShadeController;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class PanelLifecycleTest {
 private AccessibilityService service;
 private ShadeWindowController panel;
 @Before public void setup(){
  service=Robolectric.buildService(com.ubai.veilpanel.accessibility.PanOSPAccessibilityServiceCore.class).create().get();
  service.getSharedPreferences("veil_panel",0).edit().clear().commit();
  Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);
  TestWallpaper.install(service);org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true);
  panel=new ShadeWindowController(service,new SystemShadeController(service));panel.start();
 }
 @After public void close(){panel.stop();com.ubai.veilpanel.image.PowerSaverBackdrop.clear(service);service.getSharedPreferences("veil_panel",0).edit().clear().commit();}
 @Test public void missingOverlayGrantKeepsThePreviousBackdropWorking(){
  org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(false);ReflectionHelpers.callInstanceMethod(panel,"open");assertTrue(panel.isExpanded());assertNull(ReflectionHelpers.getField(panel,"backdrop"));
  android.content.Intent launched=Shadows.shadowOf((android.app.Application)service.getApplicationContext()).getNextStartedActivity();assertNotNull(launched);assertEquals(com.ubai.veilpanel.WallpaperBackdropActivity.class.getName(),launched.getComponent().getClassName());panel.collapse();
 }
 @Test public void lockUnlockKeepsTheSameGestureWindowAndRestoresInput(){
  View root=ReflectionHelpers.getField(panel,"root");
  panel.setSuspended(true);assertEquals(View.GONE,root.getVisibility());
  WindowManager.LayoutParams params=ReflectionHelpers.getField(panel,"params");assertTrue((params.flags&WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)!=0);
  panel.setSuspended(false);assertSame(root,ReflectionHelpers.getField(panel,"root"));assertEquals(View.VISIBLE,root.getVisibility());assertEquals(0,params.flags&WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
  assertFalse(panel.isExpanded());
 }
 @Test public void headerAndNotificationsShareOneScrollbarFreeScrollView(){
  ReflectionHelpers.callInstanceMethod(panel,"open");assertTrue(panel.isExpanded());
  ScrollView scroll=ReflectionHelpers.getField(panel,"scroll");ShadeView content=ReflectionHelpers.getField(panel,"content");
  assertSame(scroll,content.getParent());assertEquals(1,countScrolls(scroll));assertFalse(scroll.isVerticalScrollBarEnabled());assertFalse(scroll.isHorizontalScrollBarEnabled());
  panel.collapse();assertFalse(panel.isExpanded());
 }
 @Test public void aCancelledLeftGestureRecoversFromSystemUiTakingTheStream(){
  send(MotionEvent.ACTION_DOWN,10,2);send(MotionEvent.ACTION_CANCEL,10,2);assertTrue(panel.isExpanded());
 }
 @Test public void blurStaysBelowTheStatusBarInsteadOfInTheAccessibilityOverlay(){
  WindowManager.LayoutParams params=ReflectionHelpers.getField(panel,"params");
  ReflectionHelpers.callInstanceMethod(panel,"open");assertEquals(0,params.flags&WindowManager.LayoutParams.FLAG_BLUR_BEHIND);assertEquals(0,params.getBlurBehindRadius());
  assertNull(Shadows.shadowOf((android.app.Application)service.getApplicationContext()).getNextStartedActivity());
  PanelBackdropWindow backdrop=ReflectionHelpers.getField(panel,"backdrop");assertNotNull(backdrop);
  WindowManager.LayoutParams visual=ReflectionHelpers.getField(backdrop,"params");assertTrue((visual.flags&WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)!=0);assertTrue((visual.flags&WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)!=0);assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,visual.type);
  panel.collapse();assertNull(ReflectionHelpers.getField(backdrop,"view"));
 }
 @Test public void rightSideGestureIsLeftForNativeQuickSettings(){
  int width=service.getResources().getDisplayMetrics().widthPixels;send(MotionEvent.ACTION_DOWN,width-2,2);send(MotionEvent.ACTION_MOVE,width-2,200);assertFalse(panel.isExpanded());
 }
 @Test public void collapsedStackDoesNotStretchToFillTheViewport(){
  android.service.notification.StatusBarNotification[] entries=new android.service.notification.StatusBarNotification[2];
  for(int i=0;i<2;i++)entries[i]=new android.service.notification.StatusBarNotification(service.getPackageName(),service.getPackageName(),i,null,10001,0,0,new android.app.Notification.Builder(service,"test").setContentTitle("Message "+i).setContentText("Preview").build(),android.os.Process.myUserHandle(),i);
  com.ubai.veilpanel.notifications.NotificationRepository.getInstance().replaceAll(entries,service);
  try{
   ReflectionHelpers.callInstanceMethod(panel,"open");ScrollView scroll=ReflectionHelpers.getField(panel,"scroll");
   scroll.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));scroll.layout(0,0,500,900);
   View stack=findStack(scroll);assertNotNull(stack);assertTrue("Stack backing must hug its card",stack.getMeasuredHeight()<300);
  }finally{com.ubai.veilpanel.notifications.NotificationRepository.getInstance().replaceAll(new android.service.notification.StatusBarNotification[0]);}
 }
 private View findStack(View view){if(view.getTransitionName()!=null&&view.getTransitionName().startsWith("stack:"))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=findStack(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}return null;}
 @Test public void bottomHandleAndHeaderCloseAreExplicitAndOrdinaryScrollDoesNotClose(){
  ReflectionHelpers.callInstanceMethod(panel,"open");View handle=ReflectionHelpers.getField(panel,"dismissHandle");assertNotNull(handle);assertTrue(handle.isClickable());handle.performClick();assertTrue((Boolean)ReflectionHelpers.getField(panel,"closing"));
 }
 @Test public void dismissLineIsAdjustableAndHasNoFooterSurface(){
  service.getSharedPreferences("veil_panel",0).edit().putBoolean("control_borders",true).putInt("dismiss_handle_width",72).putInt("dismiss_handle_bottom",0).commit();ReflectionHelpers.callInstanceMethod(panel,"open");
  FrameLayout handle=ReflectionHelpers.getField(panel,"dismissHandle"),viewport=ReflectionHelpers.getField(panel,"viewport");assertEquals(0,((FrameLayout.LayoutParams)handle.getLayoutParams()).bottomMargin);assertTrue(handle.getLayoutParams().width>0);assertEquals(1,handle.getChildCount());assertEquals(Math.round(72*service.getResources().getDisplayMetrics().density),handle.getChildAt(0).getLayoutParams().width);assertEquals(0,((android.graphics.drawable.ColorDrawable)handle.getBackground()).getAlpha());assertEquals(0,((FrameLayout.LayoutParams)viewport.getLayoutParams()).bottomMargin);
 }
 @Test public void layoutLimitsClampAndResetPreservesOtherSettings(){
  service.getSharedPreferences("veil_panel",0).edit().putInt("icon_size",10000).putInt("card_width",-1).putBoolean("show_status_bar",false).putInt("split",62).commit();
  assertEquals(64,PanelAppearance.get(service,"icon_size"));assertEquals(75,PanelAppearance.get(service,"card_width"));assertFalse(PanelSystemBars.visible(service));
  PanelAppearance.reset(service);assertEquals(40,PanelAppearance.get(service,"icon_size"));assertEquals(62,service.getSharedPreferences("veil_panel",0).getInt("split",0));assertFalse(PanelSystemBars.visible(service));
 }
 private void send(int action,float x,float y){MotionEvent event=MotionEvent.obtain(0,10,action,x,y,0);ReflectionHelpers.callInstanceMethod(panel,"touch",ReflectionHelpers.ClassParameter.from(View.class,ReflectionHelpers.getField(panel,"root")),ReflectionHelpers.ClassParameter.from(MotionEvent.class,event));event.recycle();}
 private int countScrolls(View view){int count=view instanceof ScrollView?1:0;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)count+=countScrolls(((ViewGroup)view).getChildAt(i));return count;}
}
