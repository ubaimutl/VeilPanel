package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.RotationController;
import com.ubai.veilpanel.system.SettingsLauncher;

public final class RotationTile extends PanTile {

    private final RotationController controller;
    private final SettingsLauncher settingsLauncher;

    public RotationTile(
            Context context,
            RotationController controller,
            SettingsLauncher settingsLauncher) {
        super(context, "rotation", R.string.tile_rotation);
        this.controller = controller;
        this.settingsLauncher = settingsLauncher;
    }

    @Override
    public TileStatus getStatus() {
        if (!controller.canWrite()) {
            return TileStatus.UNAVAILABLE;
        }

        return controller.isAutoRotationEnabled()
                ? TileStatus.ACTIVE
                : TileStatus.INACTIVE;
    }

    @Override
    public void handleClick() {
        if (!controller.canWrite()) {
            settingsLauncher.openWriteSettings();
            return;
        }

        controller.toggle();
    }

    @Override
    public boolean handleLongClick() {
        settingsLauncher.openDisplaySettings();
        return true;
    }
}
