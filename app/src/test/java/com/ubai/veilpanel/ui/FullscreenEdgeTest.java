package com.ubai.veilpanel.ui;

import static org.junit.Assert.*;
import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.view.*;
import com.ubai.veilpanel.system.SystemShadeController;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class FullscreenEdgeTest {
    private AccessibilityService service;
    private ShadeWindowController panel;
    private View root;
    @Before public void setup() {
        service=Robolectric.buildService(com.ubai.veilpanel.accessibility.PanOSPAccessibilityServiceCore.class).create().get();
        service.getSharedPreferences("veil_panel",0).edit().clear().commit();
        Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);
        TestWallpaper.install(service);
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true);
        panel=new ShadeWindowController(service,new SystemShadeController(service));panel.start();
        root=ReflectionHelpers.getField(panel,"root");
    }
    @After public void close(){panel.stop();com.ubai.veilpanel.image.PowerSaverBackdrop.clear(service);}
    private void bars(boolean visible){ReflectionHelpers.callInstanceMethod(panel,"statusBarVisibilityChanged",ReflectionHelpers.ClassParameter.from(boolean.class,visible));}
    private boolean untouchable(){WindowManager.LayoutParams p=ReflectionHelpers.getField(panel,"params");return (p.flags&WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)!=0;}
    private boolean touch(int action,int y){MotionEvent e=MotionEvent.obtain(0,10,action,5,y,0);boolean handled=ReflectionHelpers.callInstanceMethod(panel,"touch",ReflectionHelpers.ClassParameter.from(View.class,root),ReflectionHelpers.ClassParameter.from(MotionEvent.class,e));e.recycle();return handled;}
    private int sensorHeight(){WindowManager.LayoutParams p=ReflectionHelpers.getField(panel,"params");return p.height;}
    private int dp(int value){return Math.round(value*service.getResources().getDisplayMetrics().density);}
    @Test public void fullscreenTapNeverOpensOrArmsThePanel(){
        bars(false);assertFalse(untouchable());assertTrue(sensorHeight()>dp(4));
        touch(MotionEvent.ACTION_DOWN,2);touch(MotionEvent.ACTION_UP,2);
        assertFalse(panel.isExpanded());assertTrue(sensorHeight()>dp(4));
        assertTrue(Shadows.shadowOf(service).getGlobalActionsPerformed().isEmpty());
    }
    @Test public void firstPullIsRevealOnlyEvenWhenNativeTransientInsetsStayHidden(){
        bars(false);touch(MotionEvent.ACTION_DOWN,dp(8));assertFalse(panel.shouldSuppressSystemShade());
        touch(MotionEvent.ACTION_MOVE,180);touch(MotionEvent.ACTION_MOVE,300);assertFalse(panel.isExpanded());
        touch(MotionEvent.ACTION_CANCEL,300);assertFalse(panel.isExpanded());assertTrue(sensorHeight()>dp(4));
        assertTrue(Shadows.shadowOf(service).getGlobalActionsPerformed().isEmpty());
        touch(MotionEvent.ACTION_DOWN,2);touch(MotionEvent.ACTION_MOVE,180);assertTrue(panel.isExpanded());
    }
    @Test public void upFinishesRevealAndFreshSecondGestureIsRequired(){
        bars(false);touch(MotionEvent.ACTION_DOWN,2);touch(MotionEvent.ACTION_MOVE,300);assertFalse(panel.isExpanded());
        touch(MotionEvent.ACTION_UP,300);assertFalse(panel.isExpanded());
        touch(MotionEvent.ACTION_DOWN,2);touch(MotionEvent.ACTION_CANCEL,2);assertTrue(panel.isExpanded());
    }
    @Test public void expiredRevealNeedsTwoPullsAgain(){
        ((FullscreenBarMonitor)ReflectionHelpers.getField(panel,"barMonitor")).close();
        bars(false);touch(MotionEvent.ACTION_DOWN,2);touch(MotionEvent.ACTION_CANCEL,2);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(2251));
        assertTrue(sensorHeight()>dp(4));touch(MotionEvent.ACTION_DOWN,2);touch(MotionEvent.ACTION_MOVE,180);
        assertFalse(panel.isExpanded());
    }
    @Test public void revealingPermanentBarsMidSwipeDoesNotReuseThatGesture(){
        bars(false);touch(MotionEvent.ACTION_DOWN,2);bars(true);touch(MotionEvent.ACTION_CANCEL,180);assertFalse(panel.isExpanded());
        touch(MotionEvent.ACTION_DOWN,2);touch(MotionEvent.ACTION_MOVE,180);assertTrue(panel.isExpanded());
    }
    @Test public void openPanelRemainsInteractiveWithItsStatusBarHidden(){
        panel.openFromShortcut();bars(false);assertFalse(untouchable());assertTrue(panel.isExpanded());
        panel.collapse();assertTrue(sensorHeight()>dp(4));
    }
    @Test public void lockClearsTheArmedSecondPull(){
        bars(false);touch(MotionEvent.ACTION_DOWN,2);touch(MotionEvent.ACTION_CANCEL,2);
        panel.setSuspended(true);assertTrue(untouchable());panel.setSuspended(false);
        assertTrue(sensorHeight()>dp(4));touch(MotionEvent.ACTION_DOWN,2);touch(MotionEvent.ACTION_MOVE,180);assertFalse(panel.isExpanded());
    }
    @Test public void protectionCanBeDisabledWithoutChangingPanelAppearance(){
        bars(false);service.getSharedPreferences("veil_panel",0).edit().putBoolean("fullscreen_two_swipe",false).commit();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        touch(MotionEvent.ACTION_DOWN,2);touch(MotionEvent.ACTION_MOVE,180);assertTrue(panel.isExpanded());
    }
    @Test public void statusBarOnlyHiddenDoesNotRequireARevealPull(){
        Object monitor=ReflectionHelpers.getField(panel,"barMonitor");View sensor=ReflectionHelpers.getField(monitor,"sensor");
        sensor.dispatchApplyWindowInsets(new WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(),false).setVisible(WindowInsets.Type.navigationBars(),true).build());
        touch(MotionEvent.ACTION_DOWN,dp(8));assertTrue(panel.shouldSuppressSystemShade());
        touch(MotionEvent.ACTION_MOVE,180);assertTrue(panel.isExpanded());
    }
    private void noNavigationSourceStatusHidden(){
        Object monitor=ReflectionHelpers.getField(panel,"barMonitor");View sensor=ReflectionHelpers.getField(monitor,"sensor");
        sensor.dispatchApplyWindowInsets(new WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(),false).setVisible(WindowInsets.Type.navigationBars(),false).build());
    }
    private void nativeShadeOpen(boolean open){
        Shadows.shadowOf(service).setSystemActions(open?java.util.List.of(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE,"Dismiss")):java.util.List.of());
    }
    private void idle(long ms){Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(ms));}
    // Status-only app on a device without a navigation inset: SystemUI opens its shade.
    @Test public void withoutNavigationSourceVeilReplacesTheNativeShadeOnTheFirstPull(){
        noNavigationSourceStatusHidden();nativeShadeOpen(false);
        touch(MotionEvent.ACTION_DOWN,dp(8));touch(MotionEvent.ACTION_MOVE,180);assertFalse(panel.isExpanded());
        nativeShadeOpen(true);idle(100);
        assertTrue(panel.isExpanded());
        assertTrue(Shadows.shadowOf(service).getGlobalActionsPerformed().contains(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE));
    }
    @Test public void nativeShadeAppearingJustAfterReleaseIsStillReplaced(){
        noNavigationSourceStatusHidden();nativeShadeOpen(false);
        touch(MotionEvent.ACTION_DOWN,dp(8));touch(MotionEvent.ACTION_MOVE,180);touch(MotionEvent.ACTION_CANCEL,180);
        idle(200);assertFalse(panel.isExpanded());nativeShadeOpen(true);idle(100);assertTrue(panel.isExpanded());
    }
    // Immersive app on a device without a navigation inset: SystemUI only reveals bars.
    @Test public void withoutNavigationSourceImmersiveAppStillNeedsTwoPulls(){
        noNavigationSourceStatusHidden();nativeShadeOpen(false);
        touch(MotionEvent.ACTION_DOWN,dp(8));touch(MotionEvent.ACTION_MOVE,180);touch(MotionEvent.ACTION_UP,180);
        idle(1000);assertFalse(panel.isExpanded());
        assertTrue(Shadows.shadowOf(service).getGlobalActionsPerformed().isEmpty());
        touch(MotionEvent.ACTION_DOWN,dp(8));touch(MotionEvent.ACTION_MOVE,180);assertTrue(panel.isExpanded());
    }
    @Test public void nativeShadeWatchEndsSoALaterShadeIsNotHijacked(){
        noNavigationSourceStatusHidden();nativeShadeOpen(false);
        touch(MotionEvent.ACTION_DOWN,dp(8));touch(MotionEvent.ACTION_MOVE,180);touch(MotionEvent.ACTION_UP,180);
        idle(700);nativeShadeOpen(true);idle(1000);
        assertFalse(panel.isExpanded());assertTrue(Shadows.shadowOf(service).getGlobalActionsPerformed().isEmpty());
    }
    @Test public void devicesWithANavigationBarDoNotWatchTheNativeShade(){
        Object monitor=ReflectionHelpers.getField(panel,"barMonitor");View sensor=ReflectionHelpers.getField(monitor,"sensor");
        sensor.dispatchApplyWindowInsets(new WindowInsets.Builder().setInsetsIgnoringVisibility(WindowInsets.Type.navigationBars(),android.graphics.Insets.of(0,0,0,24)).setVisible(WindowInsets.Type.statusBars(),false).setVisible(WindowInsets.Type.navigationBars(),false).build());
        nativeShadeOpen(false);touch(MotionEvent.ACTION_DOWN,dp(8));touch(MotionEvent.ACTION_MOVE,180);
        nativeShadeOpen(true);idle(200);assertFalse(panel.isExpanded());
    }
    @Test public void existingNavigationBarHiddenWithStatusBarStillRequiresTwoPulls(){
        Object monitor=ReflectionHelpers.getField(panel,"barMonitor");View sensor=ReflectionHelpers.getField(monitor,"sensor");
        sensor.dispatchApplyWindowInsets(new WindowInsets.Builder().setInsetsIgnoringVisibility(WindowInsets.Type.navigationBars(),android.graphics.Insets.of(0,0,0,24)).setVisible(WindowInsets.Type.statusBars(),false).setVisible(WindowInsets.Type.navigationBars(),false).build());
        touch(MotionEvent.ACTION_DOWN,dp(8));touch(MotionEvent.ACTION_MOVE,180);assertFalse(panel.isExpanded());touch(MotionEvent.ACTION_CANCEL,180);
        touch(MotionEvent.ACTION_DOWN,dp(8));touch(MotionEvent.ACTION_MOVE,180);assertTrue(panel.isExpanded());
    }
    @Test public void visibilitySensorIsBelowSystemUiAndCannotTakeTouchesOrFocus(){
        Object monitor=ReflectionHelpers.getField(panel,"barMonitor");View sensor=ReflectionHelpers.getField(monitor,"sensor");assertNotNull(sensor);
        WindowManager.LayoutParams p=(WindowManager.LayoutParams)sensor.getLayoutParams();
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,p.type);assertEquals(0,p.alpha,0);
        assertTrue((p.flags&WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)!=0);assertTrue((p.flags&WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)!=0);
        sensor.dispatchApplyWindowInsets(new WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(),false).build());assertTrue(sensorHeight()>dp(4));
    }
}
