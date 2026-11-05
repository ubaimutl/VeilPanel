package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.ubai.veilpanel.R;
import com.ubai.veilpanel.config.PanOSPPreferences;
import com.ubai.veilpanel.tiles.PanTile;
import com.ubai.veilpanel.tiles.TileStatus;

/**
 * Non-interactive preview of the real PanOSP panel.
 *
 * The content is laid out at 100% using PanOSP's real rendering components,
 * then the whole panel is drawn at PREVIEW_SCALE. This preserves the real
 * proportions while fitting more content across the configuration screen.
 */
public final class PanelPreviewView extends FrameLayout {

    private static final float PREVIEW_SCALE = 0.65f;
    private static final int MIN_PREVIEW_HEIGHT_DP = 340;

    private final PanOSPPreferences preferences;
    private final java.util.Map<String, TileStatus> tileStatusSnapshot;
    private final AtomicLong renderGeneration = new AtomicLong();

    private ScaledPreviewFrame scaledFrame;

    public PanelPreviewView(Context context) {
        this(
                context,
                java.util.Collections.emptyMap()
        );
    }

    public PanelPreviewView(
            Context context,
            java.util.Map<String, TileStatus> tileStatusSnapshot) {

        super(context);

        preferences = new PanOSPPreferences(context);

        this.tileStatusSnapshot =
                tileStatusSnapshot == null
                        ? java.util.Collections.emptyMap()
                        : new java.util.HashMap<>(
                                tileStatusSnapshot
                        );

        setClipChildren(false);
        setClipToPadding(false);
        refresh();
    }

