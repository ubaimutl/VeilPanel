package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.Color;

import com.ubai.veilpanel.config.PanOSPPreferences;

/**
 * Central visual defaults for the PanOSP shade.
 *
 * IMPORTANT:
 * Keep visual constants here instead of scattering colors / sizes through
 * individual views. This is the base for the future Appearance screen where
 * these defaults will be replaced by values stored in PanOSPPreferences.
 */
public final class PanOSPTheme {

    private PanOSPTheme() {
    }

    // =========================================================================
    // PANEL
    // =========================================================================

    public static final int PANEL_PADDING_DP = 24;

    // =========================================================================
    // QUICK TILES
    // =========================================================================

    /*
     * Five columns fit the current five functional tiles on one line.
     * The TileGridView already supports additional rows, so later this value
     * can become a user preference (4 / 5 / 6 columns, 1 / 2 visible rows).
     */
    public static final int DEFAULT_TILE_COLUMNS = 5;

    public static final int TILE_HORIZONTAL_GAP_DP = 4;
    public static final int TILE_VERTICAL_GAP_DP = 6;

    /*
     * Neutral initial palette.
     * No OEM-specific blue is imposed at install time.
     */
    public static final int TILE_INACTIVE_BACKGROUND =
            Color.rgb(60, 60, 60);

    public static final int TILE_UNAVAILABLE_BACKGROUND =
            Color.rgb(45, 45, 45);

    // =========================================================================
    // BRIGHTNESS
    // =========================================================================

    /*
     * Deliberately thick, MIUI-like touch target.
     * The height is isolated here so it can later become a user preference.
     */
    public static final int BRIGHTNESS_HEIGHT_DP = 44;
    public static final int BRIGHTNESS_TRACK_HEIGHT_DP = 33;
    public static final int BRIGHTNESS_TRACK_BACKGROUND =
            Color.rgb(60, 60, 60);

    public static final int BRIGHTNESS_TRACK_PROGRESS =
            Color.rgb(170, 170, 170);

    public static final int AUTO_BUTTON_BACKGROUND =
            Color.rgb(60, 60, 60);

    public static final int AUTO_BUTTON_ACTIVE_BACKGROUND =
            Color.rgb(58, 58, 58);

    // =========================================================================
    // NOTIFICATIONS
    // =========================================================================

    // =========================================================================
    // TEXT
    // =========================================================================

    public static final int TEXT_SECONDARY =
            Color.rgb(185, 185, 185);

    // =========================================================================
    // DYNAMIC GLOBAL PALETTE
    // =========================================================================

    public static boolean followsWallpaper(Context context) { return context.getSharedPreferences("veil_panel",0).getBoolean("dynamic",true); }

    public static int panelBackground(Context context) {
        return followsWallpaper(context) ? context.getColor(android.R.color.system_neutral1_900) : prefs(context).getThemeBackgroundColor();
    }

    public static int accent(Context context) {
        return followsWallpaper(context) ? context.getColor(android.R.color.system_accent1_200) : prefs(context).getThemeAccentColor();
    }

    public static int primaryText(Context context) {
        PanOSPPreferences p = prefs(context);

        return readableColor(
                followsWallpaper(context) ? context.getColor(android.R.color.system_neutral1_50) : p.getThemePrimaryTextColor(),
                panelBackground(context),
                4.5
        );
    }

    public static int secondaryText(Context context) {
        PanOSPPreferences p = prefs(context);

        return readableColor(
                followsWallpaper(context) ? context.getColor(android.R.color.system_neutral2_200) : p.getThemeSecondaryTextColor(),
                panelBackground(context),
                3.0
        );
    }

    public static int tileActiveBackground(Context context) {
        return accent(context);
    }

    public static int tileInactiveBackground(Context context) {
        return TILE_INACTIVE_BACKGROUND;
    }

    public static int tileUnavailableBackground(Context context) {
        return TILE_UNAVAILABLE_BACKGROUND;
    }

    public static int tileActiveForeground(Context context) {
        PanOSPPreferences p = prefs(context);

        return readableColor(
                p.getThemePrimaryTextColor(),
                tileActiveBackground(context),
                4.5
        );
    }

    public static int tileInactiveForeground(Context context) {
        PanOSPPreferences p = prefs(context);

        return readableColor(
                p.getThemeSecondaryTextColor(),
                tileInactiveBackground(context),
                3.0
        );
    }

    public static int tileUnavailableForeground(Context context) {
        return blend(
                tileUnavailableBackground(context),
                readableColor(
                        prefs(context).getThemeSecondaryTextColor(),
                        tileUnavailableBackground(context),
                        3.0
                ),
                0.55f
        );
    }

