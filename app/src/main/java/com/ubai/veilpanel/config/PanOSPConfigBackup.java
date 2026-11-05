package com.ubai.veilpanel.config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Versioned JSON import/export for PanOSP user configuration. */
public final class PanOSPConfigBackup {

    public static final int FORMAT_VERSION = 1;

    private PanOSPConfigBackup() {
    }

    public static String exportToJson(PanOSPPreferences preferences)
            throws JSONException {

        JSONObject root = new JSONObject();
        root.put("format_version", FORMAT_VERSION);

        JSONArray tiles = new JSONArray();
        for (String spec : preferences.getTileSpecs()) {
            tiles.put(spec);
        }
        root.put("tile_order", tiles);

        root.put("tile_columns", preferences.getTileColumns());
        root.put("tile_rows", preferences.getTileRows());
        root.put("tile_size_dp", preferences.getTileSizeDp());
        root.put("tile_corner_radius_dp", preferences.getTileCornerRadiusDp());
        root.put("panel_opacity_percent", preferences.getPanelOpacityPercent());
        root.put("panel_corner_radius_dp", preferences.getPanelCornerRadiusDp());
        root.put("notification_text_size_sp", preferences.getNotificationTextSizeSp());
        root.put("notification_background_opacity_percent", preferences.getNotificationBackgroundOpacityPercent());
        root.put("panel_background_image_opacity_percent", preferences.getPanelBackgroundImageOpacityPercent());
        root.put("theme_background_color", preferences.getThemeBackgroundColor());
        root.put("theme_background_type", preferences.getThemeBackgroundType());
        root.put("theme_background_color_2", preferences.getThemeBackgroundColor2());
        root.put("theme_background_direction", preferences.getThemeBackgroundDirection());
        root.put("theme_accent_color", preferences.getThemeAccentColor());
        root.put("theme_primary_text_color", preferences.getThemePrimaryTextColor());
        root.put("theme_secondary_text_color", preferences.getThemeSecondaryTextColor());
        root.put("tile_shape", preferences.getTileShape());

        String backgroundUri = preferences.getPanelBackgroundImageUri();
        root.put(
                "panel_background_image_uri",
                backgroundUri == null || backgroundUri.trim().isEmpty()
                        ? JSONObject.NULL
                        : backgroundUri
        );

        root.put(
                "panel_background_image_uv_top_left_x",
                preferences.getPanelBackgroundImageUvTopLeftX()
        );
        root.put(
                "panel_background_image_uv_top_left_y",
                preferences.getPanelBackgroundImageUvTopLeftY()
        );
        root.put(
                "panel_background_image_uv_top_right_x",
                preferences.getPanelBackgroundImageUvTopRightX()
        );
        root.put(
                "panel_background_image_uv_top_right_y",
                preferences.getPanelBackgroundImageUvTopRightY()
        );
        root.put(
                "panel_background_image_uv_bottom_left_x",
                preferences.getPanelBackgroundImageUvBottomLeftX()
        );
        root.put(
                "panel_background_image_uv_bottom_left_y",
                preferences.getPanelBackgroundImageUvBottomLeftY()
        );
        root.put(
                "panel_background_image_uv_bottom_right_x",
                preferences.getPanelBackgroundImageUvBottomRightX()
        );
        root.put(
                "panel_background_image_uv_bottom_right_y",
                preferences.getPanelBackgroundImageUvBottomRightY()
        );

        return root.toString(2);
    }

    public static final String IMAGE_URI = "panel_background_image_uri";
    private static final int MAX_JSON_BYTES = 1024 * 1024;

    public static java.util.Map<String, Object> parseConfiguration(String json) throws JSONException {
        org.json.JSONTokener parser = new org.json.JSONTokener(json);
        Object parsed = parser.nextValue();
        if (!(parsed instanceof JSONObject) || parser.nextClean() != 0) throw new JSONException("Invalid configuration JSON");
        JSONObject root = (JSONObject) parsed;
        if (!(root.get("format_version") instanceof Number)
                || root.getDouble("format_version") != FORMAT_VERSION) throw new JSONException("Unsupported version");
        java.util.Map<String, Object> values = new java.util.HashMap<>();
        if (root.has("tile_order")) {
            JSONArray tiles = root.getJSONArray("tile_order");
            List<String> specs = new ArrayList<>();
            for (int i = 0; i < tiles.length(); i++) specs.add(tiles.getString(i));
            if (!specs.isEmpty()) values.put("tile_order", String.join(",", specs));
        }
        String[] integers = {
            "tile_columns", "tile_rows", "tile_size_dp", "tile_corner_radius_dp",
            "panel_opacity_percent", "panel_corner_radius_dp", "notification_text_size_sp",
            "notification_background_opacity_percent", "panel_background_image_opacity_percent",
            "theme_background_color", "theme_background_type", "theme_background_color_2",
            "theme_background_direction", "theme_accent_color", "theme_primary_text_color",
            "theme_secondary_text_color", "tile_shape"
        };
        for (String key : integers) {
            if (root.has(key)) {
                Object raw = root.get(key);
                if (!(raw instanceof Number)) throw new JSONException("Invalid integer: " + key);
                double number = ((Number) raw).doubleValue();
                if (!Double.isFinite(number) || number != Math.rint(number)
                        || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) throw new JSONException("Invalid integer");
                values.put(key, (int) number);
            }
        }
        if (root.has(IMAGE_URI)) {
            if (!root.isNull(IMAGE_URI) && !(root.get(IMAGE_URI) instanceof String)) throw new JSONException("Invalid image URI");
            String uri = root.isNull(IMAGE_URI) ? null : root.getString(IMAGE_URI);
            values.put(IMAGE_URI, uri == null || uri.trim().isEmpty() ? null : uri);
        }
        String[] corners = {"top_left", "top_right", "bottom_left", "bottom_right"};
        float[] defaults = {0, 0, 1, 0, 0, 1, 1, 1};
        int index = 0;
        for (String corner : corners) {
            for (String axis : new String[]{"x", "y"}) {
                String key = "panel_background_image_uv_" + corner + "_" + axis;
                Object raw = root.has(key) ? root.get(key) : defaults[index];
                if (!(raw instanceof Number)) throw new JSONException("Invalid UV");
                float value = ((Number) raw).floatValue();
                if (!Float.isFinite(value)) throw new JSONException("Non-finite UV");
                values.put(key, value);
                index++;
            }
        }
        return values;
    }

