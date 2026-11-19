package com.ubai.veilpanel.ui;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.*;
import android.widget.FrameLayout;
import com.ubai.veilpanel.config.BottomActionOptions;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class BottomPanelActionsTest {
 private AccessibilityService service;private android.content.SharedPreferences prefs;private int closes;private BottomPanelActions actions;
 @Before public void setup(){
  service=Robolectric.buildService(com.ubai.veilpanel.accessibility.PanOSPAccessibilityServiceCore.class).create().get();prefs=service.getSharedPreferences("veil_panel",0);prefs.edit().clear().commit();
  Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setIsDeviceLocked(false);
  Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.CAMERA);
 }
 @After public void close(){if(actions!=null)actions.close();}
 private BottomPanelActions create(){actions=BottomPanelActions.create(service,()->closes++,2000,24);return actions;}
 @Test public void disabledByDefaultAndBothHiddenCreateNoButtons(){
  assertNull(create());prefs.edit().putBoolean("bottom_actions_enabled",true).putString("bottom_action_left","none").putString("bottom_action_right","none").commit();assertNull(create());
 }
 @Test public void chosenBottomDistanceIsRespectedAndHidesWhenThereIsNoRoom(){
  prefs.edit().putBoolean("bottom_actions_enabled",true).putInt("dismiss_handle_bottom",100).putInt("dismiss_grab_height",100).putInt("bottom_action_bottom",48).commit();assertNotNull(create());
  float density=service.getResources().getDisplayMetrics().density;assertEquals(Math.round(48*density),actions.placement().bottomMargin);assertTrue(actions.reservedBottom()>actions.placement().bottomMargin+actions.placement().height);
  assertNull(BottomPanelActions.create(service,()->{},100,24));
 }
 @Test public void buttonsCanReachTheEdgeWithoutTheHandleGuardCoveringThem(){
  assertEquals(48,BottomActionOptions.bottom(service));assertFalse(BottomActionOptions.fade(service));
  prefs.edit().putBoolean("bottom_actions_enabled",true).putInt("bottom_action_bottom",0).commit();create();assertEquals(0,actions.placement().bottomMargin);
  float density=service.getResources().getDisplayMetrics().density;int width=Math.round(374*density),preferred=Math.round(220*density);
  int guard=BottomActionOptions.handleGuardWidth(service,width,preferred),side=BottomActionOptions.sidePixels(service,width),size=Math.round(BottomActionOptions.size(service)*density);
  assertTrue((width-guard)/2>=side+size);assertTrue(guard>=48*density);
  prefs.edit().putBoolean("bottom_actions_enabled",false).commit();assertEquals(preferred,BottomActionOptions.handleGuardWidth(service,width,preferred));
 }
 @Test public void fadeReservesSpaceToKeepTheLastNotificationAboveTheTransition(){
  prefs.edit().putBoolean("bottom_actions_enabled",true).commit();create();int normal=actions.reservedBottom();prefs.edit().putBoolean("bottom_actions_fade",true).commit();assertEquals(normal+Math.round(64*service.getResources().getDisplayMetrics().density),actions.reservedBottom());
 }
 @Test public void cameraLaunchesCameraAndClosesPanelOnlyAfterLaunch(){
  prefs.edit().putBoolean("bottom_actions_enabled",true).putString("bottom_action_left","camera").putString("bottom_action_right","none").commit();create().findViewWithTag("bottom_action_left").performClick();
  Intent intent=Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();assertEquals(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA,intent.getAction());assertEquals(1,closes);
 }
 @Test public void flashlightWithoutPermissionOpensOnlyItsSettingsPermissionFlow(){
  prefs.edit().putBoolean("bottom_actions_enabled",true).commit();create().findViewWithTag("bottom_action_left").performClick();
  Intent intent=Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();assertTrue(intent.getBooleanExtra("request_flashlight_permission",false));assertTrue(intent.getBooleanExtra("open_bottom_actions_settings",false));assertEquals(1,closes);
 }
 @Test public void lockedAndClosedButtonsCannotLaunchOrPerformActions(){
  prefs.edit().putBoolean("bottom_actions_enabled",true).putString("bottom_action_left","close").putString("bottom_action_right","settings").commit();create();
  Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(true);actions.findViewWithTag("bottom_action_left").performClick();actions.findViewWithTag("bottom_action_right").performClick();assertEquals(0,closes);assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity());
  Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);actions.close();actions.findViewWithTag("bottom_action_left").performClick();assertEquals(0,closes);
 }
 @Test public void singleCloseButtonAndInvalidPreferencesRemainSafe(){
  prefs.edit().putBoolean("bottom_actions_enabled",true).putString("bottom_action_left","none").putString("bottom_action_right","close").putInt("bottom_action_size",900).putInt("bottom_action_side",900).commit();create();assertEquals(android.view.View.GONE,actions.findViewWithTag("bottom_action_left").getVisibility());assertEquals(72,BottomActionOptions.size(service));assertEquals(64,BottomActionOptions.side(service));
  actions.findViewWithTag("bottom_action_right").performClick();assertEquals(1,closes);
 }
}
