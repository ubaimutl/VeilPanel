package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.AirplaneModeController;
import com.ubai.veilpanel.system.PublicSystemControlBridge;
import com.ubai.veilpanel.system.SettingsLauncher;

/**
 * Airplane-mode quick-setting tile.
 *
 * PanOSP deliberately avoids shell helpers in the standard build. A short
 * press therefore opens Android's Airplane-mode settings immediately, with no
 * hidden OEM shade, timeout or Shizuku dependency.
 */
public final class AirplaneModeTile extends PanTile {

    private final AirplaneModeController controller;
    private final SettingsLauncher settingsLauncher;

    public AirplaneModeTile(
            Context context,
            AirplaneModeController controller,
            PublicSystemControlBridge bridge,
            SettingsLauncher settingsLauncher) {

        super(
                context,
                "airplane",
                R.string.tile_airplane
        );

        this.controller = controller;
        this.settingsLauncher = settingsLauncher;
    }

    @Override
    public TileStatus getStatus() {
        return controller.isEnabled()
                ? TileStatus.ACTIVE
                : TileStatus.INACTIVE;
    }

    @Override
    public void handleClick() {
        settingsLauncher.openAirplaneModeSettings();
    }

    @Override
    public boolean handleLongClick() {
        settingsLauncher.openAirplaneModeSettings();
        return true;
    }
}
