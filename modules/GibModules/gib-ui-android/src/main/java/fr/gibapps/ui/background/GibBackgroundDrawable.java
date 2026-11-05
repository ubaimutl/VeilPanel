// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.ui.background;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** EXPERIMENTAL. Extracted from PanOSP; gradient geometry preserved, alpha applied once. */
public final class GibBackgroundDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF drawRect = new RectF();
    private final int type;
    private final int color1;
    private final int color2;
    private final int direction;
    private final float configuredRadius;
    private int alpha = 255;

    /** Corner radius is in pixels. Each owner must use its own drawable instance. */
    public GibBackgroundDrawable(BackgroundConfig config, float cornerRadius) {
        this(config.type, config.color1, config.color2, config.direction, cornerRadius);
    }

    public GibBackgroundDrawable(
            int type,
            int color1,
            int color2,
            int direction,
            float cornerRadius) {
        this.type = Math.max(0, Math.min(2, type));
        this.color1 = color1 | 0xFF000000;
        this.color2 = color2 | 0xFF000000;
        this.direction = Math.max(0, Math.min(5, direction));
        this.configuredRadius = Math.max(0f, cornerRadius);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        drawRect.set(bounds);
        updatePaint();
    }

    private void updatePaint() {
        paint.setShader(null);
        paint.setColor(withAlpha(color1));

        if (drawRect.isEmpty() || type == 0) {
            return;
        }

        // Shader colors already carry the requested alpha; Paint must not multiply it.
        paint.setAlpha(255);
        int first = withAlpha(color1);
        int second = withAlpha(color2);

        if (type == 2) {
            float cx = drawRect.centerX();
            float cy = drawRect.centerY();
            float radius = (float) Math.hypot(
                    drawRect.width() / 2f,
                    drawRect.height() / 2f
            );

            boolean inverse = direction == 5;

            paint.setShader(new RadialGradient(
                    cx,
                    cy,
                    Math.max(1f, radius),
                    inverse ? second : first,
                    inverse ? first : second,
                    Shader.TileMode.CLAMP
            ));
            return;
        }

        switch (direction) {
            case 1:
                // Left -> right.
                paint.setShader(new LinearGradient(
                        drawRect.left,
                        drawRect.top,
                        drawRect.right,
                        drawRect.top,
                        first,
                        second,
                        Shader.TileMode.CLAMP
                ));
                break;

            case 2:
                // Bottom -> top.
                paint.setShader(new LinearGradient(
                        drawRect.left,
                        drawRect.bottom,
                        drawRect.left,
                        drawRect.top,
                        first,
                        second,
                        Shader.TileMode.CLAMP
                ));
                break;

            case 3:
                // Right -> left.
                paint.setShader(new LinearGradient(
                        drawRect.right,
                        drawRect.top,
                        drawRect.left,
                        drawRect.top,
                        first,
                        second,
                        Shader.TileMode.CLAMP
                ));
                break;

            case 0:
            default:
                // Top -> bottom.
                paint.setShader(new LinearGradient(
                        drawRect.left,
                        drawRect.top,
                        drawRect.left,
                        drawRect.bottom,
                        first,
                        second,
                        Shader.TileMode.CLAMP
                ));
                break;
        }
    }

    private int withAlpha(int color) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    @Override
    public void draw(Canvas canvas) {
        float radius = Math.min(
                configuredRadius,
                Math.min(drawRect.width(), drawRect.height()) / 2f);
        canvas.drawRoundRect(drawRect, radius, radius, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
        updatePaint();
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return alpha == 255 ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }
}