    public void refresh() {
        long generation = renderGeneration.incrementAndGet();

        removeAllViews();

        PreviewContent content = new PreviewContent(getContext());
        content.build();

        scaledFrame = new ScaledPreviewFrame(
                getContext(),
                PREVIEW_SCALE,
                dp(MIN_PREVIEW_HEIGHT_DP)
        );
        scaledFrame.addView(
                content,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        addView(
                scaledFrame,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        loadBackgroundImageAsync(content, generation);
    }

    private final class PreviewContent extends LinearLayout {

        PreviewContent(Context context) {
            super(context);
            setOrientation(VERTICAL);
        }

        void build() {
            setPadding(
                    dp(PanOSPTheme.PANEL_PADDING_DP),
                    dp(10),
                    dp(PanOSPTheme.PANEL_PADDING_DP),
                    dp(PanOSPTheme.PANEL_PADDING_DP)
            );

            // 340 dp visible at 65% ~= 523 dp virtual.
            setMinimumHeight(
                    Math.round(dp(MIN_PREVIEW_HEIGHT_DP) / PREVIEW_SCALE)
            );

            applyPanelBackground(this, null);

            int columns = Math.max(1, preferences.getTileColumns());
            int rows = Math.max(1, preferences.getTileRows());

            TileGridView grid = new TileGridView(
                    getContext(),
                    columns,
                    rows,
                    preferences.getTileSizeDp(),
                    preferences.getTileCornerRadiusDp(),
                    true
            );

            grid.setTiles(createPreviewTiles(columns * rows));

            addView(
                    grid,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            TextView notificationsHeader = new TextView(getContext());
            notificationsHeader.setText(R.string.notifications_header);
            notificationsHeader.setTextColor(
                    PanOSPTheme.secondaryText(getContext())
            );
            notificationsHeader.setTextSize(14);
            notificationsHeader.setPadding(dp(2), dp(10), 0, dp(7));

            addView(
                    notificationsHeader,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            addView(
                    createNotificationPreview(),
                    notificationLayoutParams()
            );
        }
    }

    private List<PanTile> createPreviewTiles(int requestedCount) {
        List<PanTile> tiles = new ArrayList<>();
        List<String> specs = preferences.getTileSpecs();

        List<String> usableSpecs = new ArrayList<>();
        for (String spec : specs) {
            if (tileLabelRes(spec) != 0) {
                usableSpecs.add(spec);
            }
        }

        if (usableSpecs.isEmpty()) {
            usableSpecs.add("wifi");
            usableSpecs.add("bluetooth");
            usableSpecs.add("flashlight");
            usableSpecs.add("rotation");
        }

        int count = Math.max(1, requestedCount);

        for (int i = 0; i < count; i++) {
            String spec = usableSpecs.get(i % usableSpecs.size());
            int labelRes = tileLabelRes(spec);

            TileStatus status;

            if (!tileStatusSnapshot.isEmpty()) {
                status = tileStatusSnapshot.get(spec);

                if (status == null) {
                    status = TileStatus.INACTIVE;
                }
            } else {
                status =
                        i % 3 == 0
                                ? TileStatus.ACTIVE
                                : TileStatus.INACTIVE;
            }

            tiles.add(
                    new PreviewTile(
                            getContext(),
                            spec,
                            labelRes,
                            status
                    )
            );
        }

        return tiles;
    }

    private LinearLayout createNotificationPreview() {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));

        int background = PanOSPTheme.notificationBackground(getContext());
        int opacity = preferences.getNotificationBackgroundOpacityPercent();
        int alpha = Math.round(255f * opacity / 100f);

        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();
        drawable.setShape(
                android.graphics.drawable.GradientDrawable.RECTANGLE
        );
        drawable.setColor(
                Color.argb(
                        alpha,
                        Color.red(background),
                        Color.green(background),
                        Color.blue(background)
                )
        );
        drawable.setCornerRadius(
                dp(preferences.getTileCornerRadiusDp())
        );
        card.setBackground(drawable);

        int textSize = preferences.getNotificationTextSizeSp();

        TextView title = new TextView(getContext());
        title.setText(R.string.app_name);
        title.setTextColor(
                PanOSPTheme.notificationPrimaryText(getContext())
        );
        title.setTextSize(textSize + 1);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        TextView body = new TextView(getContext());
        body.setText(R.string.panel_config_theme_notification_preview);
        body.setTextColor(
                PanOSPTheme.notificationSecondaryText(getContext())
        );
        body.setTextSize(textSize);
        body.setPadding(0, dp(3), 0, 0);
        card.addView(body);

        card.setClickable(false);
        card.setFocusable(false);
        return card;
    }

    private LinearLayout.LayoutParams notificationLayoutParams() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        params.bottomMargin = dp(8);
        return params;
    }

    private void applyPanelBackground(
            View target,
            Bitmap bitmap) {

        int panelOpacity = preferences.getPanelOpacityPercent();
        int alpha = Math.round(255f * panelOpacity / 100f);

        target.setBackground(
                new PanelBackgroundDrawable(
                        preferences.getThemeBackgroundType(),
                        preferences.getThemeBackgroundColor(),
                        preferences.getThemeBackgroundColor2(),
                        preferences.getThemeBackgroundDirection(),
                        alpha,
                        bitmap,
                        preferences.getPanelBackgroundImageOpacityPercent(),
                        dp(preferences.getPanelCornerRadiusDp()),
                        preferences.getPanelBackgroundImageUvTopLeftX(),
                        preferences.getPanelBackgroundImageUvTopLeftY(),
                        preferences.getPanelBackgroundImageUvTopRightX(),
                        preferences.getPanelBackgroundImageUvTopRightY(),
                        preferences.getPanelBackgroundImageUvBottomLeftX(),
                        preferences.getPanelBackgroundImageUvBottomLeftY()
                )
        );
    }

    private void loadBackgroundImageAsync(
            PreviewContent target,
            long generation) {

        String uriString = preferences.getPanelBackgroundImageUri();

        if (uriString == null || uriString.trim().isEmpty()) {
            return;
        }

        Thread decoder = new Thread(
                () -> {
                    Bitmap bitmap = decodeBitmap(uriString);

                    if (bitmap == null) {
                        return;
                    }

                    post(
                            () -> {
                                if (generation != renderGeneration.get()) {
                                    return;
                                }

                                String current =
                                        preferences.getPanelBackgroundImageUri();

                                if (!uriString.equals(current)) {
                                    return;
                                }

                                applyPanelBackground(target, bitmap);
                            }
                    );
                },
                "PanOSP-preview-background"
        );

        decoder.start();
    }

    @android.annotation.SuppressLint("WrongThread")
    private Bitmap decodeBitmap(String uriString) {
        try {
            ImageDecoder.Source source =
                    ImageDecoder.createSource(
                            getContext().getContentResolver(),
                            Uri.parse(uriString)
                    );

            final int maxDimension =
                    Math.max(
                            getResources().getDisplayMetrics().widthPixels,
                            getResources().getDisplayMetrics().heightPixels
                    );

            return ImageDecoder.decodeBitmap(
                    source,
                    (decoder, info, sourceInfo) -> {
                        Size size = info.getSize();
                        int width = size.getWidth();
                        int height = size.getHeight();
                        int largest = Math.max(width, height);

                        if (largest > maxDimension && largest > 0) {
                            float scale =
                                    (float) maxDimension / largest;

                            decoder.setTargetSize(
                                    Math.max(1, Math.round(width * scale)),
                                    Math.max(1, Math.round(height * scale))
                            );
                        }

                        decoder.setAllocator(
                                ImageDecoder.ALLOCATOR_SOFTWARE
                        );
                    }
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private int tileLabelRes(String spec) {
        switch (spec) {
            case "wifi":
                return R.string.tile_wifi;
            case "data":
                return R.string.tile_mobile_data;
            case "bluetooth":
                return R.string.tile_bluetooth;
            case "flashlight":
                return R.string.tile_flashlight;
            case "rotation":
                return R.string.tile_rotation;
            case "location":
                return R.string.tile_location;
            case "hotspot":
                return R.string.tile_hotspot;
            case "airplane":
                return R.string.tile_airplane;
            case "dnd":
                return R.string.tile_dnd;
            case "battery":
                return R.string.tile_battery_saver;
            case "nfc":
                return R.string.tile_nfc;
            case "dark":
                return R.string.tile_dark_mode;
            case "silent":
                return R.string.tile_silent;
            case "vibrate":
                return R.string.tile_vibrate;
            case "reading":
                return R.string.tile_reading_mode;
            case "screenshot":
                return R.string.tile_screenshot;
            case "camera":
                return R.string.tile_camera;
            default:
                return 0;
        }
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    private static final class PreviewTile extends PanTile {

        private final TileStatus status;

        PreviewTile(
                Context context,
                String spec,
                int labelRes,
                TileStatus status) {

            super(context, spec, labelRes);
            this.status = status;
        }

        @Override
        public TileStatus getStatus() {
            return status;
        }

        @Override
        public void handleClick() {
            // Intentionally inert.
        }

        @Override
        public boolean handleLongClick() {
            return false;
        }
    }

    /**
     * Measures its child in a wider virtual canvas, then scales the complete
     * result back to the real screen width. This keeps the exact PanOSP
     * proportions instead of resizing each individual component separately.
     */
    private static final class ScaledPreviewFrame extends FrameLayout {

        private final float scale;
        private final int minimumVisibleHeight;
        private int contentWidth;
        private int contentHeight;
        private float drawOffsetX;

        ScaledPreviewFrame(
                Context context,
                float scale,
                int minimumVisibleHeight) {

            super(context);
            this.scale = scale;
            this.minimumVisibleHeight = minimumVisibleHeight;
            setWillNotDraw(false);
            setClipChildren(true);
        }

        @Override
        protected void onMeasure(
                int widthMeasureSpec,
                int heightMeasureSpec) {

            int visibleWidth =
                    MeasureSpec.getSize(widthMeasureSpec);

            /*
             * Faithful preview:
             * lay out the panel at the real display width, exactly like the
             * real shade, then scale the complete result uniformly.
             */
            contentWidth =
                    getResources()
                            .getDisplayMetrics()
                            .widthPixels;

            int childWidthSpec =
                    MeasureSpec.makeMeasureSpec(
                            contentWidth,
                            MeasureSpec.EXACTLY
                    );

            int childHeightSpec =
                    MeasureSpec.makeMeasureSpec(
                            0,
                            MeasureSpec.UNSPECIFIED
                    );

            contentHeight = 0;

            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                child.measure(childWidthSpec, childHeightSpec);
                contentHeight =
                        Math.max(
                                contentHeight,
                                child.getMeasuredHeight()
                        );
            }

            int scaledContentWidth =
                    Math.round(contentWidth * scale);

            int visibleHeight =
                    Math.max(
                            minimumVisibleHeight,
                            Math.round(contentHeight * scale)
                    );

            drawOffsetX =
                    Math.max(
                            0f,
                            (visibleWidth - scaledContentWidth) / 2f
                    );

            setMeasuredDimension(
                    resolveSize(visibleWidth, widthMeasureSpec),
                    resolveSize(visibleHeight, heightMeasureSpec)
            );
        }

        @Override
        protected void onLayout(
                boolean changed,
                int left,
                int top,
                int right,
                int bottom) {

            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                child.layout(
                        0,
                        0,
                        contentWidth,
                        child.getMeasuredHeight()
                );
            }
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            int save = canvas.save();

            canvas.translate(drawOffsetX, 0f);
            canvas.scale(scale, scale);

            super.dispatchDraw(canvas);

            canvas.restoreToCount(save);
        }
    }

}
