package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.system.BrightnessController;
import com.ubai.veilpanel.system.SettingsLauncher;

/**
 * Thick brightness control inspired by modern OEM panels.
 *
 * Function and appearance are intentionally separated:
 * BrightnessController owns system state, while this class owns dimensions,
 * track colors, rounded corners and the AUTO affordance.
 */
public final class BrightnessControlView
        extends LinearLayout {

    private final BrightnessController
            brightnessController;

    private final SettingsLauncher
            settingsLauncher;

    private final SeekBar brightnessSeek;
    private final Button autoButton;

    public BrightnessControlView(
            Context context,
            BrightnessController brightnessController,
            SettingsLauncher settingsLauncher) {

        super(context);

        this.brightnessController =
                brightnessController;

        this.settingsLauncher =
                settingsLauncher;

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        // =====================================================================
        // THICK SLIDER
        // =====================================================================

        FrameLayout sliderContainer =
                new FrameLayout(context);

        LinearLayout.LayoutParams
                sliderContainerParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(PanOSPTheme.BRIGHTNESS_HEIGHT_DP),
                        1f
                );

        sliderContainer.setLayoutParams(
                sliderContainerParams
        );

        brightnessSeek = new SeekBar(context);
        brightnessSeek.setMax(
                BrightnessController.MAX_BRIGHTNESS
                        - BrightnessController.MIN_BRIGHTNESS
        );

        brightnessSeek.setPadding(0, 0, 0, 0);
        brightnessSeek.setMinHeight(
                dp(PanOSPTheme.BRIGHTNESS_HEIGHT_DP)
        );
        brightnessSeek.setMaxHeight(
                dp(PanOSPTheme.BRIGHTNESS_HEIGHT_DP)
        );
        brightnessSeek.setSplitTrack(false);
        int brightnessTrackDifference =
                PanOSPTheme.BRIGHTNESS_HEIGHT_DP
                        - PanOSPTheme.BRIGHTNESS_TRACK_HEIGHT_DP;

        int brightnessTrackTopInset =
                dp(brightnessTrackDifference / 2);

        int brightnessTrackBottomInset =
                dp(
                        brightnessTrackDifference
                                - brightnessTrackDifference / 2
                );

        brightnessSeek.setProgressDrawable(
                new InsetDrawable(
                        createProgressDrawable(),
                        0,
                        brightnessTrackTopInset,
                        0,
                        brightnessTrackBottomInset
                )
        );

        /*
         * MIUI-style track: the whole bar is the control, so the classic
         * Android thumb is deliberately hidden.
         */
        brightnessSeek.setThumb(
                new ColorDrawable(Color.TRANSPARENT)
        );
        brightnessSeek.setThumbOffset(0);

        FrameLayout.LayoutParams seekParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(PanOSPTheme.BRIGHTNESS_HEIGHT_DP)
                );

        brightnessSeek.setLayoutParams(seekParams);

        brightnessSeek.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser) {

                        if (!fromUser
                                || !brightnessController
                                .canWrite()) {
                            return;
                        }

                        brightnessController.setBrightness(
                                progress
                                        + BrightnessController
                                        .MIN_BRIGHTNESS
                        );
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar seekBar) {
                    }
                }
        );

        sliderContainer.addView(brightnessSeek);

        ImageView sunIcon =
                new ImageView(context);

        sunIcon.setImageResource(
                R.drawable.ic_qs_brightness
        );

        sunIcon.setColorFilter(
                PanOSPTheme.brightnessIcon(getContext())
        );

        sunIcon.setClickable(false);
        sunIcon.setFocusable(false);

        FrameLayout.LayoutParams sunParams =
                new FrameLayout.LayoutParams(
                        dp(20),
                        dp(20),
                        Gravity.START
                                | Gravity.CENTER_VERTICAL
                );

        sunParams.leftMargin = dp(12);

        sliderContainer.addView(
                sunIcon,
                sunParams
        );

        addView(sliderContainer);

        // =====================================================================
        // AUTO BUTTON
        // =====================================================================

        autoButton = new Button(context);
        autoButton.setAllCaps(false);
        autoButton.setTextSize(10);
        autoButton.setTextColor(
                PanOSPTheme.autoText(getContext(), false)
        );
        autoButton.setMinWidth(0);
        autoButton.setMinimumWidth(0);
        autoButton.setPadding(
                dp(10),
                0,
                dp(10),
                0
        );

        LinearLayout.LayoutParams autoParams =
                new LinearLayout.LayoutParams(
                        dp(66),
                        dp(PanOSPTheme.BRIGHTNESS_HEIGHT_DP)
                );

        autoParams.leftMargin = dp(8);
        autoButton.setLayoutParams(autoParams);

        autoButton.setOnClickListener(view -> {
            if (!brightnessController.canWrite()) {
                settingsLauncher.openWriteSettings();
                return;
            }

            brightnessController.setAutomatic(
                    !brightnessController.isAutomatic()
            );

            refresh();
        });

        addView(autoButton);

        refresh();
    }

    // =========================================================================
    // STATE
    // =========================================================================

    public void refresh() {
        boolean canWrite =
                brightnessController.canWrite();

        brightnessSeek.setEnabled(canWrite);

        int progress =
                brightnessController.getBrightness()
                        - BrightnessController
                        .MIN_BRIGHTNESS;

        brightnessSeek.setProgress(progress);

        if (!canWrite) {
            autoButton.setTextColor(
                    PanOSPTheme.autoText(
                            getContext(),
                            false
                    )
            );

            autoButton.setText(
                    R.string.settings_required
            );
            autoButton.setBackground(
                    createAutoBackground(false)
            );
            return;
        }

        boolean automatic =
                brightnessController.isAutomatic();

        autoButton.setTextColor(
                PanOSPTheme.autoText(
                        getContext(),
                        automatic
                )
        );

        autoButton.setText(
                automatic
                        ? R.string.auto_brightness_on
                        : R.string.auto_brightness_off
        );

        autoButton.setBackground(
                createAutoBackground(automatic)
        );
    }

    // =========================================================================
    // DRAWABLES
    // =========================================================================

    private LayerDrawable createProgressDrawable() {
        GradientDrawable background =
                roundedDrawable(
                        PanOSPTheme
                                .BRIGHTNESS_TRACK_BACKGROUND
                );

        GradientDrawable progressShape =
                roundedDrawable(
                        PanOSPTheme
                                .BRIGHTNESS_TRACK_PROGRESS
                );

        ClipDrawable progress =
                new ClipDrawable(
                        progressShape,
                        Gravity.START,
                        ClipDrawable.HORIZONTAL
                );

        LayerDrawable drawable =
                new LayerDrawable(
                        new android.graphics.drawable.Drawable[]{
                                background,
                                progress
                        }
                );

        drawable.setId(
                0,
                android.R.id.background
        );

        drawable.setId(
                1,
                android.R.id.progress
        );

        return drawable;
    }

    private GradientDrawable createAutoBackground(
            boolean active) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(
                active
                        ? PanOSPTheme
                        .AUTO_BUTTON_ACTIVE_BACKGROUND
                        : PanOSPTheme
                        .AUTO_BUTTON_BACKGROUND
        );

        drawable.setCornerRadius(
        dp(
                Math.min(
                        new com.ubai.veilpanel.config.PanOSPPreferences(
                                getContext()
                        ).getTileCornerRadiusDp(),
                        PanOSPTheme.BRIGHTNESS_HEIGHT_DP / 2
                )
        )
);

        return drawable;
    }

    private GradientDrawable roundedDrawable(
            int color) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);

        drawable.setCornerRadius(
        dp(
                Math.min(
                        new com.ubai.veilpanel.config.PanOSPPreferences(
                                getContext()
                        ).getTileCornerRadiusDp(),
                        PanOSPTheme.BRIGHTNESS_HEIGHT_DP / 2
                )
        )
);

        return drawable;
    }

    private int dp(int value) {
        return Math.round(
                value * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
