package com.ubai.veilpanel.system;

import android.content.Context;
import android.provider.Settings;

/**
 * Reads Android's global Airplane mode state.
 *
 * The public SDK does not allow an ordinary app to change the global Airplane
 * mode switch directly, so the tile opens the dedicated Android settings page.
 */
public final class AirplaneModeController {

    private final Context context;

    public AirplaneModeController(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isEnabled() {
        try {
            return Settings.Global.getInt(
                    context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON,
                    0
            ) == 1;

        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
