package com.ubai.veilpanel.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.InputStream;

/**
 * Reusable crop view.
 *
 * Image transformations are strictly limited to:
 * translation + uniform scale + rotation.
 *
 * No anisotropic scaling is allowed.
 */
public final class ImageCropView extends View {

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 10f;

    private static final int HANDLE_NONE = 0;
    private static final int HANDLE_TOP_LEFT = 1;
    private static final int HANDLE_TOP_RIGHT = 2;
    private static final int HANDLE_BOTTOM_LEFT = 3;
    private static final int HANDLE_BOTTOM_RIGHT = 4;

    private final Paint imagePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private final Paint cropPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint shadePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint handlePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Matrix imageMatrix = new Matrix();
    private final RectF cropRect = new RectF();

    private final ScaleGestureDetector scaleDetector;

    private Bitmap bitmap;
    private String sourceUri;

    private float targetAspectRatio = 1f;

    private float translationX;
    private float translationY;
    private float scale = 1f;
    private float rotationDegrees;

    private float lastTouchX;
    private float lastTouchY;

    private float lastTwoFingerX;
    private float lastTwoFingerY;
    private float lastTwoFingerAngle;

    private boolean twoFingerGesture;

    private int activeHandle = HANDLE_NONE;
    private float handleRadius;
    private float handleTouchRadius;
    private float minimumCropWidth;

    public ImageCropView(Context context) {
        super(context);

        cropPaint.setStyle(Paint.Style.STROKE);
        cropPaint.setStrokeWidth(dp(2));
        cropPaint.setColor(0xffffffff);

        shadePaint.setStyle(Paint.Style.FILL);
        shadePaint.setColor(0x99000000);

        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setColor(0xffffffff);

        handleRadius = dp(7);
        handleTouchRadius = dp(28);
        minimumCropWidth = dp(100);

        scaleDetector =
                new ScaleGestureDetector(
                        context,
                        new ScaleGestureDetector.SimpleOnScaleGestureListener() {

                            @Override
                            public boolean onScale(ScaleGestureDetector detector) {
                                float factor = detector.getScaleFactor();

                                if (!Float.isFinite(factor) || factor <= 0f) {
                                    return false;
                                }

                                float oldScale = scale;
                                float newScale = clamp(
                                        oldScale * factor,
                                        MIN_SCALE,
                                        MAX_SCALE
                                );

                                float effectiveFactor = newScale / oldScale;

                                float focusX = detector.getFocusX();
                                float focusY = detector.getFocusY();

                                float imageCenterX =
                                        cropRect.centerX() + translationX;

                                float imageCenterY =
                                        cropRect.centerY() + translationY;

                                imageCenterX =
                                        focusX
                                                + (imageCenterX - focusX)
                                                * effectiveFactor;

                                imageCenterY =
                                        focusY
                                                + (imageCenterY - focusY)
                                                * effectiveFactor;

                                scale = newScale;

                                translationX =
                                        imageCenterX - cropRect.centerX();

                                translationY =
                                        imageCenterY - cropRect.centerY();

                                updateImageMatrix();
                                invalidate();
                                return true;
                            }
                        }
                );
    }

    public void setSourceUri(String uriString) {
        if (uriString == null || uriString.trim().isEmpty()) {
            throw new IllegalArgumentException("uriString must not be empty");
        }

        sourceUri = uriString;

        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        try (InputStream input = getContext()
                .getContentResolver()
                .openInputStream(Uri.parse(uriString))) {

            bitmap = BitmapFactory.decodeStream(input);

            if (bitmap == null) {
                throw new IllegalArgumentException("Unable to decode image");
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to load image", e);
        }

        resetTransform();
        invalidate();
    }

    public void setTargetAspectRatio(float ratio) {
        if (ratio <= 0f) {
            throw new IllegalArgumentException("ratio must be > 0");
        }

        targetAspectRatio = ratio;
        updateCropRect();
        invalidate();
    }

    public void rotateLeft90() {
        rotationDegrees = normalizeDegrees(rotationDegrees - 90f);
        updateImageMatrix();
        invalidate();
    }

    public void rotateRight90() {
        rotationDegrees = normalizeDegrees(rotationDegrees + 90f);
        updateImageMatrix();
        invalidate();
    }

    private void resetTransform() {
        translationX = 0f;
        translationY = 0f;
        scale = 1f;
        rotationDegrees = 0f;

        updateCropRect();
        updateImageMatrix();
    }

    private void updateCropRect() {
        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        float availableWidth = width * 0.86f;
        float availableHeight = height * 0.72f;

        float cropWidth = availableWidth;
        float cropHeight = cropWidth / targetAspectRatio;

        if (cropHeight > availableHeight) {
            cropHeight = availableHeight;
            cropWidth = cropHeight * targetAspectRatio;
        }

        float cx = width * 0.5f;
        float cy = height * 0.5f;

        cropRect.set(
                cx - cropWidth * 0.5f,
                cy - cropHeight * 0.5f,
                cx + cropWidth * 0.5f,
                cy + cropHeight * 0.5f
        );

        updateImageMatrix();
    }

    private void updateImageMatrix() {
        if (bitmap == null
                || getWidth() <= 0
                || getHeight() <= 0
                || cropRect.isEmpty()) {
            return;
        }

        imageMatrix.reset();

        float bitmapWidth = bitmap.getWidth();
        float bitmapHeight = bitmap.getHeight();

        float baseScale = Math.max(
                cropRect.width() / bitmapWidth,
                cropRect.height() / bitmapHeight
        );

        float finalScale = baseScale * scale;

        imageMatrix.postTranslate(
                -bitmapWidth * 0.5f,
                -bitmapHeight * 0.5f
        );

        imageMatrix.postScale(finalScale, finalScale);
        imageMatrix.postRotate(rotationDegrees);

        imageMatrix.postTranslate(
                cropRect.centerX() + translationX,
                cropRect.centerY() + translationY
        );
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateCropRect();
        updateImageMatrix();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bitmap != null) {
            canvas.drawBitmap(bitmap, imageMatrix, imagePaint);
        }

        drawCropOverlay(canvas);
    }

