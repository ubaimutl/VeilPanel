package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ubai.veilpanel.config.PanOSPPreferences;
import com.ubai.veilpanel.tiles.PanTile;
import com.ubai.veilpanel.tiles.TileStatus;

/**
 * Visual representation of one quick-settings tile.
 *
 * The clickable cell and the visible tile button are deliberately separate:
 * the visible circle/square may be as small as 40 dp while the complete cell
 * keeps an Android-friendly touch target of at least 48 dp.
 */
public final class TileView extends LinearLayout {

    private final FrameLayout visualButton;
    private final ImageView iconView;
    private final TextView labelView;

    private PanTile tile;

    private int visualSizeDp =
            PanOSPPreferences.DEFAULT_TILE_SIZE_DP;
    private int tileCornerRadiusDp =
            PanOSPPreferences.DEFAULT_TILE_CORNER_RADIUS_DP;

    public TileView(Context context) {
        super(context);

        setOrientation(VERTICAL);
        setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        setClickable(true);
        setFocusable(true);
        setMinimumHeight(dp(48));
        setMinimumWidth(dp(48));
        setPadding(dp(2), dp(4), dp(2), dp(4));

        visualButton = new FrameLayout(context);
        addView(visualButton);

        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        FrameLayout.LayoutParams iconParams =
                new FrameLayout.LayoutParams(
                        dp(25),
                        dp(25),
                        Gravity.CENTER
                );

        visualButton.addView(iconView, iconParams);

        labelView = new TextView(context);
        labelView.setGravity(Gravity.CENTER);
        labelView.setTextSize(13f);
        labelView.setMaxLines(2);
        PanelStyle.apply(labelView);
        labelView.setPadding(0, dp(8), 0, 0);

        LayoutParams labelParams =
                new LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                );

        labelView.setLayoutParams(labelParams);
        addView(labelView);

        applyVisualStyle(
                visualSizeDp,
                tileCornerRadiusDp
        );
    }

    // =========================================================================
    // BINDING
    // =========================================================================

    public void bind(PanTile tile) {
        this.tile = tile;

        iconView.setImageResource(
                TileIconResolver.resolve(tile)
        );

        refresh();
    }

    public PanTile getTile() {
        return tile;
    }

    public void applyVisualStyle(
            int requestedSizeDp,
            int requestedCornerRadiusDp) {

        visualSizeDp = Math.max(32, requestedSizeDp);

        tileCornerRadiusDp =
                Math.max(
                        PanOSPPreferences.MIN_TILE_CORNER_RADIUS_DP,
                        Math.min(
                                requestedCornerRadiusDp,
                                visualSizeDp / 2
                        )
                );

        LayoutParams visualParams =
                new LayoutParams(
                        dp(visualSizeDp),
                        dp(visualSizeDp)
                );
        visualParams.gravity = Gravity.CENTER_HORIZONTAL;
        visualButton.setLayoutParams(visualParams);

        int iconSizeDp =
                Math.max(
                        20,
                        Math.min(
                                30,
                                Math.round(visualSizeDp * 0.48f)
                        )
                );

        FrameLayout.LayoutParams iconParams =
                new FrameLayout.LayoutParams(
                        dp(iconSizeDp),
                        dp(iconSizeDp),
                        Gravity.CENTER
                );
        iconView.setLayoutParams(iconParams);

        setMinimumHeight(
                dp(Math.max(48, visualSizeDp) + 30)
        );

        refresh();
    }

    // =========================================================================
    // STATE RENDERING
    // =========================================================================

    public void refresh() {
        if (tile == null) {
            return;
        }

        labelView.setText(tile.getDisplayText());

        TileStatus status = tile.getStatus();

        int backgroundColor;
        int foregroundColor;
        int labelColor;

        if (status == TileStatus.ACTIVE) {
            backgroundColor =
                    PanOSPTheme.tileActiveBackground(getContext());
            foregroundColor =
                    PanOSPTheme.tileActiveForeground(getContext());
            labelColor =
                    PanOSPTheme.primaryText(getContext());

        } else if (status == TileStatus.INACTIVE) {
            backgroundColor =
                    PanOSPTheme.tileInactiveBackground(getContext());
            foregroundColor =
                    PanOSPTheme.tileInactiveForeground(getContext());
            labelColor =
                    PanOSPTheme.secondaryText(getContext());

        } else {
            backgroundColor =
                    PanOSPTheme.tileUnavailableBackground(getContext());
            foregroundColor =
                    PanOSPTheme.tileUnavailableForeground(getContext());
            labelColor =
                    PanOSPTheme.tileUnavailableForeground(getContext());
        }

        visualButton.setBackground(
                createShapeBackground(backgroundColor)
        );

        iconView.setColorFilter(foregroundColor);
        labelView.setTextColor(labelColor);

        setAlpha(
                status == TileStatus.ACTIVE
                        || status == TileStatus.INACTIVE
                        ? 1.0f
                        : 0.70f
        );
    }

    private GradientDrawable createShapeBackground(
            int color) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);

        int radiusDp =
                Math.max(
                        PanOSPPreferences.MIN_TILE_CORNER_RADIUS_DP,
                        Math.min(
                                tileCornerRadiusDp,
                                visualSizeDp / 2
                        )
                );

        drawable.setCornerRadius(dp(radiusDp));
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
