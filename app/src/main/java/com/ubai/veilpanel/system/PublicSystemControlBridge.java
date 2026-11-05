package com.ubai.veilpanel.system;

import android.content.Context;

/**
 * Boundary for quick controls that need something other than the dedicated
 * controller APIs.
 *
 * PanOSP deliberately has no Shizuku dependency in the standard build.
 * Controls validated through Accessibility remain available here. Controls
 * that require shell/system identity (mobile data and Airplane mode) report
 * themselves as unavailable so their tiles can open Android Settings
 * immediately instead.
 */
public final class PublicSystemControlBridge {

    public PublicSystemControlBridge(Context context) {
        // Context kept in the constructor to preserve the existing TileHost API.
    }

    // =========================================================================
    // WI-FI
    // =========================================================================

    public boolean canToggleWifiDirectly() {
        return AccessibilityControlGateway.isAvailable();
    }

    public boolean setWifiEnabled(boolean enabled) {
        return AccessibilityControlGateway
                .requestWifiEnabled(enabled);
    }

    // =========================================================================
    // BLUETOOTH
    // =========================================================================

    public boolean canToggleBluetoothDirectly() {
        return AccessibilityControlGateway.isAvailable();
    }

    public boolean setBluetoothEnabled(boolean enabled) {
        return AccessibilityControlGateway
                .requestBluetoothEnabled(enabled);
    }

    // =========================================================================
    // MOBILE DATA
    // =========================================================================

    /**
     * A normal third-party Android application cannot toggle the user's mobile
     * data setting directly without privileged/carrier identity.
     */
    public boolean canToggleMobileDataDirectly() {
        return false;
    }

    public boolean setMobileDataEnabled(boolean enabled) {
        return false;
    }

    // =========================================================================
    // LOCATION
    // =========================================================================

    public boolean canToggleLocationDirectly() {
        return AccessibilityControlGateway.isAvailable();
    }

    public boolean setLocationEnabled(boolean enabled) {
        return AccessibilityControlGateway
                .requestLocationEnabled(enabled);
    }

    // =========================================================================
    // HOTSPOT
    // =========================================================================

    public boolean canToggleHotspotDirectly() {
        return AccessibilityControlGateway.isAvailable();
    }

    public boolean setHotspotEnabled(boolean enabled) {
        return AccessibilityControlGateway
                .requestHotspotEnabled(enabled);
    }

    public boolean toggleHotspotUnknownState() {
        return AccessibilityControlGateway
                .requestHotspotToggle();
    }

    // =========================================================================
    // AIRPLANE MODE
    // =========================================================================

    /**
     * Airplane mode is system-controlled on current Android releases. PanOSP
     * therefore uses the Settings fallback instead of a shell helper.
     */
    public boolean canToggleAirplaneModeDirectly() {
        return false;
    }

    public boolean setAirplaneModeEnabled(boolean enabled) {
        return false;
    }

    // =========================================================================
    // DO NOT DISTURB
    // =========================================================================

    public boolean canToggleDndDirectly() {
        return AccessibilityControlGateway.isAvailable();
    }

    public boolean setDndEnabled(boolean enabled) {
        return AccessibilityControlGateway
                .requestDndEnabled(enabled);
    }

    // =========================================================================
    // BATTERY SAVER
    // =========================================================================

    public boolean canToggleBatterySaverDirectly() {
        return AccessibilityControlGateway.isAvailable();
    }

    public boolean setBatterySaverEnabled(boolean enabled) {
        return AccessibilityControlGateway
                .requestBatterySaverEnabled(enabled);
    }
}
