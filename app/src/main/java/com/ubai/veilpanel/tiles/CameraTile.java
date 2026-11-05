package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.SettingsLauncher;

/**
 * Camera action tile.
 */
public final class CameraTile extends PanTile {

    private final SettingsLauncher settingsLauncher;

    public CameraTile(
            Context context,
            SettingsLauncher settingsLauncher) {

        super(
                context,
                "camera",
                R.string.tile_camera
        );

        this.settingsLauncher = settingsLauncher;
    }

    @Override
    public TileStatus getStatus() {
        return TileStatus.INACTIVE;
    }

    @Override
    public void handleClick() {
        settingsLauncher.openCamera();
    }
}
