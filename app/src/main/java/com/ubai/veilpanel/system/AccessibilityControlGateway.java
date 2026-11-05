package com.ubai.veilpanel.system;

/**
 * Process-local bridge between quick-setting tiles and PanOSP's
 * AccessibilityService automation backend.
 *
 * Tiles do not depend directly on AccessibilityService. The service registers
 * a backend while it is alive; callers only request a user-initiated control
 * operation.
 */
public final class AccessibilityControlGateway {

    public interface HotspotStateListener {
        void onHotspotStateObserved(boolean enabled);
    }

    public interface Backend {

        boolean requestWifiEnabled(boolean enabled);

        boolean requestBluetoothEnabled(boolean enabled);

        boolean requestMobileDataEnabled(boolean enabled);

        boolean requestLocationEnabled(boolean enabled);

        boolean requestHotspotEnabled(boolean enabled);

        boolean requestHotspotToggle();

        boolean requestAirplaneModeEnabled(boolean enabled);

        boolean requestDndEnabled(boolean enabled);

        boolean requestBatterySaverEnabled(boolean enabled);

        // NEW TILES - TEMPORARY TEST GROUP
        boolean requestNfcEnabled(boolean enabled);

        boolean requestDarkModeEnabled(boolean enabled);

        boolean requestReadingModeToggle();

        // NEW TILE - Screenshot action
        boolean requestScreenshot();
    }

    private static volatile Backend backend;
    private static volatile Runnable bluetoothSuccessListener;
    private static volatile HotspotStateListener hotspotStateListener;

    private AccessibilityControlGateway() {
    }

    public static void register(Backend newBackend) {
        backend = newBackend;
    }

    public static void unregister(Backend oldBackend) {
        if (backend == oldBackend) {
            backend = null;
        }
    }

    public static boolean isAvailable() {
        return backend != null;
    }

    public static void registerBluetoothSuccessListener(Runnable listener) {
        bluetoothSuccessListener = listener;
    }

    public static void unregisterBluetoothSuccessListener(Runnable listener) {
        if (bluetoothSuccessListener == listener) {
            bluetoothSuccessListener = null;
        }
    }

    public static void reportBluetoothOperationSuccess() {
        Runnable listener = bluetoothSuccessListener;
        if (listener != null) {
            listener.run();
        }
    }

    public static void registerHotspotStateListener(
            HotspotStateListener listener) {
        hotspotStateListener = listener;
    }

    public static void unregisterHotspotStateListener(
            HotspotStateListener listener) {
        if (hotspotStateListener == listener) {
            hotspotStateListener = null;
        }
    }

    public static void reportHotspotState(boolean enabled) {
        HotspotStateListener listener = hotspotStateListener;
        if (listener != null) {
            listener.onHotspotStateObserved(enabled);
        }
    }

    public static boolean requestWifiEnabled(boolean enabled) {
        Backend current = backend;
        return current != null
                && current.requestWifiEnabled(enabled);
    }

    public static boolean requestBluetoothEnabled(boolean enabled) {
        Backend current = backend;
        return current != null
                && current.requestBluetoothEnabled(enabled);
    }

    public static boolean requestMobileDataEnabled(boolean enabled) {
        Backend current = backend;
        return current != null
                && current.requestMobileDataEnabled(enabled);
    }

    public static boolean requestLocationEnabled(boolean enabled) {
        Backend current = backend;
        return current != null
                && current.requestLocationEnabled(enabled);
    }

    public static boolean requestHotspotEnabled(boolean enabled) {
        Backend current = backend;
        return current != null
                && current.requestHotspotEnabled(enabled);
    }

    public static boolean requestHotspotToggle() {
        Backend current = backend;
        return current != null
                && current.requestHotspotToggle();
    }

    public static boolean requestAirplaneModeEnabled(boolean enabled) {
        Backend current = backend;
        return current != null
                && current.requestAirplaneModeEnabled(enabled);
    }

    public static boolean requestDndEnabled(boolean enabled) {
        Backend current = backend;
        return current != null
                && current.requestDndEnabled(enabled);
    }

    public static boolean requestBatterySaverEnabled(boolean enabled) {
        Backend current = backend;
        return current != null
                && current.requestBatterySaverEnabled(enabled);
    }

    // =====================================================================
    // NEW TILES - TEMPORARY TEST GROUP
    // =====================================================================

    public static boolean requestNfcEnabled(boolean enabled) {
        Backend current = backend;
        return current != null
                && current.requestNfcEnabled(enabled);
    }

    public static boolean requestDarkModeEnabled(boolean enabled) {
        Backend current = backend;
        return current != null
                && current.requestDarkModeEnabled(enabled);
    }

    public static boolean requestReadingModeToggle() {
        Backend current = backend;
        return current != null
                && current.requestReadingModeToggle();
    }

    // =====================================================================
    // SCREENSHOT
    // =====================================================================

    public static boolean requestScreenshot() {
        Backend current = backend;
        return current != null
                && current.requestScreenshot();
    }
}
