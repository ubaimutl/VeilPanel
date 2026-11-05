package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.LocationController;
import com.ubai.veilpanel.system.PublicSystemControlBridge;
import com.ubai.veilpanel.system.SettingsLauncher;

/**
 * Location quick-setting tile.
 *
 * Short press:
 * - direct toggle when WRITE_SECURE_SETTINGS is really granted;
 * - otherwise open Android Location settings immediately.
 *
 * There is deliberately no accessibility timeout before the fallback.
 */
public final class LocationTile extends PanTile {

    private final LocationController controller;
    private final SettingsLauncher settingsLauncher;

    public LocationTile(
            Context context,
            LocationController controller,
            PublicSystemControlBridge bridge,
            SettingsLauncher settingsLauncher) {

        super(
                context,
                "location",
                R.string.tile_location
        );

        this.controller = controller;
        this.settingsLauncher = settingsLauncher;
    }

    @Override
    public TileStatus getStatus() {
        if (!controller.isAvailable()) {
            return TileStatus.UNAVAILABLE;
        }

        return controller.isEnabled()
                ? TileStatus.ACTIVE
                : TileStatus.INACTIVE;
    }

    @Override
    public void handleClick() {
        boolean target =
                !controller.isEnabled();

        if (controller.canSetEnabledDirectly()
                && controller.setEnabled(target)) {
            return;
        }

        /* No 4-second automation timeout: fallback is immediate. */
        settingsLauncher.openLocationSettings();
    }

    @Override
    public boolean handleLongClick() {
        settingsLauncher.openLocationSettings();
        return true;
    }
}