    private void drawCropOverlay(Canvas canvas) {
        float left = cropRect.left;
        float top = cropRect.top;
        float right = cropRect.right;
        float bottom = cropRect.bottom;

        canvas.drawRect(0, 0, getWidth(), top, shadePaint);
        canvas.drawRect(0, bottom, getWidth(), getHeight(), shadePaint);
        canvas.drawRect(0, top, left, bottom, shadePaint);
        canvas.drawRect(right, top, getWidth(), bottom, shadePaint);

        canvas.drawRect(cropRect, cropPaint);

        drawHandle(canvas, cropRect.left, cropRect.top);
        drawHandle(canvas, cropRect.right, cropRect.top);
        drawHandle(canvas, cropRect.left, cropRect.bottom);
        drawHandle(canvas, cropRect.right, cropRect.bottom);
    }

    private void drawHandle(Canvas canvas, float x, float y) {
        canvas.drawCircle(x, y, handleRadius, handlePaint);
    }

    private int findHandle(float x, float y) {
        if (distance(x, y, cropRect.left, cropRect.top) <= handleTouchRadius) {
            return HANDLE_TOP_LEFT;
        }

        if (distance(x, y, cropRect.right, cropRect.top) <= handleTouchRadius) {
            return HANDLE_TOP_RIGHT;
        }

        if (distance(x, y, cropRect.left, cropRect.bottom) <= handleTouchRadius) {
            return HANDLE_BOTTOM_LEFT;
        }

        if (distance(x, y, cropRect.right, cropRect.bottom) <= handleTouchRadius) {
            return HANDLE_BOTTOM_RIGHT;
        }

        return HANDLE_NONE;
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (bitmap == null) {
            return false;
        }

        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);

                twoFingerGesture = false;

                lastTouchX = event.getX();
                lastTouchY = event.getY();

