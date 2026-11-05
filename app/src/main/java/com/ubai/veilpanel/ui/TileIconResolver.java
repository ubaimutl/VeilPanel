package com.ubai.veilpanel.ui;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.tiles.PanTile;

/**
 * Maps functional PanTile classes to neutral Android-style vector icons.
 *
 * Rendering remains independent from tile behavior so icon/style changes do
 * not touch system controllers.
 */
public final class TileIconResolver {

    private TileIconResolver() {
    }

    public static int resolve(PanTile tile) {
        if (tile == null) {
            return R.drawable.ic_qs_unknown;
        }

        switch (tile.getClass().getSimpleName()) {

            case "WifiTile":
                return R.drawable.ic_qs_wifi;

            case "MobileDataTile":
                return R.drawable.ic_qs_mobile_data;

            case "BluetoothTile":
                return R.drawable.ic_qs_bluetooth;

            case "FlashlightTile":
                return R.drawable.ic_qs_flashlight;

            case "RotationTile":
                return R.drawable.ic_qs_rotation;

            case "LocationTile":
                return R.drawable.ic_qs_location;

            case "HotspotTile":
                return R.drawable.ic_qs_hotspot;

            case "AirplaneModeTile":
                return R.drawable.ic_qs_airplane;

            case "DndTile":
                return R.drawable.ic_qs_dnd;

            case "BatterySaverTile":
                return R.drawable.ic_qs_battery_saver;

            // ================================================================
            // NEW TILES - TEMPORARY TEST GROUP
            // ================================================================

            case "NfcTile":
                return R.drawable.ic_qs_nfc;

            case "DarkModeTile":
                return R.drawable.ic_qs_dark;

            case "SilentTile":
                return R.drawable.ic_qs_silent;

            case "VibrateTile":
                return R.drawable.ic_qs_vibrate;

            case "ReadingModeTile":
                return R.drawable.ic_qs_reading;

            case "ScreenshotTile":
                return R.drawable.ic_qs_screenshot;

            case "CameraTile":
                return R.drawable.ic_qs_camera;

            default:
                return resolveSpec(tile.getSpec());
        }
    }

    private static int resolveSpec(String spec) {
        if (spec == null) {
            return R.drawable.ic_qs_unknown;
        }

        switch (spec) {
            case "wifi":
                return R.drawable.ic_qs_wifi;
            case "data":
                return R.drawable.ic_qs_mobile_data;
            case "bluetooth":
                return R.drawable.ic_qs_bluetooth;
            case "flashlight":
                return R.drawable.ic_qs_flashlight;
            case "rotation":
                return R.drawable.ic_qs_rotation;
            case "location":
                return R.drawable.ic_qs_location;
            case "hotspot":
                return R.drawable.ic_qs_hotspot;
            case "airplane":
                return R.drawable.ic_qs_airplane;
            case "dnd":
                return R.drawable.ic_qs_dnd;
            case "battery":
                return R.drawable.ic_qs_battery_saver;
            case "nfc":
                return R.drawable.ic_qs_nfc;
            case "dark":
                return R.drawable.ic_qs_dark;
            case "silent":
                return R.drawable.ic_qs_silent;
            case "vibrate":
                return R.drawable.ic_qs_vibrate;
            case "reading":
                return R.drawable.ic_qs_reading;
            case "screenshot":
                return R.drawable.ic_qs_screenshot;
            case "camera":
                return R.drawable.ic_qs_camera;
            default:
                return R.drawable.ic_qs_unknown;
        }
    }
}
