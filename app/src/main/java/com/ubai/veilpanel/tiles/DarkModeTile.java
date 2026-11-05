package com.ubai.veilpanel.tiles;

import android.content.Context;
import android.content.res.Configuration;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.SettingsLauncher;

public final class DarkModeTile extends PanTile {

    private final SettingsLauncher settingsLauncher;

    public DarkModeTile(
            Context context,
            SettingsLauncher settingsLauncher) {

        super(context, "dark", R.string.tile_dark_mode);
        this.settingsLauncher = settingsLauncher;
    }

    private boolean isDark() {
        int mode =
                context.getResources()
                        .getConfiguration()
                        .uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;

        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public TileStatus getStatus() {
        return isDark()
                ? TileStatus.ACTIVE
                : TileStatus.INACTIVE;
    }

    @Override
    public void handleClick() {
        settingsLauncher.openDisplaySettings();
    }
}
