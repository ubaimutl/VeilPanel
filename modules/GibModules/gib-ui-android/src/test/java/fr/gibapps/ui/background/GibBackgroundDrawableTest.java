// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.ui.background;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class GibBackgroundDrawableTest {
    private Bitmap render(GibBackgroundDrawable drawable, int width, int height) {
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(new Canvas(b));
        return b;
    }
    @Test public void solidFillsTheBoundsWithColorOne() {
        Bitmap b = render(new GibBackgroundDrawable(new BackgroundConfig(0, 0xff125678, Color.WHITE, 5), 0), 60, 90);
        for (int y = 0; y < b.getHeight(); y++) for (int x = 0; x < b.getWidth(); x++)
            assertEquals(0xff125678, b.getPixel(x, y));
    }
    @Test public void fourLinearDirectionsMatchCanvasCoordinates() {
        for (int direction = 0; direction < 4; direction++) {
            Bitmap b = render(new GibBackgroundDrawable(1, Color.WHITE, Color.BLACK, direction, 0), 80, 120);
            for (int[] point : new int[][]{{1,1}, {78,118}, {20,60}, {60,30}}) {
                double x = (point[0] + .5) / 80, y = (point[1] + .5) / 120;
                double t = new double[]{y, x, 1-y, 1-x}[direction];
                assertEquals(255 * (1-t), Color.red(b.getPixel(point[0], point[1])), 2);
            }
        }
    }
    @Test public void bothRadialDirectionsReachCornersAndRespectAspectRatio() {
        for (int[] size : new int[][]{{80,120}, {120,80}, {100,100}}) {
            for (int direction : new int[]{4,5}) {
                Bitmap b = render(new GibBackgroundDrawable(2, Color.WHITE, Color.BLACK, direction, 0), size[0], size[1]);
                for (int[] point : new int[][]{{0,0}, {size[0]-1,size[1]-1},
                        {size[0]/2,size[1]/2}, {size[0]/2,0}, {0,size[1]/2}}) {
                    double t = Math.hypot(point[0]+.5-size[0]/2., point[1]+.5-size[1]/2.)
                            / Math.hypot(size[0]/2., size[1]/2.);
                    if (direction == 5) t = 1-t;
                    assertEquals(255 * (1-t), Color.red(b.getPixel(point[0], point[1])), 2);
                }
            }
        }
    }
    @Test public void alphaAndRoundedCornersRemainConfigurableAfterBoundsChange() {
        GibBackgroundDrawable d = new GibBackgroundDrawable(0, Color.RED, Color.BLUE, 0, 14);
        d.setAlpha(100);
        Bitmap b = render(d, 80, 100);
        assertEquals(0, Color.alpha(b.getPixel(0,0)));
        assertEquals(100, Color.alpha(b.getPixel(40,50)));
        d.setAlpha(255);
        assertEquals(255, Color.alpha(render(d, 120, 80).getPixel(60,40)));
        GibBackgroundDrawable gradient = new GibBackgroundDrawable(2, Color.WHITE, Color.BLACK, 4, 0);
        gradient.setAlpha(80);
        assertEquals(80, Color.alpha(render(gradient, 60, 90).getPixel(30,45)));
        gradient.setAlpha(-1);
        assertEquals(0, Color.alpha(render(gradient, 60, 90).getPixel(30,45)));
        gradient.setAlpha(300);
        assertEquals(255, Color.alpha(render(gradient, 60, 90).getPixel(30,45)));
    }
    private void assertExactAlpha(int type, int alpha) {
        int[] directions = type == 0 ? new int[]{0}
                : type == 1 ? new int[]{0, 1, 2, 3} : new int[]{4, 5};
        for (int direction : directions) {
            GibBackgroundDrawable d = new GibBackgroundDrawable(type, 0xff123456, 0xffabcdef, direction, 0);
            // Bounds first, then alpha: exercise rebuilding the existing Paint/shader.
            d.setBounds(0, 0, 24, 32);
            d.setAlpha(alpha);
            for (int[] size : new int[][]{{24, 32}, {32, 24}}) {
                Bitmap b = render(d, size[0], size[1]);
                for (int y = 0; y < b.getHeight(); y++) {
                    for (int x = 0; x < b.getWidth(); x++) {
                        assertEquals("type=" + type + " direction=" + direction + " alpha=" + alpha
                                + " pixel=" + x + "," + y, alpha, Color.alpha(b.getPixel(x, y)));
                    }
                }
                b.recycle();
            }
        }
    }

    @Test public void solidAlpha255IsAppliedExactlyOnce() { assertExactAlpha(0, 255); }
    @Test public void solidAlpha128IsAppliedExactlyOnce() { assertExactAlpha(0, 128); }
    @Test public void solidAlpha80IsAppliedExactlyOnce() { assertExactAlpha(0, 80); }
    @Test public void solidAlpha0IsAppliedExactlyOnce() { assertExactAlpha(0, 0); }
    @Test public void linearAlpha255IsAppliedExactlyOnce() { assertExactAlpha(1, 255); }
    @Test public void linearAlpha128IsAppliedExactlyOnce() { assertExactAlpha(1, 128); }
    @Test public void linearAlpha80IsAppliedExactlyOnce() { assertExactAlpha(1, 80); }
    @Test public void linearAlpha0IsAppliedExactlyOnce() { assertExactAlpha(1, 0); }
    @Test public void radialAlpha255IsAppliedExactlyOnce() { assertExactAlpha(2, 255); }
    @Test public void radialAlpha128IsAppliedExactlyOnce() { assertExactAlpha(2, 128); }
    @Test public void radialAlpha80IsAppliedExactlyOnce() { assertExactAlpha(2, 80); }
    @Test public void radialAlpha0IsAppliedExactlyOnce() { assertExactAlpha(2, 0); }
}
