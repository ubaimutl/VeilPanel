package com.ubai.veilpanel.system;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Reads and, when PanOSP has WRITE_SECURE_SETTINGS, changes the global
 * Location master switch.
 *
 * Android 15/16 still backs the system LocationManager state with the secure
 * "location_mode" setting. A normal Play-installed app cannot write it, but a
 * development/privileged PanOSP build can when WRITE_SECURE_SETTINGS has been
 * explicitly granted (for example through ADB during development).
 */
public final class LocationController {

    private static final String TAG = "PanOSP.Location";

    private static final String WRITE_SECURE_SETTINGS =
            "android.permission.WRITE_SECURE_SETTINGS";

    private final Context context;
    private final LocationManager locationManager;

    public LocationController(Context context) {
        this.context = context.getApplicationContext();
        locationManager =
                this.context.getSystemService(LocationManager.class);
    }

    public boolean isAvailable() {
        return locationManager != null;
    }

    public boolean isEnabled() {
        if (locationManager == null) {
            return false;
        }

        try {
            return locationManager.isLocationEnabled();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /**
     * True only when the privileged permission has really been granted.
     * Merely declaring it in AndroidManifest.xml is not enough.
     */
    public boolean canSetEnabledDirectly() {
        return context.checkSelfPermission(WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Changes the global Location master state without opening Android
     * Settings. Returns false immediately when the privileged path is not
     * available or the write is rejected.
     */
    @SuppressWarnings("deprecation")
    public boolean setEnabled(boolean enabled) {
        if (!canSetEnabledDirectly()) {
            return false;
        }

        int mode = enabled
                ? Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
                : Settings.Secure.LOCATION_MODE_OFF;

        try {
            boolean written = Settings.Secure.putInt(
                    context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE,
                    mode
            );

            Log.i(
                    TAG,
                    "secure location write target="
                            + enabled
                            + " result="
                            + written
            );

            return written;

        } catch (SecurityException exception) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS rejected for Location", exception);
            return false;

        } catch (RuntimeException exception) {
            Log.w(TAG, "Location secure-setting write failed", exception);
            return false;
        }
    }
}
