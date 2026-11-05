package com.ubai.veilpanel.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import fr.gibapps.ui.background.GibBackgroundDrawable;
import com.ubai.veilpanel.config.PanOSPPreferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PanelBackgroundDrawableTest {
    private PanelBackgroundDrawable panel(int type, int color1, int color2, int direction,
            int alpha, Bitmap image, int imageOpacity, float radius) {
        return new PanelBackgroundDrawable(type, color1, color2, direction, alpha,
                image, imageOpacity, radius, 0, 0, 1, 0, 0, 1);
    }
    private Bitmap render(Drawable drawable, int width, int height) {
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(new Canvas(output));
        return output;
    }
    @Test public void solidDelegatesToGibUiAndKeepsItsColor() throws Exception {
        PanelBackgroundDrawable d = panel(0, 0xff125678, Color.WHITE, 0, 255, null, 0, 0);
        assertEquals(GibBackgroundDrawable.class,
                PanelBackgroundDrawable.class.getDeclaredField("gibBackground").getType());
        Bitmap b = render(d, 60, 80);
        for (int y=0; y<80; y++) for (int x=0; x<60; x++) assertEquals(0xff125678, b.getPixel(x,y));
    }
    @Test public void fourLinearDirectionsPreserveGeometryAndEndpointColors() {
        for (int direction=0; direction<4; direction++) {
            Bitmap b = render(panel(1, Color.WHITE, Color.BLACK, direction, 255, null, 0, 0), 80, 120);
            for (int[] p : new int[][]{{0,0},{79,119},{20,70},{60,30}}) {
                double x=(p[0]+.5)/80, y=(p[1]+.5)/120;
                double t=new double[]{y,x,1-y,1-x}[direction];
                assertEquals(255*(1-t), Color.red(b.getPixel(p[0],p[1])), 2);
                assertEquals(255, Color.alpha(b.getPixel(p[0],p[1])));
            }
        }
    }
    @Test public void radialDirectionsKeepCenterAndCornerRadiusInBothOrientations() {
        for (int direction : new int[]{4,5}) for (int[] size : new int[][]{{80,120},{120,80}}) {
            Bitmap b=render(panel(2,Color.WHITE,Color.BLACK,direction,255,null,0,0),size[0],size[1]);
            for (int[] p : new int[][]{{0,0},{size[0]-1,size[1]-1},{size[0]/2,size[1]/2},{0,size[1]/2}}) {
                double t=Math.hypot(p[0]+.5-size[0]/2.,p[1]+.5-size[1]/2.)/Math.hypot(size[0]/2.,size[1]/2.);
                if(direction==5)t=1-t;
                assertEquals(255*(1-t),Color.red(b.getPixel(p[0],p[1])),2);
            }
        }
    }
    @Test public void backgroundAlphaIsAppliedOnceIncludingFiftyPercent() {
        assertEquals(128, Math.round(255f*50/100f));
        for(int type=0;type<=2;type++) for(int alpha:new int[]{0,80,128,255}) {
            Bitmap b=render(panel(type,Color.RED,Color.BLUE,type==2?4:0,alpha,null,0,0),24,32);
            for(int y=0;y<32;y++) for(int x=0;x<24;x++)
                assertEquals("type="+type+" alpha="+alpha,alpha,Color.alpha(b.getPixel(x,y)));
        }
    }
    @Test public void imageIsCompositedAfterBackgroundWithIndependentOpacity() {
        Bitmap image=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);image.eraseColor(Color.BLUE);
        for(int opacity:new int[]{0,50,100}) {
            int pixel=render(panel(1,Color.RED,Color.RED,0,128,image,opacity,0),40,40).getPixel(20,20);
            if(opacity==0) { assertEquals(128,Color.alpha(pixel));assertEquals(255,Color.red(pixel)); }
            if(opacity==50) {
                assertEquals(192,Color.alpha(pixel));
                assertEquals(85,Color.red(pixel),2);assertEquals(170,Color.blue(pixel),2);
            }
            if(opacity==100) assertEquals(Color.BLUE,pixel);
        }
    }
    @Test public void bothBackgroundAndImageKeepRoundedCorners() {
        Bitmap image=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);image.eraseColor(Color.BLUE);
        for(Bitmap source:new Bitmap[]{null,image}) {
            Bitmap b=render(panel(2,Color.RED,Color.GREEN,4,255,source,100,14),80,100);
            assertEquals(0,Color.alpha(b.getPixel(0,0)));
            assertEquals(0,Color.alpha(b.getPixel(79,99)));
            assertEquals(255,Color.alpha(b.getPixel(40,50)));
        }
    }
    @Test public void imageUvCropStillSelectsTheRightHalf() {
        Bitmap image=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);
        for(int y=0;y<100;y++) for(int x=0;x<100;x++) image.setPixel(x,y,x<50?Color.RED:Color.GREEN);
        PanelBackgroundDrawable cropped=new PanelBackgroundDrawable(1,Color.WHITE,Color.BLACK,0,128,
                image,100,0,.5f,0,1,0,.5f,1);
        Bitmap b=render(cropped,80,120);
        assertEquals(Color.GREEN,b.getPixel(10,60));assertEquals(Color.GREEN,b.getPixel(70,60));
    }
    @Test public void previewAndRealPanelUseTheSameSharedBackgroundAndAlpha() {
        org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("veil_panel",0).edit().putBoolean("dynamic",false).commit();
        org.robolectric.android.controller.ActivityController<Activity> c=Robolectric.buildActivity(Activity.class).setup();
        Activity activity=c.get();
        PanOSPPreferences prefs=new PanOSPPreferences(activity);
        prefs.setThemeBackgroundType(2);prefs.setThemeBackgroundColor(Color.RED);
        prefs.setThemeBackgroundColor2(Color.BLUE);prefs.setThemeBackgroundDirection(5);
        prefs.setPanelOpacityPercent(50);
        java.util.Map<String,Object> noImage=new java.util.HashMap<>();
        noImage.put("panel_background_image_uri",null);assertTrue(prefs.publishConfiguration(noImage));
        ShadeView shade=new ShadeView(activity,()->{});
        try {
            PanelPreviewView preview=new PanelPreviewView(activity);
            Drawable actual=shade.getBackground(), thumbnail=findBackground(preview);
            assertTrue(actual instanceof PanelBackgroundDrawable);assertNotNull(thumbnail);
            Bitmap a=render(actual,80,120), b=render(thumbnail,80,120);
            assertTrue(a.sameAs(b));assertEquals(128,Color.alpha(a.getPixel(40,60)));
        } finally { shade.onHidden();c.pause().stop().destroy(); }
    }
    private Drawable findBackground(View view) {
        if(view.getBackground() instanceof PanelBackgroundDrawable)return view.getBackground();
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) {
                Drawable d=findBackground(group.getChildAt(i));if(d!=null)return d;
            }
        }
        return null;
    }
}
