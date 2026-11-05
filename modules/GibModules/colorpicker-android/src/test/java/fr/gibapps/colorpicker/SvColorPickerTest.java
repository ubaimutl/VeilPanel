// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.colorpicker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SvColorPickerTest {
    private SvColorPicker picker;
    private Parent parent;
    private static class Parent extends FrameLayout {
        boolean blocked;
        Parent() { super(RuntimeEnvironment.getApplication()); }
        @Override public void requestDisallowInterceptTouchEvent(boolean disallow) {
            blocked = disallow;
            super.requestDisallowInterceptTouchEvent(disallow);
        }
    }
    @Before public void create() {
        parent = new Parent();
        picker = new SvColorPicker(parent.getContext());
        parent.addView(picker);
        picker.layout(0, 0, 101, 101);
    }
    private void touch(int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(0, 0, action, x, y, 0);
        try { picker.onTouchEvent(event); } finally { event.recycle(); }
    }
    private Bitmap render() {
        Bitmap bitmap = Bitmap.createBitmap(picker.getWidth(), picker.getHeight(), Bitmap.Config.ARGB_8888);
        picker.draw(new Canvas(bitmap));
        return bitmap;
    }
    @Test @Config(sdk = {23, 28}) @GraphicsMode(GraphicsMode.Mode.LEGACY)
    public void rendererSelectsCompatibleLayer() {
        assertEquals(android.os.Build.VERSION.SDK_INT < 28
                ? android.view.View.LAYER_TYPE_SOFTWARE : android.view.View.LAYER_TYPE_NONE,
                picker.getLayerType());
    }
    @Test public void rgbRoundTripIgnoresAlpha() {
        for (int color : new int[]{Color.RED, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK, 0x12487fb2}) {
            picker.setColor(color);
            assertEquals(color | 0xff000000, picker.getColor());
        }
    }
    @Test public void hueClampsAndWrapsAt360() {
        picker.setHue(-20); assertEquals(0f, picker.getHue(), 0f);
        picker.setHue(500); assertEquals(360f, picker.getHue(), 0f);
        assertEquals(Color.RED, picker.getColor());
    }
    @Test public void invalidHueRejectedWithoutMutation() {
        picker.setHue(120);
        for (float value : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> picker.setHue(value));
            assertEquals(Color.GREEN, picker.getColor());
        }
    }
    @Test public void huePreservesSaturationAndValue() {
        touch(MotionEvent.ACTION_DOWN, 50, 25);
        picker.setHue(240);
        assertEquals(.5f, picker.getSaturation(), .0001f);
        assertEquals(.75f, picker.getValue(), .0001f);
    }
    @Test public void touchCornersAndOutOfBounds() {
        touch(MotionEvent.ACTION_DOWN, -40, -30); assertEquals(Color.WHITE, picker.getColor());
        touch(MotionEvent.ACTION_MOVE, 200, -30); assertEquals(Color.RED, picker.getColor());
        touch(MotionEvent.ACTION_MOVE, 200, 200); assertEquals(Color.BLACK, picker.getColor());
        assertEquals(1f, picker.getSaturation(), 0f);
        assertEquals(0f, picker.getValue(), 0f);
    }
    @Test public void gestureReleasesParentOnUpAndCancel() {
        touch(MotionEvent.ACTION_DOWN, 50, 50); assertTrue(parent.blocked);
        touch(MotionEvent.ACTION_UP, 50, 50); assertFalse(parent.blocked); assertFalse(picker.isPressed());
        touch(MotionEvent.ACTION_DOWN, 30, 30); assertTrue(parent.blocked);
        int before = picker.getColor();
        touch(MotionEvent.ACTION_CANCEL, 90, 90);
        assertFalse(parent.blocked); assertEquals(before, picker.getColor());
    }
    @Test public void disablingReleasesAndPreventsChanges() {
        touch(MotionEvent.ACTION_DOWN, 20, 20);
        picker.setEnabled(false); assertFalse(parent.blocked);
        int before = picker.getColor();
        touch(MotionEvent.ACTION_DOWN, 99, 99); assertEquals(before, picker.getColor());
    }
    @Test public void zeroSizeDoesNotConsumeGesture() {
        picker.layout(0, 0, 0, 0);
        touch(MotionEvent.ACTION_DOWN, 0, 0);
        assertFalse(parent.blocked); assertEquals(Color.RED, picker.getColor());
    }
    @Test public void listenerDistinguishesOriginAndSkipsDuplicateRgb() {
        java.util.List<Boolean> origins = new java.util.ArrayList<>();
        picker.setOnColorChangeListener((color, user) -> origins.add(user));
        picker.setColor(Color.BLUE);
        picker.setColor(Color.BLUE);
        touch(MotionEvent.ACTION_DOWN, 0, 0);
        touch(MotionEvent.ACTION_UP, 0, 0);
        assertEquals(java.util.Arrays.asList(false, true), origins);
        picker.setOnColorChangeListener(null);
        picker.setColor(Color.RED);
        assertEquals(2, origins.size());
    }
    @Test public void resizePreservesSelectionAndUsesNewBounds() {
        touch(MotionEvent.ACTION_DOWN, 25, 75);
        int before = picker.getColor();
        picker.layout(0, 0, 201, 301);
        assertEquals(before, picker.getColor());
        touch(MotionEvent.ACTION_MOVE, 100, 150);
        assertEquals(.5f, picker.getSaturation(), .0001f);
        assertEquals(.5f, picker.getValue(), .0001f);
    }
    @Test public void renderedPlaneChangesWithHueAndResize() {
        picker.setColor(Color.BLACK); // cursor away from sampled top area
        Bitmap first = render();
        int red = first.getPixel(90, 10);
        assertTrue(Color.red(red) > Color.green(red) + 150);
        picker.setHue(120);
        Bitmap next = render();
        int green = next.getPixel(90, 10);
        assertTrue(Color.green(green) > Color.red(green) + 150);
        picker.layout(0, 0, 201, 201);
        Bitmap larger = render();
        int resized = larger.getPixel(180, 20);
        assertTrue(Math.abs(Color.green(resized) - Color.green(green)) < 4);
        assertTrue(Color.red(larger.getPixel(10, 10)) > 220);
        assertTrue(Color.green(larger.getPixel(180, 195)) < 10);
        first.recycle(); next.recycle(); larger.recycle();
    }
    @Test public void repeatedDrawingIsStable() {
        Bitmap first = render(); Bitmap second = render();
        assertEquals(first.getPixel(40, 60), second.getPixel(40, 60));
        first.recycle(); second.recycle();
    }
}
