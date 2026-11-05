package com.ubai.veilpanel.system;

import android.content.Context;
import android.os.Build;

import java.util.Set;

/**
 * Reads Wi-Fi tethering state using the public Android 16 TetheringManager API.
 *
 * Android 16 exposes TetheringManager publicly. The event callback can report
 * currently tethered interfaces with ACCESS_NETWORK_STATE.
 *
 * Direct start/stop remains privileged for the universal PanOSP use case, so
 * the tile opens Android's tethering settings for the actual user action.
 *
 * Android 15 fallback:
 * the public API does not expose an equivalent reliable synchronous hotspot
 * state. PanOSP keeps the tile usable as a settings shortcut but reports the
 * state as unknown rather than inventing an ON/OFF value.
 */
public final class HotspotController implements AutoCloseable {

    private final Runnable onStateChanged;

    private volatile boolean stateKnown;
    private volatile boolean enabled;

    private AutoCloseable apiBackend;
    private AccessibilityControlGateway.HotspotStateListener
            fallbackStateListener;

    public HotspotController(
            Context context,
            Runnable onStateChanged) {

        this.onStateChanged = onStateChanged;

        if (Build.VERSION.SDK_INT >= 36) {
            try {
                apiBackend =
                        Api36Backend.register(
                                context.getApplicationContext(),
                                this::updateState
                        );

            } catch (RuntimeException ignored) {
                apiBackend = null;
            }
        } else {
            fallbackStateListener = this::updateState;
            AccessibilityControlGateway
                    .registerHotspotStateListener(
                            fallbackStateListener
                    );
        }
    }

    public boolean isStateKnown() {
        return stateKnown;
    }

    public boolean isEnabled() {
        return stateKnown && enabled;
    }

    private void updateState(boolean newEnabled) {
        boolean changed =
                !stateKnown || enabled != newEnabled;

        stateKnown = true;
        enabled = newEnabled;

        if (changed && onStateChanged != null) {
            onStateChanged.run();
        }
    }

    @Override
    public void close() {
        if (fallbackStateListener != null) {
            AccessibilityControlGateway
                    .unregisterHotspotStateListener(
                            fallbackStateListener
                    );
            fallbackStateListener = null;
        }

        if (apiBackend == null) {
            return;
        }

        try {
            apiBackend.close();
        } catch (Exception ignored) {
        }

        apiBackend = null;
    }

    // =========================================================================
    // ANDROID 16 IMPLEMENTATION
    // =========================================================================

    /**
     * Isolates API 36 classes so Android 15 never loads/verifies them.
     */
    @android.annotation.SuppressLint("UseRequiresApi")
    @android.annotation.TargetApi(36)
    private static final class Api36Backend
            implements AutoCloseable {

        private final Context context;
        private final android.net.TetheringManager manager;
        private final android.net.TetheringManager.TetheringEventCallback
                callback;

        private Api36Backend(
                Context context,
                android.net.TetheringManager manager,
                android.net.TetheringManager.TetheringEventCallback
                        callback) {

            this.context = context;
            this.manager = manager;
            this.callback = callback;
        }

        // Optional upstream backend: not instantiated by the notification-only panel.
        // Both entry and cleanup paths explicitly reject missing runtime permission.
        @android.annotation.SuppressLint("MissingPermission")
        static Api36Backend register(
                Context context,
                StateSink sink) {

            if (context.checkSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) return null;
            android.net.TetheringManager manager =
                    context.getSystemService(
                            android.net.TetheringManager.class
                    );

            if (manager == null) {
                return null;
            }

            android.net.TetheringManager.TetheringEventCallback callback =
                    new android.net.TetheringManager
                            .TetheringEventCallback() {

                        @Override
                        public void onTetheredInterfacesChanged(
                                Set<android.net.TetheringInterface>
                                        interfaces) {

                            boolean wifiEnabled = false;

                            if (interfaces != null) {
                                for (android.net.TetheringInterface item
                                        : interfaces) {

                                    if (item != null
                                            && item.getType()
                                            == android.net
                                            .TetheringManager
                                            .TETHERING_WIFI) {

                                        wifiEnabled = true;
                                        break;
                                    }
                                }
                            }

                            sink.onState(wifiEnabled);
                        }
                    };

            manager.registerTetheringEventCallback(
                    context.getMainExecutor(),
                    callback
            );

            return new Api36Backend(
                    context,
                    manager,
                    callback
            );
        }

        @Override
        @android.annotation.SuppressLint("MissingPermission")
        public void close() {
            if (context.checkSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
            manager.unregisterTetheringEventCallback(
                    callback
            );
        }
    }

    private interface StateSink {
        void onState(boolean enabled);
    }
}