    public static int notificationBackground(Context context) {
        return context.getColor(android.R.color.system_neutral1_800);
    }

    public static int notificationPrimaryText(Context context) {
        return readableColor(
                prefs(context).getThemePrimaryTextColor(),
                notificationBackground(context),
                4.5
        );
    }

    public static int notificationSecondaryText(Context context) {
        return readableColor(
                prefs(context).getThemeSecondaryTextColor(),
                notificationBackground(context),
                3.0
        );
    }

    public static int notificationDescriptionText(Context context) {
        int brightness=Math.max(0,Math.min(100,context.getSharedPreferences("veil_panel",0).getInt("description_brightness",0)));
        return blend(notificationSecondaryText(context),android.graphics.Color.WHITE,brightness/100f);
    }

    public static int brightnessBackground(Context context) {
        return BRIGHTNESS_TRACK_BACKGROUND;
    }

    public static int brightnessProgress(Context context) {
        return blend(
                accent(context),
                readableColor(
                        prefs(context).getThemePrimaryTextColor(),
                        accent(context),
                        3.0
                ),
                0.57f
        );
    }

    public static int brightnessIcon(Context context) {
        int requested =
                prefs(context).getThemePrimaryTextColor();

        return readableAcross(
                requested,
                brightnessBackground(context),
                brightnessProgress(context),
                3.0
        );
    }

    public static int autoBackground(Context context) {
        return brightnessBackground(context);
    }

    public static int autoActiveBackground(Context context) {
        return accent(context);
    }

    public static int autoText(
            Context context,
            boolean active) {

        return readableColor(
                prefs(context).getThemePrimaryTextColor(),
                active
                        ? autoActiveBackground(context)
                        : autoBackground(context),
                4.5
        );
    }

    public static int readableColor(
            int requested,
            int background,
            double minimumRatio) {

        requested |= 0xFF000000;
        background |= 0xFF000000;

        if (contrastRatio(
                requested,
                background
        ) >= minimumRatio) {
            return requested;
        }

        double black =
                contrastRatio(
                        Color.BLACK,
                        background
                );

        double white =
                contrastRatio(
                        Color.WHITE,
                        background
                );

        return black >= white
                ? Color.BLACK
                : Color.WHITE;
    }

    private static int readableAcross(
            int requested,
            int backgroundA,
            int backgroundB,
            double minimumRatio) {

        requested |= 0xFF000000;

        if (contrastRatio(
                requested,
                backgroundA
        ) >= minimumRatio
                && contrastRatio(
                requested,
                backgroundB
        ) >= minimumRatio) {
            return requested;
        }

        double black =
                Math.min(
                        contrastRatio(
                                Color.BLACK,
                                backgroundA
                        ),
                        contrastRatio(
                                Color.BLACK,
                                backgroundB
                        )
                );

        double white =
                Math.min(
                        contrastRatio(
                                Color.WHITE,
                                backgroundA
                        ),
                        contrastRatio(
                                Color.WHITE,
                                backgroundB
                        )
                );

        return black >= white
                ? Color.BLACK
                : Color.WHITE;
    }

    public static double contrastRatio(
            int foreground,
            int background) {

        double l1 = luminance(foreground);
        double l2 = luminance(background);

        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);

        return (lighter + 0.05)
                / (darker + 0.05);
    }

    private static double luminance(int color) {
        double r =
                linear(Color.red(color) / 255.0);

        double g =
                linear(Color.green(color) / 255.0);

        double b =
                linear(Color.blue(color) / 255.0);

        return 0.2126 * r
                + 0.7152 * g
                + 0.0722 * b;
    }

    private static double linear(double value) {
        return value <= 0.04045
                ? value / 12.92
                : Math.pow(
                        (value + 0.055) / 1.055,
                        2.4
                );
    }

    public static int blend(
            int background,
            int foreground,
            float amount) {

        amount =
                Math.max(
                        0f,
                        Math.min(1f, amount)
                );

        int r =
                Math.round(
                        Color.red(background)
                                * (1f - amount)
                                + Color.red(foreground)
                                * amount
                );

        int g =
                Math.round(
                        Color.green(background)
                                * (1f - amount)
                                + Color.green(foreground)
                                * amount
                );

        int b =
                Math.round(
                        Color.blue(background)
                                * (1f - amount)
                                + Color.blue(foreground)
                                * amount
                );

        return Color.rgb(r, g, b);
    }

    private static PanOSPPreferences prefs(
            Context context) {

        return new PanOSPPreferences(context);
    }

}
