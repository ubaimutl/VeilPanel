// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.colorpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

/** Saturation increases to the right; value increases upwards. Colors are opaque. */
public final class SvColorPicker extends View {
    public interface OnColorChangeListener {
        void onColorChanged(int color, boolean fromUser);
    }

    private final float[] selection = {0f, 1f, 1f};
    private final Paint plane = new Paint();
    private final Paint cursor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private OnColorChangeListener listener;
    private int pointer = MotionEvent.INVALID_POINTER_ID;
    private ViewParent gestureParent;
    private boolean shaderDirty = true;

    public SvColorPicker(Context context) { this(context, null); }
    public SvColorPicker(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public SvColorPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClickable(true);
        // Equal-type shader composition requires software rendering before Android P.
        if (Build.VERSION.SDK_INT < 28) setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        cursor.setStyle(Paint.Style.STROKE);
    }

    public int getColor() { return Color.HSVToColor(selection); }
    public float getHue() { return selection[0]; }
    public float getSaturation() { return selection[1]; }
    public float getValue() { return selection[2]; }

    public void setOnColorChangeListener(OnColorChangeListener next) { listener = next; }

    /** Sets RGB, ignoring alpha. Notifies only when the resulting RGB changes. */
    public void setColor(int color) {
        int before = getColor();
        Color.colorToHSV(color, selection);
        shaderDirty = true;
        publish(before, false);
    }

    /** Clamps finite degrees to [0, 360]. NaN and infinities are rejected. */
    public void setHue(float degrees) {
        if (Float.isNaN(degrees) || Float.isInfinite(degrees)) {
            throw new IllegalArgumentException("Hue must be finite");
        }
        int before = getColor();
        selection[0] = Math.max(0f, Math.min(360f, degrees));
        shaderDirty = true;
        publish(before, false);
    }

    private void publish(int before, boolean fromUser) {
        invalidate();
        if (listener != null && before != getColor()) listener.onColorChanged(getColor(), fromUser);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        shaderDirty = true;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() < 2 || getHeight() < 2) return;
        if (shaderDirty) {
            int tint = Color.HSVToColor(new float[]{selection[0], 1f, 1f});
            Shader horizontal = new LinearGradient(0, 0, getWidth() - 1f, 0,
                    Color.WHITE, tint, Shader.TileMode.CLAMP);
            Shader vertical = new LinearGradient(0, 0, 0, getHeight() - 1f,
                    Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP);
            plane.setShader(new ComposeShader(horizontal, vertical, PorterDuff.Mode.MULTIPLY));
            shaderDirty = false;
        }
        canvas.drawRect(0, 0, getWidth(), getHeight(), plane);
        float density = getResources().getDisplayMetrics().density;
        float x = selection[1] * (getWidth() - 1);
        float y = (1f - selection[2]) * (getHeight() - 1);
        cursor.setColor(Color.BLACK);
        cursor.setStrokeWidth(3f * density);
        canvas.drawCircle(x, y, 6f * density, cursor);
        cursor.setColor(Color.WHITE);
        cursor.setStrokeWidth(density);
        canvas.drawCircle(x, y, 6f * density, cursor);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || getWidth() < 2 || getHeight() < 2) {
            endGesture();
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                endGesture();
                pointer = event.getPointerId(0);
                gestureParent = getParent();
                if (gestureParent != null) gestureParent.requestDisallowInterceptTouchEvent(true);
                setPressed(true);
                select(event, 0);
                return true;
            case MotionEvent.ACTION_MOVE:
                int index = event.findPointerIndex(pointer);
                if (index < 0) { endGesture(); return false; }
                select(event, index);
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerId(event.getActionIndex()) == pointer) endGesture();
                return true;
            case MotionEvent.ACTION_UP:
                boolean handled = pointer != MotionEvent.INVALID_POINTER_ID;
                int last = event.findPointerIndex(pointer);
                if (last >= 0) select(event, last);
                endGesture();
                if (handled) performClick();
                return handled;
            case MotionEvent.ACTION_CANCEL:
                endGesture();
                return true;
            default:
                return pointer != MotionEvent.INVALID_POINTER_ID;
        }
    }

    private void select(MotionEvent event, int index) {
        int before = getColor();
        selection[1] = Math.max(0f, Math.min(1f, event.getX(index) / (getWidth() - 1f)));
        selection[2] = 1f - Math.max(0f, Math.min(1f, event.getY(index) / (getHeight() - 1f)));
        publish(before, true);
    }

    private void endGesture() {
        pointer = MotionEvent.INVALID_POINTER_ID;
        setPressed(false);
        if (gestureParent != null) gestureParent.requestDisallowInterceptTouchEvent(false);
        gestureParent = null;
    }

    @Override public void setEnabled(boolean enabled) {
        if (!enabled) endGesture();
        super.setEnabled(enabled);
    }

    @Override protected void onDetachedFromWindow() {
        endGesture();
        super.onDetachedFromWindow();
    }

    @Override public boolean performClick() { super.performClick(); return true; }
}
