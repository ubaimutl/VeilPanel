package com.ubai.veilpanel.tiles;

import android.content.Context;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.AccessibilityControlGateway;

/**
 * Screenshot action tile.
 *
 * Uses PanOSP's registered AccessibilityService backend.
 */
public final class ScreenshotTile extends PanTile {

    public ScreenshotTile(Context context) {
        super(
                context,
                "screenshot",
                R.string.tile_screenshot
        );
    }

    @Override
    public TileStatus getStatus() {
        return AccessibilityControlGateway.isAvailable()
                ? TileStatus.INACTIVE
                : TileStatus.UNAVAILABLE;
    }

    @Override
    public void handleClick() {
        AccessibilityControlGateway.requestScreenshot();
    }
}
