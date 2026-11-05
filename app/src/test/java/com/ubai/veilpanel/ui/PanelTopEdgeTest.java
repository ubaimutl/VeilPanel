package com.ubai.veilpanel.ui;
import android.accessibilityservice.AccessibilityService;import android.app.KeyguardManager;import android.os.Looper;import android.view.*;
import com.ubai.veilpanel.accessibility.PanOSPAccessibilityServiceCore;import com.ubai.veilpanel.system.SystemShadeController;
import java.time.Duration;import java.util.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class PanelTopEdgeTest {
 public static class RecordingService extends PanOSPAccessibilityServiceCore{}
 private RecordingService service;private ShadeWindowController panel;
 @Before public void setup(){service=Robolectric.buildService(RecordingService.class).create().get();service.getSharedPreferences("veil_panel",0).edit().clear().putBoolean("curtain_motion",false).commit();Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);Shadows.shadowOf(service).setCanDispatchGestures(true);TestWallpaper.install(service);org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true);panel=new ShadeWindowController(service,new SystemShadeController(service));panel.start();ReflectionHelpers.callInstanceMethod(panel,"open");}
 @After public void close(){panel.stop();com.ubai.veilpanel.image.PowerSaverBackdrop.clear(service);}
 @Test public void leftPullStaysInVeilAndNeverLaunchesQuickSettings(){send(MotionEvent.ACTION_DOWN,5,2);send(MotionEvent.ACTION_MOVE,5,140);send(MotionEvent.ACTION_UP,5,180);idle(600);assertTrue(panel.isExpanded());assertFalse(Shadows.shadowOf(service).getGlobalActionsPerformed().contains(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS));assertNotNull(ReflectionHelpers.getField(panel,"topGuard"));}
 @Test public void rightTapDoesNotCloseButRightPullOpensNativeQuickSettingsAfterRelease(){float right=service.getResources().getDisplayMetrics().widthPixels-3;send(MotionEvent.ACTION_DOWN,right,2);send(MotionEvent.ACTION_UP,right,2);idle(150);assertTrue(panel.isExpanded());assertFalse(Shadows.shadowOf(service).getGlobalActionsPerformed().contains(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS));send(MotionEvent.ACTION_DOWN,right,2);send(MotionEvent.ACTION_MOVE,right,130);assertTrue(panel.isExpanded());send(MotionEvent.ACTION_UP,right,180);idle(48);assertFalse(panel.isExpanded());assertEquals(1,Shadows.shadowOf(service).getGesturesDispatched().size());assertFalse(Shadows.shadowOf(service).getGlobalActionsPerformed().contains(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS));assertNotNull(ReflectionHelpers.getField(panel,"topGuard"));}
 @Test public void cancelledLeftStreamIsSuppressedWithoutClosingPanel(){send(MotionEvent.ACTION_DOWN,5,2);send(MotionEvent.ACTION_CANCEL,5,2);idle(600);assertTrue(panel.isExpanded());assertFalse(Shadows.shadowOf(service).getGlobalActionsPerformed().contains(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS));}
 @Test public void cancelledRightStreamWaitsBeforeHandoff(){float right=service.getResources().getDisplayMetrics().widthPixels-3;send(MotionEvent.ACTION_DOWN,right,2);send(MotionEvent.ACTION_CANCEL,right,20);idle(200);assertTrue(panel.isExpanded());idle(400);assertFalse(panel.isExpanded());assertEquals(1,Shadows.shadowOf(service).getGesturesDispatched().size());}
 @Test public void lockedScreenCancelsThePendingRightSwipe(){float right=service.getResources().getDisplayMetrics().widthPixels-3;send(MotionEvent.ACTION_DOWN,right,2);send(MotionEvent.ACTION_MOVE,right,130);send(MotionEvent.ACTION_UP,right,180);idle(16);Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(true);idle(400);assertTrue(Shadows.shadowOf(service).getGesturesDispatched().isEmpty());}
 @Test public void orphanedEventsCannotReuseRightSideAfterReopening(){float right=service.getResources().getDisplayMetrics().widthPixels-3;send(MotionEvent.ACTION_DOWN,right,2);send(MotionEvent.ACTION_CANCEL,right,20);panel.collapse();panel.openFromShortcut();idle(100);int before=Shadows.shadowOf(service).getGesturesDispatched().size();send(MotionEvent.ACTION_CANCEL,right,200);send(MotionEvent.ACTION_UP,right,250);idle(600);assertTrue(panel.isExpanded());assertEquals(before,Shadows.shadowOf(service).getGesturesDispatched().size());}
 @Test public void freshLeftTouchCancelsPendingRightHandoff(){float right=service.getResources().getDisplayMetrics().widthPixels-3;send(MotionEvent.ACTION_DOWN,right,2);send(MotionEvent.ACTION_CANCEL,right,20);send(MotionEvent.ACTION_DOWN,5,2);send(MotionEvent.ACTION_MOVE,right,400);send(MotionEvent.ACTION_UP,right,500);idle(600);assertTrue(panel.isExpanded());assertTrue("A fresh left touch must not inject a native handoff",Shadows.shadowOf(service).getGesturesDispatched().isEmpty());}
 @Test public void leftPullKeepsItsOriginWhenFingerCrossesSplit(){float right=service.getResources().getDisplayMetrics().widthPixels-3;send(MotionEvent.ACTION_DOWN,5,2);send(MotionEvent.ACTION_MOVE,5,100);send(MotionEvent.ACTION_MOVE,right,600);send(MotionEvent.ACTION_CANCEL,right,600);idle(600);assertTrue(panel.isExpanded());assertTrue(Shadows.shadowOf(service).getGesturesDispatched().isEmpty());}
 // The reported failure happens after the old 360 ms opening checks expired.
 @Test public void nativeShadeIsStillDismissedDuringLateHeldOpening(){
  panel.collapse();openingTouch(MotionEvent.ACTION_DOWN,5,2);openingTouch(MotionEvent.ACTION_MOVE,5,200);
  idle(500);int early=dismissals();
  // Same pointer continues far outside the small original catcher; no UP.
  openingTouch(MotionEvent.ACTION_MOVE,5,2000);
  idle(1100);assertTrue("Late opening must still suppress the OEM shade",dismissals()>early);
  assertTrue(panel.isExpanded());
 }
 @Test public void openingSuppressionEndsAfterTwoSecondsEvenWithoutFingerUp(){
  idle(2100);int settled=dismissals();idle(6000);
  assertEquals("No permanent polling when the release is lost",settled,dismissals());
  assertTrue(panel.isExpanded());
 }
 @Test public void collapseCancelsLateOpeningChecks(){
  idle(500);panel.collapse();int before=dismissals();idle(2500);assertEquals(before,dismissals());
 }
 @Test public void lockStopsLateOpeningChecksWithoutNeedingAnAccessibilityEvent(){
  idle(500);Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(true);
  int before=dismissals();idle(2500);assertEquals(before,dismissals());
 }
 @Test public void cancelledRightHandoffStopsSuppressionWhileNativeGestureIsPending(){
  idle(500);float right=service.getResources().getDisplayMetrics().widthPixels-3;
  send(MotionEvent.ACTION_DOWN,right,2);send(MotionEvent.ACTION_CANCEL,right,20);
  int before=dismissals();idle(250);assertTrue(panel.isExpanded());assertEquals(before,dismissals());
 }
 @Test public void suspendCancelsLateOpeningChecks(){
  idle(500);panel.setSuspended(true);int before=dismissals();idle(2500);assertEquals(before,dismissals());
 }
 @Test public void openingNeverInjectsATapIntoTheStatusBar(){
  idle(500);assertTrue("Keep the actual finger stream instead of replacing it",Shadows.shadowOf(service).getGesturesDispatched().isEmpty());
 }
 @Test public void openingCatcherIsStableDuringCurtainAndAcrossClose(){
  panel.collapse();service.getSharedPreferences("veil_panel",0).edit().putBoolean("curtain_motion",true).commit();
  View catcher=ReflectionHelpers.getField(panel,"topGuard");
  catcherEvent(catcher,MotionEvent.ACTION_DOWN,5,1);catcherEvent(catcher,MotionEvent.ACTION_MOVE,5,200);
  assertTrue(panel.isExpanded());assertSame(catcher,ReflectionHelpers.getField(panel,"topGuard"));
  View root=ReflectionHelpers.getField(panel,"root");assertTrue(root.getTranslationY()<0);assertEquals(0,catcher.getTranslationY(),0);
  idle(500);catcherEvent(catcher,MotionEvent.ACTION_UP,5,2000);panel.collapse();
  assertSame("Collapse must retain the original input window",catcher,ReflectionHelpers.getField(panel,"topGuard"));
  WindowManager.LayoutParams bounds=(WindowManager.LayoutParams)catcher.getLayoutParams();assertTrue(bounds.width>0);
 }
 @Test public void originalLeftStreamCannotBecomeRightHandoffDuringLongHold(){
  panel.collapse();View catcher=ReflectionHelpers.getField(panel,"topGuard");
  catcherEvent(catcher,MotionEvent.ACTION_DOWN,5,1);catcherEvent(catcher,MotionEvent.ACTION_MOVE,5,200);idle(6000);
  float right=service.getResources().getDisplayMetrics().widthPixels-3;
  catcherEvent(catcher,MotionEvent.ACTION_MOVE,right,2000);catcherEvent(catcher,MotionEvent.ACTION_UP,right,2400);idle(600);
  assertTrue(panel.isExpanded());assertSame(catcher,ReflectionHelpers.getField(panel,"topGuard"));assertTrue(Shadows.shadowOf(service).getGesturesDispatched().isEmpty());
 }
 @Test public void stopRemovesThePersistentCatcher(){panel.stop();assertNull(ReflectionHelpers.getField(panel,"topGuard"));assertNull(ReflectionHelpers.getField(panel,"root"));}
 @Test public void cancelledPointerRecoveryCoversLaterNativeReopeningAndEndsOnFreshInput(){
  var shadow=Shadows.shadowOf(service);shadow.setSystemActions(java.util.List.of(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE,"Dismiss")));
  send(MotionEvent.ACTION_DOWN,5,1);send(MotionEvent.ACTION_CANCEL,5,120);idle(2200);int before=dismissals();idle(1800);
  assertTrue("OS can reopen after the two-second fallback",dismissals()>before);
  View root=ReflectionHelpers.getField(panel,"root");catcherEvent(root,MotionEvent.ACTION_DOWN,300,700);
  assertFalse(ReflectionHelpers.<Boolean>getField(panel,"recoveringNativeShade"));before=dismissals();idle(4000);assertEquals(before,dismissals());
 }
 @Test public void cancelledPointerRecoveryDoesNotIssueUnavailableSystemActions(){
  Shadows.shadowOf(service).setSystemActions(java.util.List.of());send(MotionEvent.ACTION_DOWN,5,1);send(MotionEvent.ACTION_CANCEL,5,120);idle(2200);
  int before=dismissals();idle(4000);assertEquals("Only ask to dismiss when SystemUI offers that action",before,dismissals());
  panel.collapse();assertFalse(ReflectionHelpers.<Boolean>getField(panel,"recoveringNativeShade"));
 }
 @Test public void cancelledOriginalOpeningStreamStartsRecovery(){
  panel.collapse();View catcher=ReflectionHelpers.getField(panel,"topGuard");
  catcherEvent(catcher,MotionEvent.ACTION_DOWN,5,0);catcherEvent(catcher,MotionEvent.ACTION_MOVE,5,150);catcherEvent(catcher,MotionEvent.ACTION_CANCEL,5,180);
  assertTrue(ReflectionHelpers.<Boolean>getField(panel,"recoveringNativeShade"));
  float right=service.getResources().getDisplayMetrics().widthPixels-3;catcherEvent(catcher,MotionEvent.ACTION_DOWN,right,1);
  assertFalse("Right-side native handoff must not fight recovery",ReflectionHelpers.<Boolean>getField(panel,"recoveringNativeShade"));
 }
 @Test public void freshTopPullReversesAnUnfinishedCurtainClose(){
  panel.collapse();service.getSharedPreferences("veil_panel",0).edit().putBoolean("curtain_motion",true).commit();panel.openFromShortcut();
  CurtainMotion motion=ReflectionHelpers.getField(panel,"curtain");motion.jump(1);
  ReflectionHelpers.callInstanceMethod(panel,"animateClose");
  for(long frame:new long[]{1000000000L,1016666666L,1033333332L})ReflectionHelpers.callInstanceMethod(motion,"step",ReflectionHelpers.ClassParameter.from(long.class,frame));
  assertTrue(ReflectionHelpers.<Boolean>getField(panel,"closing"));
  View catcher=ReflectionHelpers.getField(panel,"topGuard");catcherEvent(catcher,MotionEvent.ACTION_DOWN,5,0);catcherEvent(catcher,MotionEvent.ACTION_MOVE,5,200);idle(800);
  assertTrue("The previous close completion must not discard this new pull",panel.isExpanded());assertFalse(ReflectionHelpers.<Boolean>getField(panel,"closing"));
 }
 @Test public void freshTopPullCancelsLegacyCloseCompletion(){
  ReflectionHelpers.callInstanceMethod(panel,"animateClose");
  View catcher=ReflectionHelpers.getField(panel,"topGuard");catcherEvent(catcher,MotionEvent.ACTION_DOWN,5,0);idle(800);assertTrue(panel.isExpanded());
 }
 @Test public void recoveryCoverWaitsForQuietNativeShadeAndClearsOnClose(){
  var shadow=Shadows.shadowOf(service);shadow.setSystemActions(java.util.List.of(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE,"Dismiss")));
  send(MotionEvent.ACTION_DOWN,5,1);send(MotionEvent.ACTION_CANCEL,5,120);idle(3000);
  assertTrue(ReflectionHelpers.<Boolean>getField(panel,"statusRecoveryCovered"));
  shadow.setSystemActions(java.util.List.of());idle(200);assertTrue(ReflectionHelpers.<Boolean>getField(panel,"statusRecoveryCovered"));
  idle(300);assertFalse(ReflectionHelpers.<Boolean>getField(panel,"statusRecoveryCovered"));
  shadow.setSystemActions(java.util.List.of(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE,"Dismiss")));idle(150);
  assertTrue(ReflectionHelpers.<Boolean>getField(panel,"statusRecoveryCovered"));panel.collapse();
  assertFalse(ReflectionHelpers.<Boolean>getField(panel,"statusRecoveryCovered"));idle(1500);assertFalse(ReflectionHelpers.<Boolean>getField(panel,"statusRecoveryCovered"));
 }
 @Test public void statusCoverCanBeDisabledWithoutDisablingRecovery(){
  service.getSharedPreferences("veil_panel",0).edit().putBoolean("recovery_status_cover",false).commit();
  send(MotionEvent.ACTION_DOWN,5,1);send(MotionEvent.ACTION_CANCEL,5,120);
  assertTrue(ReflectionHelpers.<Boolean>getField(panel,"recoveringNativeShade"));assertFalse(ReflectionHelpers.<Boolean>getField(panel,"statusRecoveryCovered"));
 }
 @Test public void lostGestureRecoveryIsEnabledByDefaultAndCanBeTurnedOffLive(){
  send(MotionEvent.ACTION_DOWN,5,1);send(MotionEvent.ACTION_CANCEL,5,120);
  assertTrue(ReflectionHelpers.<Boolean>getField(panel,"recoveringNativeShade"));
  service.getSharedPreferences("veil_panel",0).edit().putBoolean("native_edge_recovery",false).commit();idle(1);
  assertFalse(ReflectionHelpers.<Boolean>getField(panel,"recoveringNativeShade"));assertFalse(ReflectionHelpers.<Boolean>getField(panel,"statusRecoveryCovered"));
  send(MotionEvent.ACTION_DOWN,5,1);send(MotionEvent.ACTION_CANCEL,5,120);idle(1000);
  assertFalse(ReflectionHelpers.<Boolean>getField(panel,"recoveringNativeShade"));assertTrue(panel.isExpanded());
 }
 private void catcherEvent(View catcher,int action,float x,float y){
  MotionEvent event=MotionEvent.obtain(0,10,action,x,y,0);assertTrue(catcher.dispatchTouchEvent(event));event.recycle();
 }
 private void openingTouch(int action,float x,float y){
  MotionEvent event=MotionEvent.obtain(0,10,action,x,y,0);View root=ReflectionHelpers.getField(panel,"root");
  ReflectionHelpers.callInstanceMethod(panel,"touch",ReflectionHelpers.ClassParameter.from(View.class,root),ReflectionHelpers.ClassParameter.from(MotionEvent.class,event));event.recycle();
 }
 private int dismissals(){return java.util.Collections.frequency(Shadows.shadowOf(service).getGlobalActionsPerformed(),AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);}
 private void send(int action,float x,float y){MotionEvent event=MotionEvent.obtain(0,10,action,x,y,0);View guard=ReflectionHelpers.getField(panel,"topGuard");ReflectionHelpers.callInstanceMethod(panel,"topEdgeTouch",ReflectionHelpers.ClassParameter.from(View.class,guard),ReflectionHelpers.ClassParameter.from(MotionEvent.class,event));event.recycle();}
 private void idle(long millis){Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis));}
}
