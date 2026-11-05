package com.ubai.veilpanel.system;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Bluetooth state reader.
 *
 * Android 13+ blocks BluetoothAdapter.enable()/disable() for ordinary apps
 * targeting current SDK levels, so direct writes belong in the privileged
 * bridge.
 */
public final class BluetoothController {

    private final Context context;
    private final BluetoothAdapter adapter;

    public BluetoothController(Context context) {
        this.context = context.getApplicationContext();

        BluetoothManager manager =
                (BluetoothManager) this.context.getSystemService(
                        Context.BLUETOOTH_SERVICE
                );

        adapter = manager != null ? manager.getAdapter() : null;
    }

    public boolean hasPermission() {
        return context.checkSelfPermission(
                Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isAvailable() {
        return adapter != null;
    }

    public boolean isEnabled() {
        if (adapter == null || !hasPermission()) {
            return false;
        }

        try {
            return adapter.isEnabled();
        } catch (SecurityException ignored) {
            return false;
        }
    }
}
