package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.BluetoothController;
import com.ubai.veilpanel.system.PublicSystemControlBridge;
import com.ubai.veilpanel.system.SettingsLauncher;

public final class BluetoothTile extends PanTile {

    private final BluetoothController controller;
    private final PublicSystemControlBridge bridge;
    private final SettingsLauncher settingsLauncher;

    public BluetoothTile(
            Context context,
            BluetoothController controller,
            PublicSystemControlBridge bridge,
            SettingsLauncher settingsLauncher) {

        super(
                context,
                "bluetooth",
                R.string.tile_bluetooth
        );

        this.controller = controller;
        this.bridge = bridge;
        this.settingsLauncher = settingsLauncher;
    }

    @Override
    public TileStatus getStatus() {
        if (!controller.isAvailable()
                || !controller.hasPermission()) {

            return TileStatus.UNAVAILABLE;
        }

        return controller.isEnabled()
                ? TileStatus.ACTIVE
                : TileStatus.INACTIVE;
    }

    @Override
    public void handleClick() {
        if (!controller.hasPermission()) {
            settingsLauncher.openPanOSPSetup();
            return;
        }

        boolean target =
                !controller.isEnabled();

        if (bridge.canToggleBluetoothDirectly()
                && bridge.setBluetoothEnabled(target)) {
            return;
        }

        settingsLauncher.openBluetoothSettings();
    }

    @Override
    public boolean handleLongClick() {
        settingsLauncher.openBluetoothSettings();
        return true;
    }
}
