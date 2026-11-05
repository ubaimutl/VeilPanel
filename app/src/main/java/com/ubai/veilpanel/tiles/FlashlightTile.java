package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.FlashlightController;
import com.ubai.veilpanel.system.SettingsLauncher;

public final class FlashlightTile extends PanTile {

    private final FlashlightController controller;
    private final SettingsLauncher settingsLauncher;

    public FlashlightTile(
            Context context,
            FlashlightController controller,
            SettingsLauncher settingsLauncher) {
        super(context, "flashlight", R.string.tile_flashlight);
        this.controller = controller;
        this.settingsLauncher = settingsLauncher;
    }

    @Override
    public TileStatus getStatus() {
        if (!controller.hasPermission()
                || !controller.isAvailable()) {
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

        controller.toggle();
    }
}
