package com.ubai.veilpanel.tiles;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ubai.veilpanel.config.PanOSPPreferences;
import com.ubai.veilpanel.system.AirplaneModeController;
import com.ubai.veilpanel.system.BatterySaverController;
import com.ubai.veilpanel.system.BluetoothController;
import com.ubai.veilpanel.system.DndController;
import com.ubai.veilpanel.system.FlashlightController;
import com.ubai.veilpanel.system.HotspotController;
import com.ubai.veilpanel.system.LocationController;
import com.ubai.veilpanel.system.MobileDataController;
import com.ubai.veilpanel.system.PublicSystemControlBridge;
import com.ubai.veilpanel.system.RotationController;
import com.ubai.veilpanel.system.SettingsLauncher;
import com.ubai.veilpanel.system.WifiController;

/**
 * Creates the current ordered set of PanOSP tiles.
 */
public final class TileHost {

    private final List<PanTile> tiles =
            new ArrayList<>();

    private final Context context;
    private final PanOSPPreferences preferences;

    private final SettingsLauncher settingsLauncher;
    private final PublicSystemControlBridge bridge;

    private final WifiController wifiController;
    private final MobileDataController mobileDataController;
    private final BluetoothController bluetoothController;
    private final FlashlightController flashlightController;
    private final RotationController rotationController;

    private final LocationController locationController;
    private final HotspotController hotspotController;
    private final AirplaneModeController airplaneModeController;
    private final DndController dndController;
    private final BatterySaverController batterySaverController;

    private Runnable stateChangeListener;

    public TileHost(
            Context context,
            Runnable closeShade) {

        this.context = context;

        preferences =
                new PanOSPPreferences(context);

        settingsLauncher =
                new SettingsLauncher(
                        context,
                        closeShade
                );

        bridge =
                new PublicSystemControlBridge(context);

        wifiController =
                new WifiController(context);

        mobileDataController =
                new MobileDataController(context);

        bluetoothController =
                new BluetoothController(context);

        flashlightController =
                new FlashlightController(context);

        rotationController =
                new RotationController(context);

        locationController =
                new LocationController(context);

        hotspotController =
                new HotspotController(
                        context,
                        this::notifyStateChanged
                );

        airplaneModeController =
                new AirplaneModeController(context);

        dndController =
                new DndController(context);

        batterySaverController =
                new BatterySaverController(context);

        reloadTiles();
    }

    /**
     * Rebuilds only the tile objects/order. Controllers remain shared so a
     * layout change never changes the system-control backend.
     */
    public void reloadTiles() {
        tiles.clear();

        Set<String> addedSpecs =
                new HashSet<>();

        for (String spec : preferences.getTileSpecs()) {
            addTileIfKnown(spec, addedSpecs);
        }
    }

    public List<PanTile> getTiles() {
        return Collections.unmodifiableList(tiles);
    }

    public void setStateChangeListener(
            Runnable listener) {

        stateChangeListener = listener;
        mobileDataController.setStateChangeListener(
                listener == null ? null : this::notifyStateChanged);
    }

    public void close() {
        stateChangeListener = null;

        mobileDataController.close();
        flashlightController.close();
        hotspotController.close();
    }

    private void addTileIfKnown(
            String spec,
            Set<String> addedSpecs) {

        if (spec == null
                || addedSpecs.contains(spec)) {
            return;
        }

        PanTile tile =
                createTile(spec);

        if (tile != null) {
            tiles.add(tile);
            addedSpecs.add(spec);
        }
    }

    private PanTile createTile(String spec) {
        switch (spec) {

            case "wifi":
                return new WifiTile(
                        context,
                        wifiController,
                        bridge,
                        settingsLauncher
                );

            case "data":
                return new MobileDataTile(
                        context,
                        mobileDataController,
                        bridge,
                        settingsLauncher
                );

            case "bluetooth":
                return new BluetoothTile(
                        context,
                        bluetoothController,
                        bridge,
                        settingsLauncher
                );

            case "flashlight":
                return new FlashlightTile(
                        context,
                        flashlightController,
                        settingsLauncher
                );

            case "rotation":
                return new RotationTile(
                        context,
                        rotationController,
                        settingsLauncher
                );

            case "location":
                return new LocationTile(
                        context,
                        locationController,
                        bridge,
                        settingsLauncher
                );

            case "hotspot":
                return new HotspotTile(
                        context,
                        hotspotController,
                        bridge,
                        settingsLauncher
                );

            case "airplane":
                return new AirplaneModeTile(
                        context,
                        airplaneModeController,
                        bridge,
                        settingsLauncher
                );

            case "dnd":
                return new DndTile(
                        context,
                        dndController,
                        bridge,
                        settingsLauncher
                );

            case "battery":
                return new BatterySaverTile(
                        context,
                        batterySaverController,
                        bridge,
                        settingsLauncher
                );

            // ================================================================
            // NEW TILES - TEMPORARY TEST GROUP
            // ================================================================

            case "nfc":
                return new NfcTile(
                        context,
                        settingsLauncher
                );

            case "dark":
                return new DarkModeTile(
                        context,
                        settingsLauncher
                );

            case "silent":
                return new SilentTile(context);

            case "vibrate":
                return new VibrateTile(context);

            case "reading":
                return new ReadingModeTile(
                        context,
                        settingsLauncher
                );

            case "screenshot":
                return new ScreenshotTile(context);

            case "camera":
                return new CameraTile(
                        context,
                        settingsLauncher
                );

            default:
                return null;
        }
    }

    private void notifyStateChanged() {
        Runnable listener =
                stateChangeListener;

        if (listener != null) {
            listener.run();
        }
    }
}
