package com.ubai.veilpanel.system;

import android.content.Context;
import android.provider.Settings;

/**
 * Reads and writes Android's user auto-rotation preference.
 */
public final class RotationController {

    private final Context context;

    public RotationController(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean canWrite() {
        return Settings.System.canWrite(context);
    }

    public boolean isAutoRotationEnabled() {
        try {
            return Settings.System.getInt(
                    context.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION,
                    0
            ) == 1;
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean toggle() {
        if (!canWrite()) {
            return false;
        }

        boolean target = !isAutoRotationEnabled();

        return Settings.System.putInt(
                context.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION,
                target ? 1 : 0
        );
    }
}
