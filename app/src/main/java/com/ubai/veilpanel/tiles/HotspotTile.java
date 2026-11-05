package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.HotspotController;
import com.ubai.veilpanel.system.PublicSystemControlBridge;
import com.ubai.veilpanel.system.SettingsLauncher;

/**
 * Wi-Fi hotspot / tethering quick-setting tile.
 *
 * Android 16 exposes a real public state through HotspotController.
 * Android 15 can still perform a user-triggered accessibility toggle by
 * deriving the target from the real Settings switch.
 */
public final class HotspotTile extends PanTile {

    private final HotspotController controller;
    private final PublicSystemControlBridge bridge;
    private final SettingsLauncher settingsLauncher;

    public HotspotTile(
            Context context,
            HotspotController controller,
            PublicSystemControlBridge bridge,
            SettingsLauncher settingsLauncher) {

        super(
                context,
                "hotspot",
                R.string.tile_hotspot
        );

        this.controller = controller;
        this.bridge = bridge;
        this.settingsLauncher = settingsLauncher;
    }

    @Override
    public TileStatus getStatus() {
        if (!controller.isStateKnown()) {
            return TileStatus.UNAVAILABLE;
        }

        return controller.isEnabled()
                ? TileStatus.ACTIVE
                : TileStatus.INACTIVE;
    }

    @Override
    public String getStateLabel() {
        if (!controller.isStateKnown()) {
            return context.getString(
                    R.string.tile_state_settings
            );
        }

        return super.getStateLabel();
    }

    @Override
    public void handleClick() {
        if (!bridge.canToggleHotspotDirectly()) {
            settingsLauncher.openTetherSettings();
            return;
        }

        if (controller.isStateKnown()) {
            if (bridge.setHotspotEnabled(
                    !controller.isEnabled())) {

                return;
            }

        } else if (bridge.toggleHotspotUnknownState()) {
            return;
        }

        settingsLauncher.openTetherSettings();
    }

    @Override
    public boolean handleLongClick() {
        settingsLauncher.openTetherSettings();
        return true;
    }
}
