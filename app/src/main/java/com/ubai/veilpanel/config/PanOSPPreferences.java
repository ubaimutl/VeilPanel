package com.ubai.veilpanel.config;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistent PanOSP panel configuration.
 *
 * The configuration intentionally remains a small set of simple values:
 * ordered tile specs, grid dimensions and tile appearance. Keeping these
 * values behind one class prevents UI code from depending directly on the
 * persistence format.
 */
public final class PanOSPPreferences {

    private static final String PREFS_NAME = "panosp_preferences";

    private static final String KEY_CONFIG_VERSION = "panel_config_version";
    private static final String KEY_TILE_ORDER = "tile_order";
    private static final String KEY_TILE_COLUMNS = "tile_columns";
    private static final String KEY_TILE_ROWS = "tile_rows";
    private static final String KEY_TILE_SIZE_DP = "tile_size_dp";
    private static final String KEY_TILE_CORNER_RADIUS_DP =
            "tile_corner_radius_dp";
    private static final String KEY_PANEL_OPACITY_PERCENT =
            "panel_opacity_percent";
    private static final String KEY_PANEL_CORNER_RADIUS_DP =
            "panel_corner_radius_dp";
    private static final String KEY_NOTIFICATION_TEXT_SIZE_SP =
            "notification_text_size_sp";
    private static final String KEY_NOTIFICATION_BACKGROUND_OPACITY_PERCENT =
            "notification_background_opacity_percent";
    private static final String KEY_PANEL_BACKGROUND_IMAGE_URI =
            "panel_background_image_uri";
    private static final String KEY_PANEL_BACKGROUND_IMAGE_UV_TOP_LEFT_X =
            "panel_background_image_uv_top_left_x";
    private static final String KEY_PANEL_BACKGROUND_IMAGE_UV_TOP_LEFT_Y =
            "panel_background_image_uv_top_left_y";
    private static final String KEY_PANEL_BACKGROUND_IMAGE_UV_TOP_RIGHT_X =
            "panel_background_image_uv_top_right_x";
    private static final String KEY_PANEL_BACKGROUND_IMAGE_UV_TOP_RIGHT_Y =
            "panel_background_image_uv_top_right_y";
    private static final String KEY_PANEL_BACKGROUND_IMAGE_UV_BOTTOM_LEFT_X =
            "panel_background_image_uv_bottom_left_x";
    private static final String KEY_PANEL_BACKGROUND_IMAGE_UV_BOTTOM_LEFT_Y =
            "panel_background_image_uv_bottom_left_y";
    private static final String KEY_PANEL_BACKGROUND_IMAGE_UV_BOTTOM_RIGHT_X =
            "panel_background_image_uv_bottom_right_x";
    private static final String KEY_PANEL_BACKGROUND_IMAGE_UV_BOTTOM_RIGHT_Y =
            "panel_background_image_uv_bottom_right_y";
    private static final String KEY_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT =
            "panel_background_image_opacity_percent";

    private static final String KEY_THEME_BACKGROUND_COLOR =
            "theme_background_color";
    private static final String KEY_THEME_BACKGROUND_TYPE =
            "theme_background_type";
    private static final String KEY_THEME_BACKGROUND_COLOR_2 =
            "theme_background_color_2";
    private static final String KEY_THEME_BACKGROUND_DIRECTION =
            "theme_background_direction";
    private static final String KEY_THEME_ACCENT_COLOR =
            "theme_accent_color";
    private static final String KEY_THEME_PRIMARY_TEXT_COLOR =
            "theme_primary_text_color";
    private static final String KEY_THEME_SECONDARY_TEXT_COLOR =
            "theme_secondary_text_color";
    private static final String KEY_TILE_SHAPE = "tile_shape";

    private static final int CURRENT_CONFIG_VERSION = 1;

    public static final String[] AVAILABLE_TILE_SPECS = {
            // Existing validated tiles
            "wifi",
            "data",
            "bluetooth",
            "flashlight",
            "rotation",
            "location",
            "hotspot",
            "airplane",
            "dnd",
            "battery",

            // Additional validated tiles
            "nfc",
            "dark",
            "silent",
            "vibrate",
            "reading",
            "screenshot",
            "camera"
    };

    public static final String DEFAULT_TILE_ORDER =
            "wifi,bluetooth,data,flashlight,rotation,dnd,location,battery";

