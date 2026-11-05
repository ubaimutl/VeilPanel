package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.MobileDataController;
import com.ubai.veilpanel.system.PublicSystemControlBridge;
import com.ubai.veilpanel.system.SettingsLauncher;

/**
 * Mobile-data quick-setting tile.
 *
 * Android does not expose a normal third-party setter for the user's mobile
 * data state. PanOSP therefore opens the Internet settings surface
 * immediately, with no hidden Quick Settings attempt, timeout or Shizuku
 * dependency.
 */
public final class MobileDataTile extends PanTile {

    private final MobileDataController controller;
    private final SettingsLauncher settingsLauncher;

    public MobileDataTile(
            Context context,
            MobileDataController controller,
            PublicSystemControlBridge bridge,
            SettingsLauncher settingsLauncher) {

        super(
                context,
                "data",
                R.string.tile_mobile_data
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
        settingsLauncher.openInternetPanel();
    }

    @Override
    public boolean handleLongClick() {
        settingsLauncher.openInternetPanel();
        return true;
    }
}
