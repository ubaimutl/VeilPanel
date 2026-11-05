package com.ubai.veilpanel.image;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public final class ImageCropActivity extends Activity {

    public static final String EXTRA_SOURCE_URI = "source_uri";
    public static final String EXTRA_TARGET_ASPECT_RATIO = "target_aspect_ratio";

    public static final String EXTRA_TRANSLATION_X = "translation_x";
    public static final String EXTRA_TRANSLATION_Y = "translation_y";
    public static final String EXTRA_SCALE = "scale";
    public static final String EXTRA_ROTATION_DEGREES = "rotation_degrees";
    public static final String EXTRA_CROP_CENTER_X = "crop_center_x";
    public static final String EXTRA_CROP_CENTER_Y = "crop_center_y";
    public static final String EXTRA_CROP_WIDTH = "crop_width";
    public static final String EXTRA_CROP_HEIGHT = "crop_height";
    public static final String EXTRA_RESULT_ASPECT_RATIO = "result_aspect_ratio";

    public static final String EXTRA_UV_TOP_LEFT_X = "uv_top_left_x";
    public static final String EXTRA_UV_TOP_LEFT_Y = "uv_top_left_y";
    public static final String EXTRA_UV_TOP_RIGHT_X = "uv_top_right_x";
    public static final String EXTRA_UV_TOP_RIGHT_Y = "uv_top_right_y";
    public static final String EXTRA_UV_BOTTOM_LEFT_X = "uv_bottom_left_x";
    public static final String EXTRA_UV_BOTTOM_LEFT_Y = "uv_bottom_left_y";
    public static final String EXTRA_UV_BOTTOM_RIGHT_X = "uv_bottom_right_x";
    public static final String EXTRA_UV_BOTTOM_RIGHT_Y = "uv_bottom_right_y";

    private ImageCropView cropView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setNavigationBarContrastEnforced(false);

        String sourceUri = getIntent().getStringExtra(EXTRA_SOURCE_URI);
        float targetAspectRatio =
                getIntent().getFloatExtra(EXTRA_TARGET_ASPECT_RATIO, 0f);

        if (sourceUri == null || sourceUri.trim().isEmpty()
                || targetAspectRatio <= 0f) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        cropView = new ImageCropView(this);
        root.addView(
                cropView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        android.widget.FrameLayout screen = new android.widget.FrameLayout(this);
        screen.addView(root, new android.widget.FrameLayout.LayoutParams(-1, -1));
        android.view.View navigationBarScrim = new android.view.View(this);
        navigationBarScrim.setBackgroundColor(0xff101216);
        screen.addView(navigationBarScrim,
                new android.widget.FrameLayout.LayoutParams(-1, 0, Gravity.BOTTOM));
        controls.setOnApplyWindowInsetsListener((view, insets) -> {
            android.view.WindowInsetsController controller = view.getWindowInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(0,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            int bottom = insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom;
            ViewGroup.LayoutParams params = navigationBarScrim.getLayoutParams();
            if (params.height != bottom) {
                params.height = bottom;
                navigationBarScrim.setLayoutParams(params);
            }
            view.setPadding(
                    insets.getSystemWindowInsetLeft(),
                    0,
                    insets.getSystemWindowInsetRight(),
                    insets.getSystemWindowInsetBottom()
            );
            return insets;
        });

        Button cancelButton = new Button(this);
        styleCropAction(cancelButton);
        cancelButton.setText(com.ubai.veilpanel.R.string.crop_cancel);
        cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        Button rotateLeftButton = new Button(this);
        styleCropAction(rotateLeftButton);
        rotateLeftButton.setText("↶ 90°");
        rotateLeftButton.setOnClickListener(
                v -> cropView.rotateLeft90()
        );

        Button rotateRightButton = new Button(this);
        styleCropAction(rotateRightButton);
        rotateRightButton.setText("90° ↷");
        rotateRightButton.setOnClickListener(
                v -> cropView.rotateRight90()
        );

        Button validateButton = new Button(this);
        styleCropAction(validateButton);
        validateButton.setText(com.ubai.veilpanel.R.string.crop_validate);
        validateButton.setOnClickListener(v -> validateCrop());

        controls.addView(cancelButton);
        controls.addView(rotateLeftButton);
        controls.addView(rotateRightButton);
        controls.addView(validateButton);

        root.addView(
                controls,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(screen);
        controls.requestApplyInsets();

        cropView.setTargetAspectRatio(targetAspectRatio);
        cropView.setSourceUri(sourceUri);
    }

    private void styleCropAction(Button button) {
        float density = getResources().getDisplayMetrics().density;
        button.setAllCaps(false);
        button.setTextColor(getColor(com.ubai.veilpanel.R.color.gib_text_primary));
        button.setMinHeight(Math.round(48f * density));
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(Color.argb(22, 255, 255, 255));
        background.setCornerRadius(14f * density);
        button.setBackground(background);
        button.setStateListAnimator(null);
    }

    private void validateCrop() {
        ImageCropResult result = cropView.getCropResult();

        if (result == null) {
            return;
        }

        Intent data = new Intent();

        data.putExtra(EXTRA_SOURCE_URI, result.sourceUri);
        data.putExtra(EXTRA_TRANSLATION_X, result.translationX);
        data.putExtra(EXTRA_TRANSLATION_Y, result.translationY);
        data.putExtra(EXTRA_SCALE, result.scale);
        data.putExtra(EXTRA_ROTATION_DEGREES, result.rotationDegrees);
        data.putExtra(EXTRA_CROP_CENTER_X, result.cropCenterX);
        data.putExtra(EXTRA_CROP_CENTER_Y, result.cropCenterY);
        data.putExtra(EXTRA_CROP_WIDTH, result.cropWidth);
        data.putExtra(EXTRA_CROP_HEIGHT, result.cropHeight);
        data.putExtra(EXTRA_RESULT_ASPECT_RATIO, result.targetAspectRatio);

        data.putExtra(EXTRA_UV_TOP_LEFT_X, result.uvTopLeftX);
        data.putExtra(EXTRA_UV_TOP_LEFT_Y, result.uvTopLeftY);
        data.putExtra(EXTRA_UV_TOP_RIGHT_X, result.uvTopRightX);
        data.putExtra(EXTRA_UV_TOP_RIGHT_Y, result.uvTopRightY);
        data.putExtra(EXTRA_UV_BOTTOM_LEFT_X, result.uvBottomLeftX);
        data.putExtra(EXTRA_UV_BOTTOM_LEFT_Y, result.uvBottomLeftY);
        data.putExtra(EXTRA_UV_BOTTOM_RIGHT_X, result.uvBottomRightX);
        data.putExtra(EXTRA_UV_BOTTOM_RIGHT_Y, result.uvBottomRightY);

        setResult(RESULT_OK, data);
        finish();
    }
}
