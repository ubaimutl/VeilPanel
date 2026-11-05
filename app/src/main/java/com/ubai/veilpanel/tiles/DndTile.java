package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.DndController;
import com.ubai.veilpanel.system.PublicSystemControlBridge;
import com.ubai.veilpanel.system.SettingsLauncher;

/**
 * Do Not Disturb quick-setting tile.
 *
 * PanOSP prefers its official NotificationManager path when policy access has
 * already been granted. Otherwise the same short press is handled through the
 * accessibility bridge rather than forcing a manual settings visit.
 */
public final class DndTile extends PanTile {

    private final DndController controller;
    private final PublicSystemControlBridge bridge;
    private final SettingsLauncher settingsLauncher;

    public DndTile(
            Context context,
            DndController controller,
            PublicSystemControlBridge bridge,
            SettingsLauncher settingsLauncher) {

        super(
                context,
                "dnd",
                R.string.tile_dnd
        );

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
        boolean target =
                !controller.isEnabled();

        /*
         * Official API first when already authorized.
         */
        if (controller.hasPolicyAccess()
                && controller.toggle()) {
            return;
        }

        /*
         * Otherwise reproduce a SystemUI-like user tap through the real Android
         * DND switch. We deliberately do not auto-grant policy access.
         */
        if (bridge.canToggleDndDirectly()
                && bridge.setDndEnabled(target)) {
            return;
        }

        settingsLauncher.openDndSettings();
    }

    @Override
    public boolean handleLongClick() {
        settingsLauncher.openDndSettings();
        return true;
    }
}
