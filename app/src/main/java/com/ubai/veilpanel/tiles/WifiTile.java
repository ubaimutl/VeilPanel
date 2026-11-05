package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.PublicSystemControlBridge;
import com.ubai.veilpanel.system.SettingsLauncher;
import com.ubai.veilpanel.system.WifiController;

public final class WifiTile extends PanTile {

    private final WifiController controller;
    private final PublicSystemControlBridge bridge;
    private final SettingsLauncher settingsLauncher;

    public WifiTile(
            Context context,
            WifiController controller,
            PublicSystemControlBridge bridge,
            SettingsLauncher settingsLauncher) {
        super(context, "wifi", R.string.tile_wifi);
        this.controller = controller;
        this.bridge = bridge;
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
        boolean target = !controller.isEnabled();

        if (bridge.canToggleWifiDirectly()
                && bridge.setWifiEnabled(target)) {
            return;
        }

        settingsLauncher.openWifiPanel();
    }

    @Override
    public boolean handleLongClick() {
        settingsLauncher.openWifiPanel();
        return true;
    }
}