    public static final int MIN_TILE_COLUMNS = 3;
    public static final int MAX_TILE_COLUMNS = 6;
    public static final int DEFAULT_TILE_COLUMNS = 4;

    public static final int MIN_TILE_ROWS = 1;
    public static final int MAX_TILE_ROWS = 4;
    public static final int DEFAULT_TILE_ROWS = 2;

    /** Visual button diameter/side. Touch target remains at least 48 dp. */
    public static final int MIN_TILE_SIZE_DP = 40;
    public static final int MAX_TILE_SIZE_DP = 64;
    public static final int DEFAULT_TILE_SIZE_DP = 52;

    public static final int MIN_TILE_CORNER_RADIUS_DP = 0;
    public static final int MAX_TILE_CORNER_RADIUS_DP = 32;
    public static final int DEFAULT_TILE_CORNER_RADIUS_DP = 26;

    public static final int MIN_PANEL_OPACITY_PERCENT = 20;
    public static final int MAX_PANEL_OPACITY_PERCENT = 100;
    public static final int DEFAULT_PANEL_OPACITY_PERCENT = 100;

    public static final int MIN_PANEL_CORNER_RADIUS_DP = 0;
    public static final int MAX_PANEL_CORNER_RADIUS_DP = 24;
    public static final int DEFAULT_PANEL_CORNER_RADIUS_DP = 24;

    public static final int MIN_NOTIFICATION_TEXT_SIZE_SP = 11;
    public static final int MAX_NOTIFICATION_TEXT_SIZE_SP = 18;
    public static final int DEFAULT_NOTIFICATION_TEXT_SIZE_SP = 15;

    public static final int MIN_NOTIFICATION_BACKGROUND_OPACITY_PERCENT = 0;
    public static final int MAX_NOTIFICATION_BACKGROUND_OPACITY_PERCENT = 100;
    public static final int DEFAULT_NOTIFICATION_BACKGROUND_OPACITY_PERCENT = 100;

    public static final int MIN_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT = 0;
    public static final int MAX_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT = 100;
    public static final int DEFAULT_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT = 83;

    public static final int DEFAULT_THEME_BACKGROUND_COLOR =
            0xFF242424;
    public static final int DEFAULT_THEME_BACKGROUND_TYPE = 0;
    public static final int DEFAULT_THEME_BACKGROUND_COLOR_2 = 0xFF242424;
    public static final int DEFAULT_THEME_BACKGROUND_DIRECTION = 0;
    public static final int DEFAULT_THEME_ACCENT_COLOR =
            0xFFCCCCCC;
    public static final int DEFAULT_THEME_PRIMARY_TEXT_COLOR =
            0xFFF0F0F0;
    public static final int DEFAULT_THEME_SECONDARY_TEXT_COLOR =
            0xFFBDBDBD;

    /*
     * Historical/deprecated preference: tile_shape no longer controls the
     * active tile renderer, which uses tile size and corner radius instead.
     * Keep these values and the stored key for compatibility with existing
     * preferences and configuration backup/import files.
     */
    public static final int TILE_SHAPE_CIRCLE = 0;
    public static final int TILE_SHAPE_ROUNDED = 1;
    public static final int TILE_SHAPE_SQUARE = 2;
    public static final int DEFAULT_TILE_SHAPE = 1;

    private final Context context;
    private final SharedPreferences preferences;

    public PanOSPPreferences(Context context) {
        this.context = context.getApplicationContext();
        preferences =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREFS_NAME,
                                Context.MODE_PRIVATE
                        );

