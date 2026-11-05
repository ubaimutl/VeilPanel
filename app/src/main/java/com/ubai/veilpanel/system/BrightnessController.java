package com.ubai.veilpanel.system;

import android.content.Context;
import android.provider.Settings;

/**
 * System brightness controller.
 *
 * WRITE_SETTINGS is user-granted through Android settings. The UI must check
 * canWrite() before attempting a change.
 */
public final class BrightnessController {

    public static final int MIN_BRIGHTNESS = 1;
    public static final int MAX_BRIGHTNESS = 255;

    private final Context context;

    public BrightnessController(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean canWrite() {
        return Settings.System.canWrite(context);
    }

    public int getBrightness() {
        return clamp(
                Settings.System.getInt(
                        context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS,
                        128
                )
        );
    }

    public boolean setBrightness(int value) {
        if (!canWrite()) {
            return false;
        }

        return Settings.System.putInt(
                context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                clamp(value)
        );
    }

    public boolean isAutomatic() {
        return Settings.System.getInt(
                context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    }

    public boolean setAutomatic(boolean automatic) {
        if (!canWrite()) {
            return false;
        }

        return Settings.System.putInt(
                context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                automatic
                        ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                        : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        );
    }

    private int clamp(int value) {
        return Math.max(
                MIN_BRIGHTNESS,
                Math.min(MAX_BRIGHTNESS, value)
        );
    }
}