    /** Owns an unpublished image until the activity successfully publishes it. */
    public static final class Restore {
        public final java.util.Map<String, Object> values;
        public final String preparedUri;
        public final String externalUri;
        Restore(java.util.Map<String, Object> values, String preparedUri, String externalUri) {
            this.values = values;
            this.preparedUri = preparedUri;
            this.externalUri = externalUri;
        }
    }

    public static final class MissingImageException extends java.io.IOException {
        MissingImageException(Exception cause) { super("Historical background unavailable", cause); }
    }

    public static Restore readBackup(java.io.InputStream source,
            com.ubai.veilpanel.image.BackgroundImageStore store, String currentUri) throws Exception {
        java.io.BufferedInputStream input = new java.io.BufferedInputStream(source);
        input.mark(4);
        int first = input.read();
        int second = input.read();
        input.reset();
        if (first != 'P' || second != 'K') {
            java.util.Map<String, Object> values = parseConfiguration(readJson(input));
            String uri = values.containsKey(IMAGE_URI) ? (String) values.get(IMAGE_URI) : currentUri;
            String prepared = null;
            if (uri != null) {
                try { prepared = store.prepare(android.net.Uri.parse(uri)); }
                catch (Exception error) { throw new MissingImageException(error); }
            }
            values.put(IMAGE_URI, prepared);
            return new Restore(values, prepared, uri);
        }
        String prepared = null;
        java.io.File archive = store.stageArchive(input);
        try {
            String json = null;
            java.util.Set<String> seen = new java.util.HashSet<>();
            // ZipFile requires a complete central directory; CRC is checked for every entry.
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive)) {
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    java.util.zip.ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory() || !seen.add(name)) throw new java.io.IOException("Duplicate or invalid entry");
                    try (java.util.zip.CheckedInputStream contents = new java.util.zip.CheckedInputStream(
                            zip.getInputStream(entry), new java.util.zip.CRC32())) {
                        if ("config.json".equals(name)) json = readJson(contents);
                        else if ("background.webp".equals(name)) prepared = store.prepareWebp(contents);
                        else throw new java.io.IOException("Unexpected ZIP entry");
                        if (entry.getCrc() != contents.getChecksum().getValue()) throw new java.io.IOException("ZIP checksum mismatch");
                    }
                }
            }
            if (json == null) throw new java.io.IOException("Missing config.json");
            java.util.Map<String, Object> values = parseConfiguration(json);
            if (!values.containsKey(IMAGE_URI)) throw new java.io.IOException("Missing image declaration");
            Object declaration = values.get(IMAGE_URI);
            if (declaration != null && !"background.webp".equals(declaration)) throw new java.io.IOException("Invalid embedded image reference");
            if ((declaration != null) != (prepared != null)) throw new java.io.IOException("Missing or unexpected background");
            values.put(IMAGE_URI, prepared);
            return new Restore(values, prepared, null);
        } catch (Exception | OutOfMemoryError error) {
            store.deleteManaged(prepared);
            throw error;
        } finally { archive.delete(); }
    }

    private static String readJson(java.io.InputStream input) throws java.io.IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        com.ubai.veilpanel.image.BackgroundImageStore.copy(input, output, MAX_JSON_BYTES);
        return output.toString(java.nio.charset.StandardCharsets.UTF_8.name());
    }

    public static void writeBackup(java.io.OutputStream output, String snapshot,
            com.ubai.veilpanel.image.BackgroundImageStore store) throws Exception {
        JSONObject root = new JSONObject(snapshot);
        String uri = root.isNull(IMAGE_URI) ? null : root.getString(IMAGE_URI);
        String temporary = null;
        try {
            if (uri != null && !store.isManaged(uri)) {
                temporary = store.prepare(android.net.Uri.parse(uri));
                uri = temporary;
            }
            if (uri != null) store.validate(new java.io.File(android.net.Uri.parse(uri).getPath()));
            root.put(IMAGE_URI, uri == null ? JSONObject.NULL : "background.webp");
            try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(output)) {
                zip.putNextEntry(new java.util.zip.ZipEntry("config.json"));
                zip.write(root.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
                if (uri != null) {
                    zip.putNextEntry(new java.util.zip.ZipEntry("background.webp"));
                    try (java.io.InputStream image = store.open(android.net.Uri.parse(uri))) {
                        com.ubai.veilpanel.image.BackgroundImageStore.copy(image, zip,
                                com.ubai.veilpanel.image.BackgroundImageStore.MAX_IMAGE_BYTES);
                    }
                    zip.closeEntry();
                }
            }
        } finally { store.deleteManaged(temporary); }
    }
}
