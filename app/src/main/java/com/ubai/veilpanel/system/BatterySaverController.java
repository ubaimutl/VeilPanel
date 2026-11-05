package com.ubai.veilpanel.system;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Reads the real Android Battery Saver state and exposes an optional
 * privileged direct setter for development/privileged PanOSP builds.
 *
 * Android's power service observes the global "low_power" setting on
 * Android 15/16. A normal third-party app cannot write it; PanOSP only tries
 * this path when WRITE_SECURE_SETTINGS has actually been granted.
 *
 * Some devices can temporarily leave Settings.Global.low_power out of sync
 * with PowerManager.isPowerSaveMode(). When that happens, writing the same
 * stale value again does not create a state transition. PanOSP first restores
 * the setting to the real state, then writes the requested state shortly
 * afterwards so the system observer receives a genuine edge.
 */
public final class BatterySaverController {

    private static final String TAG = "PanOSP.BatterySaver";

    private static final String WRITE_SECURE_SETTINGS =
            "android.permission.WRITE_SECURE_SETTINGS";

    /*
     * Settings.Global.LOW_POWER_MODE is hidden from the public SDK even though
     * the Android framework uses this exact key internally.
     */
    private static final String GLOBAL_LOW_POWER_MODE = "low_power";

    /*
     * Gives the system settings observer time to process the normalization
     * write before the requested value is written again.
     */
    private static final long RESYNC_DELAY_MS = 250L;

    private final Context context;
    private final ContentResolver contentResolver;
    private final PowerManager powerManager;
    private final Handler mainHandler;

    private Runnable pendingResyncWrite;

    public BatterySaverController(Context context) {
        this.context = context.getApplicationContext();
        contentResolver = this.context.getContentResolver();
        powerManager = this.context.getSystemService(PowerManager.class);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public boolean isAvailable() {
        return powerManager != null;
    }

    public boolean isEnabled() {
        return powerManager != null
                && powerManager.isPowerSaveMode();
    }

    public boolean canSetEnabledDirectly() {
        return context.checkSelfPermission(WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests Battery Saver through the global setting observed by Android's
     * BatterySaverStateMachine. The real state remains authoritative and is
     * always read back through PowerManager.isPowerSaveMode().
     */
    public boolean setEnabled(boolean enabled) {
        if (!canSetEnabledDirectly() || powerManager == null) {
            return false;
        }

        cancelPendingResyncWrite();

        try {
            boolean realEnabled = powerManager.isPowerSaveMode();

            if (realEnabled == enabled) {
                return true;
            }

            int realValue = realEnabled ? 1 : 0;
            int targetValue = enabled ? 1 : 0;
            int storedValue = Settings.Global.getInt(
                    contentResolver,
                    GLOBAL_LOW_POWER_MODE,
                    realValue
            );

            /*
             * Example seen on HyperOS:
             *   PowerManager.isPowerSaveMode() == false
             *   Settings.Global.low_power      == 1
             *
             * A new write of 1 is accepted by SettingsProvider but does not
             * create the change Android's BatterySaverStateMachine needs.
             * Normalize to the real state first, then request the target.
             */
            if (storedValue != realValue && storedValue == targetValue) {
                boolean normalized = writeLowPowerSetting(realEnabled, "resync");

                if (!normalized) {
                    return false;
                }

                Log.w(
                        TAG,
                        "low_power was stale: stored="
                                + storedValue
                                + " real="
                                + realValue
                                + " target="
                                + targetValue
                                + "; normalizing before retry"
                );

                pendingResyncWrite = () -> {
                    pendingResyncWrite = null;
                    writeLowPowerSetting(enabled, "retry");
                };

                mainHandler.postDelayed(
                        pendingResyncWrite,
                        RESYNC_DELAY_MS
                );

                return true;
            }

            return writeLowPowerSetting(enabled, "direct");

        } catch (SecurityException exception) {
            Log.w(
                    TAG,
                    "WRITE_SECURE_SETTINGS rejected for Battery Saver",
                    exception
            );
            return false;

        } catch (RuntimeException exception) {
            Log.w(
                    TAG,
                    "Battery Saver global-setting operation failed",
                    exception
            );
            return false;
        }
    }

    private boolean writeLowPowerSetting(boolean enabled, String reason) {
        try {
            boolean written = Settings.Global.putInt(
                    contentResolver,
                    GLOBAL_LOW_POWER_MODE,
                    enabled ? 1 : 0
            );

            Log.i(
                    TAG,
                    "global low_power write reason="
                            + reason
                            + " target="
                            + enabled
                            + " result="
                            + written
            );

            return written;

        } catch (SecurityException exception) {
            Log.w(
                    TAG,
                    "WRITE_SECURE_SETTINGS rejected for Battery Saver",
                    exception
            );
            return false;

        } catch (RuntimeException exception) {
            Log.w(
                    TAG,
                    "Battery Saver global-setting write failed",
                    exception
            );
            return false;
        }
    }

    private void cancelPendingResyncWrite() {
        if (pendingResyncWrite == null) {
            return;
        }

        mainHandler.removeCallbacks(pendingResyncWrite);
        pendingResyncWrite = null;
    }
}
