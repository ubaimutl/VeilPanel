package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.BatterySaverController;
import com.ubai.veilpanel.system.PublicSystemControlBridge;
import com.ubai.veilpanel.system.SettingsLauncher;

/**
 * Battery Saver quick-setting tile.
 *
 * Short press uses the privileged direct path when available. If Android or
 * the OEM rejects it, PanOSP opens Battery Saver settings immediately rather
 * than waiting for an accessibility automation timeout.
 */
public final class BatterySaverTile extends PanTile {

    private final BatterySaverController controller;
    private final SettingsLauncher settingsLauncher;

    public BatterySaverTile(
            Context context,
            BatterySaverController controller,
            PublicSystemControlBridge bridge,
            SettingsLauncher settingsLauncher) {

        super(
                context,
                "battery",
                R.string.tile_battery_saver
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

        /* No delayed fallback. */
        settingsLauncher.openBatterySaverSettings();
    }

    @Override
    public boolean handleLongClick() {
        settingsLauncher.openBatterySaverSettings();
        return true;
    }
}
