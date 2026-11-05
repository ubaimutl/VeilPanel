package com.ubai.veilpanel.ui;
import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import com.ubai.veilpanel.system.SystemShadeController;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.time.Duration;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class CurtainInteractionTest {
 private AccessibilityService service;private ShadeWindowController panel;
 @Before public void setup(){
  service=Robolectric.buildService(com.ubai.veilpanel.accessibility.PanOSPAccessibilityServiceCore.class).create().get();
  service.getSharedPreferences("veil_panel",0).edit().clear().commit();Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);
  TestWallpaper.install(service);org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true);panel=new ShadeWindowController(service,new SystemShadeController(service));panel.start();ReflectionHelpers.callInstanceMethod(panel,"open");motion().jump(1);
 }
 @After public void close(){panel.stop();com.ubai.veilpanel.image.PowerSaverBackdrop.clear(service);}
 private CurtainMotion motion(){return ReflectionHelpers.getField(panel,"curtain");}
 private float height(){return ((WindowManager.LayoutParams)ReflectionHelpers.getField(panel,"params")).height;}
 private View root(){return ReflectionHelpers.getField(panel,"root");}
 private void send(int action,long time,float y){View guard=ReflectionHelpers.getField(panel,"handleGuard");MotionEvent event=MotionEvent.obtain(0,time,action,120,y,0);ReflectionHelpers.callInstanceMethod(panel,"handleTouch",ReflectionHelpers.ClassParameter.from(View.class,guard),ReflectionHelpers.ClassParameter.from(MotionEvent.class,event));event.recycle();}
 private void idle(long ms){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));}
 @Test public void statusContentScrollingDefaultsOnInTheRestoredUpperPanel(){
  FrameLayout viewport=ReflectionHelpers.getField(panel,"viewport");PanelBackdropWindow backdrop=ReflectionHelpers.getField(panel,"backdrop");
  assertSame(root(),viewport.getParent());assertNotNull(ReflectionHelpers.getField(backdrop,"statusContent"));assertNotNull(ReflectionHelpers.getField(backdrop,"cover"));
  assertEquals(0,((FrameLayout.LayoutParams)viewport.getLayoutParams()).topMargin);
  ScrollView scroll=ReflectionHelpers.getField(panel,"scroll");assertEquals(com.ubai.veilpanel.system.SystemWindowMetrics.getStatusBarHeight(service),scroll.getPaddingTop());
 }
 @Test public void disablingStatusContentScrollingRestoresTheWallpaperOnlyStrip(){
  panel.collapse();service.getSharedPreferences("veil_panel",0).edit().putBoolean("scroll_under_status_bar",false).commit();ReflectionHelpers.callInstanceMethod(panel,"open");motion().jump(1);
  FrameLayout viewport=ReflectionHelpers.getField(panel,"viewport");PanelBackdropWindow backdrop=ReflectionHelpers.getField(panel,"backdrop");
  assertSame(root(),viewport.getParent());assertNull(ReflectionHelpers.getField(backdrop,"statusContent"));assertNotNull(ReflectionHelpers.getField(backdrop,"cover"));
  assertEquals(com.ubai.veilpanel.system.SystemWindowMetrics.getStatusBarHeight(service),((FrameLayout.LayoutParams)viewport.getLayoutParams()).topMargin);
  ScrollView scroll=ReflectionHelpers.getField(panel,"scroll");assertEquals(0,scroll.getPaddingTop());assertTrue(PanelSystemBars.visible(service));
 }
 @Test public void dragMovesWallpaperCardsAndHandleTogetherWithoutFadingOrCoveringStatusIcons(){
  float h=height();send(MotionEvent.ACTION_DOWN,0,h-30);send(MotionEvent.ACTION_MOVE,1000,h-30-h*.10f);
  assertEquals(-h*.1f,root().getTranslationY(),1);assertEquals(1,root().getAlpha(),0);
  PanelBackdropWindow backdrop=ReflectionHelpers.getField(panel,"backdrop");View background=ReflectionHelpers.getField(backdrop,"view");assertEquals(root().getTranslationY(),background.getTranslationY(),.1f);
  int status=com.ubai.veilpanel.system.SystemWindowMetrics.getStatusBarHeight(service);assertTrue(root().getTranslationY()+root().getClipBounds().top>=status-.6f);
  send(MotionEvent.ACTION_UP,1200,h-30-h*.1f);assertFalse((Boolean)ReflectionHelpers.getField(panel,"closing"));idle(1000);assertTrue(panel.isExpanded());assertEquals(0,root().getTranslationY(),.1f);
 }
 @Test public void aLongSlowPullClosesAndRemovesTheGestureGuard(){
  float h=height();send(MotionEvent.ACTION_DOWN,0,h-30);send(MotionEvent.ACTION_MOVE,1000,h-30-h*.35f);send(MotionEvent.ACTION_UP,1200,h-30-h*.35f);
  assertTrue((Boolean)ReflectionHelpers.getField(panel,"closing"));idle(1200);assertFalse(panel.isExpanded());assertNull(ReflectionHelpers.getField(panel,"handleGuard"));assertEquals(0,root().getTranslationY(),0);assertNull(root().getClipBounds());
 }
 @Test public void aCancelledOrMultitouchGrabReturnsOpenAndReleaseCannotBecomeATap(){
  float h=height();send(MotionEvent.ACTION_DOWN,0,h-30);send(MotionEvent.ACTION_MOVE,1000,h-30-h*.3f);send(MotionEvent.ACTION_POINTER_DOWN,1050,h-30-h*.3f);send(MotionEvent.ACTION_UP,1100,h-30-h*.3f);idle(1000);assertTrue(panel.isExpanded());assertEquals(1,motion().position(),.001f);
 }
 @Test public void grabBoundsGrowAroundTheUnchangedLineAndSupportBottomZero(){
  panel.collapse();service.getSharedPreferences("veil_panel",0).edit().putInt("dismiss_handle_bottom",42).putInt("dismiss_grab_width",280).putInt("dismiss_grab_height",88).commit();ReflectionHelpers.callInstanceMethod(panel,"open");
  FrameLayout handle=ReflectionHelpers.getField(panel,"dismissHandle");FrameLayout.LayoutParams hit=(FrameLayout.LayoutParams)handle.getLayoutParams(),line=(FrameLayout.LayoutParams)handle.getChildAt(0).getLayoutParams();float density=service.getResources().getDisplayMetrics().density;
  assertEquals(Math.round(42*density),hit.bottomMargin+line.bottomMargin);assertEquals(Math.min((int)(280*density),service.getResources().getDisplayMetrics().widthPixels),hit.width);assertEquals(Math.round(88*density),hit.height);assertTrue(line.bottomMargin>0);
  panel.collapse();service.getSharedPreferences("veil_panel",0).edit().putInt("dismiss_handle_bottom",0).commit();ReflectionHelpers.callInstanceMethod(panel,"open");handle=ReflectionHelpers.getField(panel,"dismissHandle");assertEquals(0,((FrameLayout.LayoutParams)handle.getLayoutParams()).bottomMargin);assertEquals(0,((FrameLayout.LayoutParams)handle.getChildAt(0).getLayoutParams()).bottomMargin);
 }
 @Test public void closingSpringCanBeGrabbedAgainWithoutPositionJumpOrStaleCloseCallback(){
  motion().settle(0,0,()->panel.collapse());for(long frame:new long[]{1000000000L,1016666666L,1033333332L})ReflectionHelpers.callInstanceMethod(motion(),"step",ReflectionHelpers.ClassParameter.from(long.class,frame));float before=motion().position();assertTrue(before<1&&before>0);
  send(MotionEvent.ACTION_DOWN,100,300);assertEquals(before,motion().position(),.0001f);send(MotionEvent.ACTION_CANCEL,110,300);idle(1000);assertTrue(panel.isExpanded());assertEquals(1,motion().position(),.001f);
 }
 @Test public void releaseDecisionUsesDistanceSpeedAndDirection(){
  assertFalse(CurtainMotion.shouldClose(.94f,-100,1000,1));assertTrue(CurtainMotion.shouldClose(.94f,-1000,1000,1));assertTrue(CurtainMotion.shouldClose(.7f,0,1000,1));assertFalse(CurtainMotion.shouldClose(.7f,1000,1000,1));assertFalse(CurtainMotion.shouldClose(.995f,-2000,1000,1));
 }
 @Test public void disablingCurtainKeepsThePreviousAnimationPathAvailable(){panel.collapse();service.getSharedPreferences("veil_panel",0).edit().putBoolean("curtain_motion",false).commit();ReflectionHelpers.callInstanceMethod(panel,"open");assertNull(ReflectionHelpers.getField(panel,"curtain"));assertEquals(0,root().getTranslationY(),0);assertTrue(panel.isExpanded());}
}
