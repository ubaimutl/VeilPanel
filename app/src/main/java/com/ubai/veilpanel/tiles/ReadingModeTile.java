package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.SettingsLauncher;

public final class ReadingModeTile extends PanTile {

    private final SettingsLauncher settingsLauncher;

    public ReadingModeTile(
            Context context,
            SettingsLauncher settingsLauncher) {

        super(context, "reading", R.string.tile_reading_mode);
        this.settingsLauncher = settingsLauncher;
    }

    @Override
    public TileStatus getStatus() {
        return TileStatus.INACTIVE;
    }

    @Override
    public void handleClick() {
        settingsLauncher.openReadingModeSettings();
    }
}
