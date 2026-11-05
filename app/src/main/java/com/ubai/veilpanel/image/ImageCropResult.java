package com.ubai.veilpanel.image;

/**
 * Immutable description of an image crop.
 *
 * The source image is never stretched anisotropically.
 * Allowed transformations are:
 * translation + uniform scale + rotation.
 *
 * Crop coordinates are normalized relative to the editing viewport.
 */
public final class ImageCropResult {

    public final String sourceUri;

    /** Translation normalized relative to viewport width/height. */
    public final float translationX;
    public final float translationY;

    /** Uniform image scale only. */
    public final float scale;

    /** Free rotation in degrees. */
    public final float rotationDegrees;

    /** Crop rectangle center, normalized to viewport coordinates [0..1]. */
    public final float cropCenterX;
    public final float cropCenterY;

    /** Crop rectangle size, normalized to viewport width/height. */
    public final float cropWidth;
    public final float cropHeight;

    /** Required physical output aspect ratio: width / height. */
    public final float targetAspectRatio;

    /**
     * Crop corners mapped into normalized source-image coordinates.
     * Order: top-left, top-right, bottom-left, bottom-right.
     */
    public final float uvTopLeftX;
    public final float uvTopLeftY;
    public final float uvTopRightX;
    public final float uvTopRightY;
    public final float uvBottomLeftX;
    public final float uvBottomLeftY;
    public final float uvBottomRightX;
    public final float uvBottomRightY;

    public ImageCropResult(
            String sourceUri,
            float translationX,
            float translationY,
            float scale,
            float rotationDegrees,
            float cropCenterX,
            float cropCenterY,
            float cropWidth,
            float cropHeight,
            float targetAspectRatio,
            float uvTopLeftX,
            float uvTopLeftY,
            float uvTopRightX,
            float uvTopRightY,
            float uvBottomLeftX,
            float uvBottomLeftY,
            float uvBottomRightX,
            float uvBottomRightY
    ) {
        if (sourceUri == null || sourceUri.trim().isEmpty()) {
            throw new IllegalArgumentException("sourceUri must not be empty");
        }

        if (scale <= 0f) {
            throw new IllegalArgumentException("scale must be > 0");
        }

        if (cropWidth <= 0f || cropHeight <= 0f) {
            throw new IllegalArgumentException("crop dimensions must be > 0");
        }

        if (targetAspectRatio <= 0f) {
            throw new IllegalArgumentException("targetAspectRatio must be > 0");
        }

        this.sourceUri = sourceUri;
        this.translationX = translationX;
        this.translationY = translationY;
        this.scale = scale;
        this.rotationDegrees = rotationDegrees;
        this.cropCenterX = cropCenterX;
        this.cropCenterY = cropCenterY;
        this.cropWidth = cropWidth;
        this.cropHeight = cropHeight;
        this.targetAspectRatio = targetAspectRatio;

        this.uvTopLeftX = uvTopLeftX;
        this.uvTopLeftY = uvTopLeftY;
        this.uvTopRightX = uvTopRightX;
        this.uvTopRightY = uvTopRightY;
        this.uvBottomLeftX = uvBottomLeftX;
        this.uvBottomLeftY = uvBottomLeftY;
        this.uvBottomRightX = uvBottomRightX;
        this.uvBottomRightY = uvBottomRightY;
    }
}
