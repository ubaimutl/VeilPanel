package com.ubai.veilpanel.system;

import android.content.Context;
import android.net.wifi.WifiManager;

/**
 * Wi-Fi state reader.
 *
 * Direct Wi-Fi enable/disable is intentionally delegated to the privileged
 * bridge because Android 10+ blocks WifiManager.setWifiEnabled() for ordinary
 * target-SDK applications.
 */
public final class WifiController {

    private final WifiManager wifiManager;

    public WifiController(Context context) {
        wifiManager =
                (WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
    }

    public boolean isAvailable() {
        return wifiManager != null;
    }

    public boolean isEnabled() {
        return wifiManager != null && wifiManager.isWifiEnabled();
    }
}
