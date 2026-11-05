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
    @Test public void visibilitySensorIsBelowSystemUiAndCannotTakeTouchesOrFocus(){
        Object monitor=ReflectionHelpers.getField(panel,"barMonitor");View sensor=ReflectionHelpers.getField(monitor,"sensor");assertNotNull(sensor);
        WindowManager.LayoutParams p=(WindowManager.LayoutParams)sensor.getLayoutParams();
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,p.type);assertEquals(0,p.alpha,0);
        assertTrue((p.flags&WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)!=0);assertTrue((p.flags&WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)!=0);
        sensor.dispatchApplyWindowInsets(new WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(),false).build());assertTrue(sensorHeight()>dp(4));
    }
}
