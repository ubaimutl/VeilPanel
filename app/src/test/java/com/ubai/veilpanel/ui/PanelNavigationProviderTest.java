package com.ubai.veilpanel.ui;

import static org.junit.Assert.*;
import android.app.KeyguardManager;
import android.os.Looper;
import android.provider.Settings;
import com.ubai.veilpanel.accessibility.*;
import com.ubai.veilpanel.system.SystemShadeController;
import java.lang.ref.WeakReference;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBinder;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class PanelNavigationProviderTest {
 private PanOSPAccessibilityServiceCore service;
 private ShadeWindowController panel;
 private PanelNavigationProvider provider;
 @Before public void setup(){
  service=Robolectric.buildService(PanOSPAccessibilityServiceCore.class).create().get();
  Shadows.shadowOf(service.getSystemService(KeyguardManager.class)).setKeyguardLocked(false);
  org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true);TestWallpaper.install(service);
  panel=new ShadeWindowController(service,new SystemShadeController(service));panel.start();panel.openFromShortcut();
  ReflectionHelpers.setField(service,"panel",panel);
  ReflectionHelpers.setStaticField(PanOSPAccessibilityServiceCore.class,"active",new WeakReference<>(service));
  provider=Robolectric.buildContentProvider(PanelNavigationProvider.class).create().get();
  ShadowBinder.setCallingUid(12345);
  Shadows.shadowOf(service.getPackageManager()).setPackagesForUid(12345,"example.gestures");
 }
 @After public void close(){panel.stop();ReflectionHelpers.setStaticField(PanOSPAccessibilityServiceCore.class,"active",new WeakReference<>(null));ShadowBinder.reset();com.ubai.veilpanel.image.PowerSaverBackdrop.clear(service);}
 @Test public void unapprovedAppCannotDismissThePanel(){
  Settings.Secure.putString(service.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,"other.app/.Service");
  assertFalse(provider.call("dismissIfOpen",null,null).getBoolean("handled"));
  Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(panel.isExpanded());
 }
 @Test public void enabledGestureServiceDismissesOnceWithoutNavigating(){
  Settings.Secure.putString(service.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,"example.gestures/.Service");
  int actions=Shadows.shadowOf(service).getGlobalActionsPerformed().size();
  assertTrue(provider.call("dismissIfOpen",null,null).getBoolean("handled"));
  Shadows.shadowOf(Looper.getMainLooper()).idle();assertFalse(panel.isExpanded());
  assertFalse(provider.call("dismissIfOpen",null,null).getBoolean("handled"));
  assertEquals(actions,Shadows.shadowOf(service).getGlobalActionsPerformed().size());
 }
 @Test public void endpointDoesNotAcceptOtherOperations(){
  Settings.Secure.putString(service.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,"example.gestures/.Service");
  assertFalse(provider.call("open",null,null).getBoolean("handled"));assertTrue(panel.isExpanded());
 }
}
