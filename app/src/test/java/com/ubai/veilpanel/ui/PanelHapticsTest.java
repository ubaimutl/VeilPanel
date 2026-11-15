package com.ubai.veilpanel.ui;

import android.content.Context;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class PanelHapticsTest {
 private Context context;private Vibrator vibrator;private View view;
 @Before public void setup(){
  context=RuntimeEnvironment.getApplication();view=new View(context);
  vibrator=context.getSystemService(VibratorManager.class).getDefaultVibrator();
  Shadows.shadowOf(vibrator).setHasVibrator(true);Shadows.shadowOf(vibrator).setHasAmplitudeControl(true);
  Settings.System.putInt(context.getContentResolver(),Settings.System.HAPTIC_FEEDBACK_ENABLED,1);
 }
 @Test public void overlayCueUsesAShortTouchVibrationWhenPredefinedTickIsUnavailable(){
  PanelHaptics.ready(view);var shadow=Shadows.shadowOf(vibrator);
  assertTrue(shadow.isVibrating());assertEquals(20,shadow.getMilliseconds());
  assertEquals(VibrationAttributes.USAGE_TOUCH,((VibrationAttributes)shadow.getVibrationAttributesFromLastVibration()).getUsage());
 }
 @Test public void systemAndViewHapticSettingsAreRespected(){
  Settings.System.putInt(context.getContentResolver(),Settings.System.HAPTIC_FEEDBACK_ENABLED,0);
  PanelHaptics.ready(view);assertFalse(Shadows.shadowOf(vibrator).isVibrating());
  Settings.System.putInt(context.getContentResolver(),Settings.System.HAPTIC_FEEDBACK_ENABLED,1);
  view.setHapticFeedbackEnabled(false);PanelHaptics.ready(view);assertFalse(Shadows.shadowOf(vibrator).isVibrating());
 }
 @Test public void missingVibratorFallsBackWithoutBreakingTheGesture(){
  Shadows.shadowOf(vibrator).setHasVibrator(false);PanelHaptics.ready(view);
  assertFalse(Shadows.shadowOf(vibrator).isVibrating());assertEquals(HapticFeedbackConstants.GESTURE_END,Shadows.shadowOf(view).lastHapticFeedbackPerformed());
 }
}