                activeHandle = findHandle(lastTouchX, lastTouchY);

                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() >= 2) {
                    twoFingerGesture = true;

                    lastTwoFingerX = midpointX(event);
                    lastTwoFingerY = midpointY(event);
                    lastTwoFingerAngle = pointerAngle(event);
                }

                return true;

            case MotionEvent.ACTION_MOVE:
                if (activeHandle != HANDLE_NONE && event.getPointerCount() == 1) {
                    resizeCropFromHandle(
                            activeHandle,
                            event.getX(),
                            event.getY()
                    );

                    updateImageMatrix();
                    invalidate();

                } else if (event.getPointerCount() >= 2) {
                    float midpointX = midpointX(event);
                    float midpointY = midpointY(event);

                    translationX += midpointX - lastTwoFingerX;
                    translationY += midpointY - lastTwoFingerY;

                    float angle = pointerAngle(event);
                    float deltaAngle =
                            normalizeDeltaDegrees(
                                    angle - lastTwoFingerAngle
                            );

                    rotateAround(
                            midpointX,
                            midpointY,
                            deltaAngle
                    );

                    lastTwoFingerX = midpointX;
                    lastTwoFingerY = midpointY;
                    lastTwoFingerAngle = angle;

                    updateImageMatrix();
                    invalidate();

                } else if (!twoFingerGesture) {
                    float x = event.getX();
                    float y = event.getY();

                    translationX += x - lastTouchX;
                    translationY += y - lastTouchY;

                    lastTouchX = x;
                    lastTouchY = y;

                    updateImageMatrix();
                    invalidate();
                }

                return true;

            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() <= 2) {
                    twoFingerGesture = false;
                }

                return true;

            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                twoFingerGesture = false;
                activeHandle = HANDLE_NONE;
                performClick();
                return true;

            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                twoFingerGesture = false;
                activeHandle = HANDLE_NONE;
                return true;

            default:
                return true;
        }
    }

    private void resizeCropFromHandle(
            int handle,
            float touchX,
            float touchY
    ) {
        float anchorX;
        float anchorY;
        float directionX;
        float directionY;

        switch (handle) {
            case HANDLE_TOP_LEFT:
                anchorX = cropRect.right;
                anchorY = cropRect.bottom;
                directionX = -1f;
                directionY = -1f;
                break;

            case HANDLE_TOP_RIGHT:
                anchorX = cropRect.left;
                anchorY = cropRect.bottom;
                directionX = 1f;
                directionY = -1f;
                break;

            case HANDLE_BOTTOM_LEFT:
                anchorX = cropRect.right;
                anchorY = cropRect.top;
                directionX = -1f;
                directionY = 1f;
                break;

            case HANDLE_BOTTOM_RIGHT:
                anchorX = cropRect.left;
                anchorY = cropRect.top;
                directionX = 1f;
                directionY = 1f;
                break;

            default:
                return;
        }

        float requestedWidth =
                Math.abs(touchX - anchorX);

        float requestedHeight =
                Math.abs(touchY - anchorY);

        float widthFromHeight =
                requestedHeight * targetAspectRatio;

        float newWidth;

        if (requestedWidth / targetAspectRatio > requestedHeight) {
            newWidth = requestedWidth;
        } else {
            newWidth = widthFromHeight;
        }

        newWidth = Math.max(
                minimumCropWidth,
                newWidth
        );

        float newHeight =
                newWidth / targetAspectRatio;

        float maxWidthByX =
                directionX < 0f
                        ? anchorX
                        : getWidth() - anchorX;

        float maxHeightByY =
                directionY < 0f
                        ? anchorY
                        : getHeight() - anchorY;

        float maxWidth =
                Math.min(
                        maxWidthByX,
                        maxHeightByY * targetAspectRatio
                );

        newWidth =
                Math.min(
                        newWidth,
                        maxWidth
                );

        newHeight =
                newWidth / targetAspectRatio;

        float movingX =
                anchorX + directionX * newWidth;

        float movingY =
                anchorY + directionY * newHeight;

        cropRect.set(
                Math.min(anchorX, movingX),
                Math.min(anchorY, movingY),
                Math.max(anchorX, movingX),
                Math.max(anchorY, movingY)
        );
    }


    private void rotateAround(
            float focusX,
            float focusY,
            float deltaDegrees
    ) {
        if (Math.abs(deltaDegrees) < 0.001f) {
            return;
        }

        float centerX =
                cropRect.centerX() + translationX;

        float centerY =
                cropRect.centerY() + translationY;

        double radians =
                Math.toRadians(deltaDegrees);

        float dx = centerX - focusX;
        float dy = centerY - focusY;

        float rotatedX =
                (float) (
                        dx * Math.cos(radians)
                                - dy * Math.sin(radians)
                );

        float rotatedY =
                (float) (
                        dx * Math.sin(radians)
                                + dy * Math.cos(radians)
                );

        centerX = focusX + rotatedX;
        centerY = focusY + rotatedY;

        translationX =
                centerX - cropRect.centerX();

        translationY =
                centerY - cropRect.centerY();

        rotationDegrees =
                normalizeDegrees(
                        rotationDegrees + deltaDegrees
                );
    }

    private static float midpointX(MotionEvent event) {
        return (event.getX(0) + event.getX(1)) * 0.5f;
    }

    private static float midpointY(MotionEvent event) {
        return (event.getY(0) + event.getY(1)) * 0.5f;
    }

    private static float pointerAngle(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);

        return (float) Math.toDegrees(
                Math.atan2(dy, dx)
        );
    }

    private static float normalizeDegrees(float degrees) {
        while (degrees > 180f) {
            degrees -= 360f;
        }

        while (degrees < -180f) {
            degrees += 360f;
        }

        return degrees;
    }

    private static float normalizeDeltaDegrees(float degrees) {
        return normalizeDegrees(degrees);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    public ImageCropResult getCropResult() {
        if (sourceUri == null || bitmap == null || cropRect.isEmpty()) {
            return null;
        }

        float width = getWidth();
        float height = getHeight();

        Matrix inverse = new Matrix();

        if (!imageMatrix.invert(inverse)) {
            return null;
        }

        float[] points = {
                cropRect.left, cropRect.top,
                cropRect.right, cropRect.top,
                cropRect.left, cropRect.bottom,
                cropRect.right, cropRect.bottom
        };

        inverse.mapPoints(points);

        float bitmapWidth = bitmap.getWidth();
        float bitmapHeight = bitmap.getHeight();

        return new ImageCropResult(
                sourceUri,
                translationX / width,
                translationY / height,
                scale,
                rotationDegrees,
                cropRect.centerX() / width,
                cropRect.centerY() / height,
                cropRect.width() / width,
                cropRect.height() / height,
                targetAspectRatio,

                points[0] / bitmapWidth,
                points[1] / bitmapHeight,

                points[2] / bitmapWidth,
                points[3] / bitmapHeight,

                points[4] / bitmapWidth,
                points[5] / bitmapHeight,

                points[6] / bitmapWidth,
                points[7] / bitmapHeight
        );
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }
}
