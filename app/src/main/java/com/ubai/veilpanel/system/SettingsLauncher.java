package com.ubai.veilpanel.system;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;

import com.ubai.veilpanel.MainActivity;

/**
 * Centralizes transitions from the PanOSP shade to Android settings.
 *
 * External settings are always opened after the shade is closed so the overlay
 * never blocks the system UI the user needs to interact with.
 */
public final class SettingsLauncher {

    private static final String ACTION_TETHER_SETTINGS =
            "android.settings.TETHER_SETTINGS";

    private static final String ACTION_ZEN_MODE_SETTINGS =
            "android.settings.ZEN_MODE_SETTINGS";

    private final Context context;
    private final Runnable beforeExternalActivity;

    public SettingsLauncher(
            Context context,
            Runnable beforeExternalActivity) {

        this.context = context;
        this.beforeExternalActivity = beforeExternalActivity;
    }

    // =========================================================================
    // CONNECTIVITY
    // =========================================================================

    public void openWifiPanel() {
        if (!openAction(Settings.Panel.ACTION_WIFI)) {
            openAction(Settings.ACTION_WIFI_SETTINGS);
        }
    }

    public void openInternetPanel() {
        if (!openAction(
                Settings.Panel.ACTION_INTERNET_CONNECTIVITY)) {

            openAction(Settings.ACTION_WIRELESS_SETTINGS);
        }
    }

    public void openBluetoothSettings() {
        openAction(Settings.ACTION_BLUETOOTH_SETTINGS);
    }

    public void openTetherSettings() {
        /*
         * ACTION_TETHER_SETTINGS is a stable Android Settings action but is a
         * SystemApi constant, so PanOSP uses the documented action string and
         * safely falls back when an OEM does not expose it.
         */
        if (!openAction(ACTION_TETHER_SETTINGS)) {
            openAction(Settings.ACTION_WIRELESS_SETTINGS);
        }
    }

    public void openAirplaneModeSettings() {
        openAction(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
    }

    // =========================================================================
    // DEVICE / DISPLAY
    // =========================================================================

    public void openDisplaySettings() {
        openAction(Settings.ACTION_DISPLAY_SETTINGS);
    }

    public void openNfcSettings() {
        openAction(Settings.ACTION_NFC_SETTINGS);
    }

    public void openReadingModeSettings() {
        openAction(Settings.ACTION_NIGHT_DISPLAY_SETTINGS);
    }

    public void openLocationSettings() {
        openAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    }

    public void openBatterySaverSettings() {
        openAction(Settings.ACTION_BATTERY_SAVER_SETTINGS);
    }

    // =========================================================================
    // DO NOT DISTURB
    // =========================================================================

    public void openDndPolicyAccess() {
        openAction(
                Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
        );
    }

    public void openDndSettings() {
        if (!openAction(ACTION_ZEN_MODE_SETTINGS)) {
            openAction(Settings.ACTION_SOUND_SETTINGS);
        }
    }

    // =========================================================================
    // EXTERNAL ACTIONS
    // =========================================================================

    public void openCamera() {
        closeShadeFirst();

        Intent intent =
                new Intent(
                        MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
                );

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startSafely(intent);
    }

    // =========================================================================
    // PANOSP SPECIAL ACCESS / APP
    // =========================================================================

    public void openWriteSettings() {
        closeShadeFirst();

        Intent intent =
                new Intent(
                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse(
                                "package:"
                                        + context.getPackageName()
                        )
                );

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startSafely(intent);
    }

    public void openPanOSPSetup() {
        closeShadeFirst();

        Intent intent =
                new Intent(
                        context,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startSafely(intent);
    }

    /** Opens native settings for the current user; never targets the wrong profile silently. */
    public boolean openNotificationSettings(String packageName, String channelId, int userId, android.os.UserHandle user) {
        if (userId >= 0 && !android.os.Process.myUserHandle().equals(user)) return false;
        closeShadeFirst();
        if (channelId != null && !channelId.isBlank()) {
            Intent channel = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (startSafely(channel)) return true;
        }
        Intent app = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (startSafely(app)) return true;
        return startSafely(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    // =========================================================================
    // INTERNAL
    // =========================================================================

    private boolean openAction(String action) {
        closeShadeFirst();

        Intent intent =
                new Intent(action);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        return startSafely(intent);
    }

    private boolean startSafely(Intent intent) {
        try {
            context.startActivity(intent);
            return true;

        } catch (Exception ignored) {
            return false;
        }
    }

    private void closeShadeFirst() {
        if (beforeExternalActivity != null) {
            beforeExternalActivity.run();
        }
    }
}
