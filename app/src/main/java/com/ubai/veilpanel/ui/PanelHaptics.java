package com.ubai.veilpanel.ui;

import android.content.Context;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;

/** A brief readiness cue that also works from an accessibility overlay. */
final class PanelHaptics {
    private PanelHaptics() {}
    static void ready(View view) {
        Context context=view.getContext();
        if (!view.isHapticFeedbackEnabled() || Settings.System.getInt(context.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED,1)==0) return;
        try {
            VibratorManager manager=context.getSystemService(VibratorManager.class);
            Vibrator vibrator=manager==null?null:manager.getDefaultVibrator();
            if (vibrator!=null && vibrator.hasVibrator()) {
                int support=vibrator.areEffectsSupported(VibrationEffect.EFFECT_TICK)[0];
                VibrationEffect effect=support==Vibrator.VIBRATION_EFFECT_SUPPORT_YES
                        ?VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                        :VibrationEffect.createOneShot(20,vibrator.hasAmplitudeControl()?80:VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect,new VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build());
                return;
            }
        } catch (RuntimeException unavailable) {
            // Keep the gesture usable when an OEM refuses direct vibration.
        }
        view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END);
    }
}
