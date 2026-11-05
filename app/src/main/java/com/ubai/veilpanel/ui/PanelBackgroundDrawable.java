package com.ubai.veilpanel.ui;

import fr.gibapps.ui.background.GibBackgroundDrawable;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Rounded panel background composed of the global Gib background and an
 * optional center-cropped image with its own independent opacity.
 */
public final class PanelBackgroundDrawable extends Drawable {

    private final Paint imagePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private final RectF drawRect = new RectF();
    private final Matrix shaderMatrix = new Matrix();

    private final Bitmap bitmap;
    private final BitmapShader bitmapShader;
    private final float configuredRadius;
    private final GibBackgroundDrawable gibBackground;

    private final float uvTopLeftX;
    private final float uvTopLeftY;
    private final float uvTopRightX;
    private final float uvTopRightY;
    private final float uvBottomLeftX;
    private final float uvBottomLeftY;

    public PanelBackgroundDrawable(
            int backgroundType,
            int backgroundColor1,
            int backgroundColor2,
            int backgroundDirection,
            int backgroundAlpha,
            Bitmap bitmap,
            int imageOpacityPercent,
            float cornerRadius,
            float uvTopLeftX,
            float uvTopLeftY,
            float uvTopRightX,
            float uvTopRightY,
            float uvBottomLeftX,
            float uvBottomLeftY) {
        this.bitmap = bitmap;
        this.configuredRadius = Math.max(0f, cornerRadius);
        this.uvTopLeftX = uvTopLeftX;
        this.uvTopLeftY = uvTopLeftY;
        this.uvTopRightX = uvTopRightX;
        this.uvTopRightY = uvTopRightY;
        this.uvBottomLeftX = uvBottomLeftX;
        this.uvBottomLeftY = uvBottomLeftY;
        this.gibBackground = new GibBackgroundDrawable(
                backgroundType,
                backgroundColor1,
                backgroundColor2,
                backgroundDirection,
                this.configuredRadius);
        this.gibBackground.setAlpha(backgroundAlpha);
        if (bitmap != null) {
            bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            imagePaint.setShader(bitmapShader);
            imagePaint.setAlpha(Math.round(
                    255f * Math.max(0, Math.min(100, imageOpacityPercent)) / 100f));
        } else {
            bitmapShader = null;
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        drawRect.set(bounds);
        gibBackground.setBounds(bounds);

        if (bitmapShader == null
                || bitmap == null
                || bitmap.getWidth() <= 0
                || bitmap.getHeight() <= 0
                || bounds.width() <= 0
                || bounds.height() <= 0) {
            return;
        }

        float[] source = {
                uvTopLeftX * bitmap.getWidth(),
                uvTopLeftY * bitmap.getHeight(),

                uvTopRightX * bitmap.getWidth(),
                uvTopRightY * bitmap.getHeight(),

                uvBottomLeftX * bitmap.getWidth(),
                uvBottomLeftY * bitmap.getHeight()
        };

        float[] destination = {
                bounds.left,
                bounds.top,

                bounds.right,
                bounds.top,

                bounds.left,
                bounds.bottom
        };

        shaderMatrix.reset();
        shaderMatrix.setPolyToPoly(
                source,
                0,
                destination,
                0,
                3
        );
        bitmapShader.setLocalMatrix(shaderMatrix);
    }

    @Override
    public void draw(Canvas canvas) {
        float radius = Math.min(
                configuredRadius,
                Math.min(drawRect.width(), drawRect.height()) / 2f
        );

        gibBackground.draw(canvas);

        if (bitmapShader != null && imagePaint.getAlpha() > 0) {
            canvas.drawRoundRect(
                    drawRect,
                    radius,
                    radius,
                    imagePaint
            );
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // Opacity is controlled independently by PanOSP preferences.
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        imagePaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