        synchronized (PanOSPPreferences.class) {
            if (preferences.getAll().isEmpty()) {
                // Publish the version only with the complete default, so a failure can retry.
                resetPanelConfiguration();
            } else {
                migrateIfNeeded();
            }
        }
    }

    // =========================================================================
    // TILES
    // =========================================================================

    public List<String> getTileSpecs() {
        String stored =
                preferences.getString(
                        KEY_TILE_ORDER,
                        DEFAULT_TILE_ORDER
                );

        List<String> result = parseAndSanitize(stored);

        if (result.isEmpty()) {
            result = parseAndSanitize(DEFAULT_TILE_ORDER);
        }

        return result;
    }

    public void setTileSpecs(List<String> specs) {
        List<String> sanitized = sanitize(specs);

        if (sanitized.isEmpty()) {
            sanitized.add(AVAILABLE_TILE_SPECS[0]);
        }

        preferences.edit()
                .putString(
                        KEY_TILE_ORDER,
                        String.join(",", sanitized)
                )
                .apply();
    }

    public List<String> getAvailableTileSpecs() {
        return new ArrayList<>(
                Arrays.asList(AVAILABLE_TILE_SPECS)
        );
    }

    public boolean isKnownTileSpec(String spec) {
        if (spec == null) {
            return false;
        }

        for (String candidate : AVAILABLE_TILE_SPECS) {
            if (candidate.equals(spec)) {
                return true;
            }
        }

        return false;
    }

    // =========================================================================
    // GRID
    // =========================================================================

    public int getTileColumns() {
        return clamp(
                preferences.getInt(
                        KEY_TILE_COLUMNS,
                        DEFAULT_TILE_COLUMNS
                ),
                MIN_TILE_COLUMNS,
                MAX_TILE_COLUMNS
        );
    }

    public void setTileColumns(int columns) {
        preferences.edit()
                .putInt(
                        KEY_TILE_COLUMNS,
                        clamp(
                                columns,
                                MIN_TILE_COLUMNS,
                                MAX_TILE_COLUMNS
                        )
                )
                .apply();
    }

    public int getTileRows() {
        return clamp(
                preferences.getInt(
                        KEY_TILE_ROWS,
                        DEFAULT_TILE_ROWS
                ),
                MIN_TILE_ROWS,
                MAX_TILE_ROWS
        );
    }

    public void setTileRows(int rows) {
        preferences.edit()
                .putInt(
                        KEY_TILE_ROWS,
                        clamp(
                                rows,
                                MIN_TILE_ROWS,
                                MAX_TILE_ROWS
                        )
                )
                .apply();
    }

    public int getTileCapacity() {
        return getTileColumns() * getTileRows();
    }

    // =========================================================================
    // APPEARANCE
    // =========================================================================

    public int getTileSizeDp() {
        return clamp(
                preferences.getInt(
                        KEY_TILE_SIZE_DP,
                        DEFAULT_TILE_SIZE_DP
                ),
                MIN_TILE_SIZE_DP,
                MAX_TILE_SIZE_DP
        );
    }

    public void setTileSizeDp(int sizeDp) {
        preferences.edit()
                .putInt(
                        KEY_TILE_SIZE_DP,
                        clamp(
                                sizeDp,
                                MIN_TILE_SIZE_DP,
                                MAX_TILE_SIZE_DP
                        )
                )
                .apply();
    }

    public int getTileCornerRadiusDp() {
        return clamp(
                preferences.getInt(
                        KEY_TILE_CORNER_RADIUS_DP,
                        DEFAULT_TILE_CORNER_RADIUS_DP
                ),
                MIN_TILE_CORNER_RADIUS_DP,
                MAX_TILE_CORNER_RADIUS_DP
        );
    }

    public void setTileCornerRadiusDp(int radiusDp) {
        preferences.edit()
                .putInt(
                        KEY_TILE_CORNER_RADIUS_DP,
                        clamp(
                                radiusDp,
                                MIN_TILE_CORNER_RADIUS_DP,
                                MAX_TILE_CORNER_RADIUS_DP
                        )
                )
                .apply();
    }

    public int getPanelOpacityPercent() {
        return clamp(
                preferences.getInt(
                        KEY_PANEL_OPACITY_PERCENT,
                        DEFAULT_PANEL_OPACITY_PERCENT
                ),
                MIN_PANEL_OPACITY_PERCENT,
                MAX_PANEL_OPACITY_PERCENT
        );
    }

    public void setPanelOpacityPercent(int percent) {
        preferences.edit()
                .putInt(
                        KEY_PANEL_OPACITY_PERCENT,
                        clamp(
                                percent,
                                MIN_PANEL_OPACITY_PERCENT,
                                MAX_PANEL_OPACITY_PERCENT
                        )
                )
                .apply();
    }

    public int getPanelCornerRadiusDp() {
        return clamp(preferences.getInt(KEY_PANEL_CORNER_RADIUS_DP, DEFAULT_PANEL_CORNER_RADIUS_DP), MIN_PANEL_CORNER_RADIUS_DP, MAX_PANEL_CORNER_RADIUS_DP);
    }

    public void setPanelCornerRadiusDp(int radiusDp) {
        preferences.edit().putInt(KEY_PANEL_CORNER_RADIUS_DP, clamp(radiusDp, MIN_PANEL_CORNER_RADIUS_DP, MAX_PANEL_CORNER_RADIUS_DP)).apply();
    }

    public int getNotificationTextSizeSp() {
        return clamp(
                preferences.getInt(
                        KEY_NOTIFICATION_TEXT_SIZE_SP,
                        DEFAULT_NOTIFICATION_TEXT_SIZE_SP
                ),
                MIN_NOTIFICATION_TEXT_SIZE_SP,
                MAX_NOTIFICATION_TEXT_SIZE_SP
        );
    }

    public void setNotificationTextSizeSp(int sizeSp) {
        preferences.edit()
                .putInt(
                        KEY_NOTIFICATION_TEXT_SIZE_SP,
                        clamp(
                                sizeSp,
                                MIN_NOTIFICATION_TEXT_SIZE_SP,
                                MAX_NOTIFICATION_TEXT_SIZE_SP
                        )
                )
                .apply();
    }


    public int getNotificationBackgroundOpacityPercent() {
        return clamp(
                preferences.getInt(
                        KEY_NOTIFICATION_BACKGROUND_OPACITY_PERCENT,
                        DEFAULT_NOTIFICATION_BACKGROUND_OPACITY_PERCENT
                ),
                MIN_NOTIFICATION_BACKGROUND_OPACITY_PERCENT,
                MAX_NOTIFICATION_BACKGROUND_OPACITY_PERCENT
        );
    }

    public void setNotificationBackgroundOpacityPercent(int percent) {
        preferences.edit()
                .putInt(
                        KEY_NOTIFICATION_BACKGROUND_OPACITY_PERCENT,
                        clamp(
                                percent,
                                MIN_NOTIFICATION_BACKGROUND_OPACITY_PERCENT,
                                MAX_NOTIFICATION_BACKGROUND_OPACITY_PERCENT
                        )
                )
                .apply();
    }

    /** Publish an already validated configuration with one editor; restore memory on disk failure. */
    public boolean publishConfiguration(java.util.Map<String, Object> values) {
        java.util.Map<String, ?> before = preferences.getAll();
        SharedPreferences.Editor editor = preferences.edit();
        for (java.util.Map.Entry<String, Object> entry : values.entrySet()) {
            putValue(editor, entry.getKey(), normalizeBackupValue(entry.getKey(), entry.getValue()));
        }
        if (editor.commit()) return true;
        SharedPreferences.Editor rollback = preferences.edit();
        for (String key : values.keySet()) putValue(rollback, key, before.get(key));
        rollback.commit();
        return false;
    }

    private static void putValue(SharedPreferences.Editor editor, String key, Object value) {
        if (value == null) editor.remove(key);
        else if (value instanceof String) editor.putString(key, (String) value);
        else if (value instanceof Float) editor.putFloat(key, (Float) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else throw new IllegalArgumentException("Unsupported preference value");
    }

    private Object normalizeBackupValue(String key, Object value) {
        if (KEY_TILE_ORDER.equals(key) && value instanceof String) {
            List<String> specs = parseAndSanitize((String) value);
            if (specs.isEmpty()) specs.add(AVAILABLE_TILE_SPECS[0]);
            return String.join(",", specs);
        }
        if (!(value instanceof Integer)) return value;
        int number = (Integer) value;
        switch (key) {
            case KEY_TILE_COLUMNS: return clamp(number, MIN_TILE_COLUMNS, MAX_TILE_COLUMNS);
            case KEY_TILE_ROWS: return clamp(number, MIN_TILE_ROWS, MAX_TILE_ROWS);
            case KEY_TILE_SIZE_DP: return clamp(number, MIN_TILE_SIZE_DP, MAX_TILE_SIZE_DP);
            case KEY_TILE_CORNER_RADIUS_DP: return clamp(number, MIN_TILE_CORNER_RADIUS_DP, MAX_TILE_CORNER_RADIUS_DP);
            case KEY_PANEL_OPACITY_PERCENT: return clamp(number, MIN_PANEL_OPACITY_PERCENT, MAX_PANEL_OPACITY_PERCENT);
            case KEY_PANEL_CORNER_RADIUS_DP: return clamp(number, MIN_PANEL_CORNER_RADIUS_DP, MAX_PANEL_CORNER_RADIUS_DP);
            case KEY_NOTIFICATION_TEXT_SIZE_SP: return clamp(number, MIN_NOTIFICATION_TEXT_SIZE_SP, MAX_NOTIFICATION_TEXT_SIZE_SP);
            case KEY_NOTIFICATION_BACKGROUND_OPACITY_PERCENT: return clamp(number, MIN_NOTIFICATION_BACKGROUND_OPACITY_PERCENT, MAX_NOTIFICATION_BACKGROUND_OPACITY_PERCENT);
            case KEY_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT: return clamp(number, MIN_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT, MAX_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT);
            case KEY_THEME_BACKGROUND_TYPE: return clamp(number, 0, 2);
            case KEY_THEME_BACKGROUND_DIRECTION: return clamp(number, 0, 5);
            case KEY_THEME_BACKGROUND_COLOR:
            case KEY_THEME_BACKGROUND_COLOR_2:
            case KEY_THEME_ACCENT_COLOR:
            case KEY_THEME_PRIMARY_TEXT_COLOR:
            case KEY_THEME_SECONDARY_TEXT_COLOR: return forceOpaque(number);
            case KEY_TILE_SHAPE: return normalizeShape(number);
            default: return value;
        }
    }

    public String getPanelBackgroundImageUri() {
        return preferences.getString(
                KEY_PANEL_BACKGROUND_IMAGE_URI,
                null
        );
    }

    public void setPanelBackgroundImageUri(String uri) {
        SharedPreferences.Editor editor = preferences.edit();

        if (uri == null || uri.trim().isEmpty()) {
            editor.remove(KEY_PANEL_BACKGROUND_IMAGE_URI);
        } else {
            editor.putString(KEY_PANEL_BACKGROUND_IMAGE_URI, uri);
        }

        editor.apply();
    }

    public float getPanelBackgroundImageUvTopLeftX() {
        return preferences.getFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_TOP_LEFT_X, 0f);
    }

    public float getPanelBackgroundImageUvTopLeftY() {
        return preferences.getFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_TOP_LEFT_Y, 0f);
    }

    public float getPanelBackgroundImageUvTopRightX() {
        return preferences.getFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_TOP_RIGHT_X, 1f);
    }

    public float getPanelBackgroundImageUvTopRightY() {
        return preferences.getFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_TOP_RIGHT_Y, 0f);
    }

    public float getPanelBackgroundImageUvBottomLeftX() {
        return preferences.getFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_BOTTOM_LEFT_X, 0f);
    }

    public float getPanelBackgroundImageUvBottomLeftY() {
        return preferences.getFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_BOTTOM_LEFT_Y, 1f);
    }

    public float getPanelBackgroundImageUvBottomRightX() {
        return preferences.getFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_BOTTOM_RIGHT_X, 1f);
    }

    public float getPanelBackgroundImageUvBottomRightY() {
        return preferences.getFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_BOTTOM_RIGHT_Y, 1f);
    }

    public void setPanelBackgroundImageCropUvs(
            float topLeftX,
            float topLeftY,
            float topRightX,
            float topRightY,
            float bottomLeftX,
            float bottomLeftY,
            float bottomRightX,
            float bottomRightY) {

        preferences.edit()
                .putFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_TOP_LEFT_X, topLeftX)
                .putFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_TOP_LEFT_Y, topLeftY)
                .putFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_TOP_RIGHT_X, topRightX)
                .putFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_TOP_RIGHT_Y, topRightY)
                .putFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_BOTTOM_LEFT_X, bottomLeftX)
                .putFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_BOTTOM_LEFT_Y, bottomLeftY)
                .putFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_BOTTOM_RIGHT_X, bottomRightX)
                .putFloat(KEY_PANEL_BACKGROUND_IMAGE_UV_BOTTOM_RIGHT_Y, bottomRightY)
                .apply();
    }

    public int getPanelBackgroundImageOpacityPercent() {
        return clamp(
                preferences.getInt(
                        KEY_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT,
                        DEFAULT_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT
                ),
                MIN_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT,
                MAX_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT
        );
    }

    public void setPanelBackgroundImageOpacityPercent(int percent) {
        preferences.edit()
                .putInt(
                        KEY_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT,
                        clamp(
                                percent,
                                MIN_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT,
                                MAX_PANEL_BACKGROUND_IMAGE_OPACITY_PERCENT
                        )
                )
                .apply();
    }

    public int getThemeBackgroundColor() {
        return forceOpaque(
                preferences.getInt(
                        KEY_THEME_BACKGROUND_COLOR,
                        DEFAULT_THEME_BACKGROUND_COLOR
                )
        );
    }

    public int getThemeBackgroundType() {
        return clamp(preferences.getInt(
                KEY_THEME_BACKGROUND_TYPE,
                DEFAULT_THEME_BACKGROUND_TYPE), 0, 2);
    }

    public void setThemeBackgroundType(int type) {
        preferences.edit().putInt(KEY_THEME_BACKGROUND_TYPE, clamp(type, 0, 2)).apply();
    }

    public int getThemeBackgroundColor2() {
        return forceOpaque(preferences.getInt(
                KEY_THEME_BACKGROUND_COLOR_2,
                DEFAULT_THEME_BACKGROUND_COLOR_2));
    }

    public void setThemeBackgroundColor2(int color) {
        preferences.edit().putInt(KEY_THEME_BACKGROUND_COLOR_2, forceOpaque(color)).apply();
    }

    public int getThemeBackgroundDirection() {
        return clamp(preferences.getInt(
                KEY_THEME_BACKGROUND_DIRECTION,
                DEFAULT_THEME_BACKGROUND_DIRECTION), 0, 5);
    }

    public void setThemeBackgroundDirection(int direction) {
        preferences.edit().putInt(KEY_THEME_BACKGROUND_DIRECTION, clamp(direction, 0, 5)).apply();
    }

    public void setThemeBackgroundColor(int color) {
        preferences.edit()
                .putInt(
                        KEY_THEME_BACKGROUND_COLOR,
                        forceOpaque(color)
                )
                .apply();
    }

    public int getThemeAccentColor() {
        return forceOpaque(
                preferences.getInt(
                        KEY_THEME_ACCENT_COLOR,
                        DEFAULT_THEME_ACCENT_COLOR
                )
        );
    }

    public void setThemeAccentColor(int color) {
        preferences.edit()
                .putInt(
                        KEY_THEME_ACCENT_COLOR,
                        forceOpaque(color)
                )
                .apply();
    }

    public int getThemePrimaryTextColor() {
        return forceOpaque(
                preferences.getInt(
                        KEY_THEME_PRIMARY_TEXT_COLOR,
                        DEFAULT_THEME_PRIMARY_TEXT_COLOR
                )
        );
    }

    public void setThemePrimaryTextColor(int color) {
        preferences.edit()
                .putInt(
                        KEY_THEME_PRIMARY_TEXT_COLOR,
                        forceOpaque(color)
                )
                .apply();
    }

    public int getThemeSecondaryTextColor() {
        return forceOpaque(
                preferences.getInt(
                        KEY_THEME_SECONDARY_TEXT_COLOR,
                        DEFAULT_THEME_SECONDARY_TEXT_COLOR
                )
        );
    }

    public void setThemeSecondaryTextColor(int color) {
        preferences.edit()
                .putInt(
                        KEY_THEME_SECONDARY_TEXT_COLOR,
                        forceOpaque(color)
                )
                .apply();
    }

    public int getTileShape() {
        return normalizeShape(
                preferences.getInt(
                        KEY_TILE_SHAPE,
                        DEFAULT_TILE_SHAPE
                )
        );
    }

    public void setTileShape(int shape) {
        preferences.edit()
                .putInt(
                        KEY_TILE_SHAPE,
                        normalizeShape(shape)
                )
                .apply();
    }

    /** Prepare the embedded reference using the same JSON parser and byte copy as ZIP restores. */
    public java.util.Map<String, Object> prepareDefaultConfiguration() throws Exception {
        java.util.Map<String, Object> values;
        try (java.io.InputStream input = context.getResources().openRawResource(
                com.ubai.veilpanel.R.raw.default_config)) {
            java.io.ByteArrayOutputStream json = new java.io.ByteArrayOutputStream();
            com.ubai.veilpanel.image.BackgroundImageStore.copy(input, json, 1024 * 1024);
            values = PanOSPConfigBackup.parseConfiguration(
                    json.toString(java.nio.charset.StandardCharsets.UTF_8.name()));
        }
        /*
         * The embedded JSON is the complete factory reference.
         * If panel_background_image_uri is null, no background image
         * must be injected implicitly.
         */
        values.put(KEY_CONFIG_VERSION, CURRENT_CONFIG_VERSION);
        return values;
    }

    public boolean resetPanelConfiguration() {
        com.ubai.veilpanel.image.BackgroundImageStore store =
                new com.ubai.veilpanel.image.BackgroundImageStore(context);
        String prepared = null;
        try {
            java.util.Map<String, Object> values = prepareDefaultConfiguration();
            prepared = (String) values.get(KEY_PANEL_BACKGROUND_IMAGE_URI);
            String previous = getPanelBackgroundImageUri();
            if (publishConfiguration(values)) {
                store.deleteManaged(previous);
                return true;
            }
        } catch (Exception | OutOfMemoryError error) {
            android.util.Log.e("PanOSPPreferences", "Cannot initialize default configuration", error);
        }
        store.deleteManaged(prepared);
        return false;
    }

    // =========================================================================
    // MIGRATION
    // =========================================================================

    /**
     * Before the editor existed, TileHost silently appended the five block-2
     * tiles at runtime. Persist them once during migration so the first config
     * screen exactly matches what the user was already seeing.
     */
    private void migrateIfNeeded() {
        int version =
                preferences.getInt(
                        KEY_CONFIG_VERSION,
                        0
                );

        if (version >= CURRENT_CONFIG_VERSION) {
            return;
        }

        String oldOrder =
                preferences.getString(
                        KEY_TILE_ORDER,
                        "wifi,data,bluetooth,flashlight,rotation"
                );

        List<String> migrated = parseAndSanitize(oldOrder);
        Set<String> present = new HashSet<>(migrated);

        for (String spec : AVAILABLE_TILE_SPECS) {
            if (!present.contains(spec)) {
                migrated.add(spec);
                present.add(spec);
            }
        }

        SharedPreferences.Editor editor =
                preferences.edit()
                        .putString(
                                KEY_TILE_ORDER,
                                String.join(",", migrated)
                        )
                        .putInt(
                                KEY_CONFIG_VERSION,
                                CURRENT_CONFIG_VERSION
                        );

        if (!preferences.contains(KEY_TILE_COLUMNS)) {
            editor.putInt(
                    KEY_TILE_COLUMNS,
                    DEFAULT_TILE_COLUMNS
            );
        }

        if (!preferences.contains(KEY_TILE_ROWS)) {
            editor.putInt(
                    KEY_TILE_ROWS,
                    DEFAULT_TILE_ROWS
            );
        }

        if (!preferences.contains(KEY_TILE_SIZE_DP)) {
            editor.putInt(
                    KEY_TILE_SIZE_DP,
                    DEFAULT_TILE_SIZE_DP
            );
        }

        if (!preferences.contains(KEY_TILE_SHAPE)) {
            editor.putInt(
                    KEY_TILE_SHAPE,
                    DEFAULT_TILE_SHAPE
            );
        }

        editor.apply();
    }

    private List<String> parseAndSanitize(String stored) {
        if (stored == null || stored.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return sanitize(
                Arrays.asList(stored.split(","))
        );
    }

    private List<String> sanitize(List<String> specs) {
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        if (specs == null) {
            return result;
        }

        for (String spec : specs) {
            if (spec == null) {
                continue;
            }

            String cleaned = spec.trim();

            if (!cleaned.isEmpty()
                    && isKnownTileSpec(cleaned)
                    && seen.add(cleaned)) {
                result.add(cleaned);
            }
        }

        return result;
    }

    private int normalizeShape(int shape) {
        switch (shape) {
            case TILE_SHAPE_CIRCLE:
            case TILE_SHAPE_ROUNDED:
            case TILE_SHAPE_SQUARE:
                return shape;

            default:
                return DEFAULT_TILE_SHAPE;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int forceOpaque(int color) {
        return color | 0xFF000000;
    }

}
