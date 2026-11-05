// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.ui.navigation;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SectionAppearanceTest {
    TextView tab(SectionPager p,int i) {
        FrameLayout header=(FrameLayout)p.getChildAt(0);
        return (TextView)((LinearLayout)((HorizontalScrollView)header.getChildAt(0)).getChildAt(0)).getChildAt(i);
    }
    Bitmap background(TextView tab) {
        Bitmap bitmap=Bitmap.createBitmap(240,80,Bitmap.Config.ARGB_8888);
        Drawable drawable=tab.getBackground();drawable.setBounds(0,0,240,80);
        drawable.draw(new Canvas(bitmap));return bitmap;
    }
    @Test public void originalDefaultsAndNullReset() {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();SectionPager p=new SectionPager(a);
        p.addSection("One",new View(a));p.addSection("Two",new View(a));
        assertEquals(0xff101216,((ColorDrawable)p.getBackground()).getColor());
        assertEquals(0xff9dc9c2,tab(p,0).getCurrentTextColor());assertEquals(0xffeceff4,tab(p,1).getCurrentTextColor());
        assertEquals(0,background(tab(p,0)).getPixel(120,40));
        assertEquals(0xff283c3b,background(tab(p,0)).getPixel(120,79));
        assertEquals(0,background(tab(p,1)).getPixel(120,79));
        p.setAppearance(new SectionAppearance(0,1,2,3,4));p.setAppearance(null);
        assertEquals(0xff9dc9c2,tab(p,0).getCurrentTextColor());
    }
    @Test public void appearanceBeforeAndAfterPagesDoesNotSelectOrNotify() {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();SectionPager p=new SectionPager(a);
        SectionAppearance style=new SectionAppearance(0,Color.YELLOW,Color.WHITE,0x30000000,0);
        p.setAppearance(style);p.addSection("One",new View(a));p.addSection("Two",new View(a));
        p.setSwipeEnabled(true);p.selectSection(1,false);int[] changes={0};
        p.setOnSectionSelectedListener(index->changes[0]++);p.setAppearance(style);
        assertEquals(0,changes[0]);assertTrue(p.isSwipeEnabled());assertEquals(1,p.getCurrentSection());
        assertEquals(Color.YELLOW,tab(p,1).getCurrentTextColor());assertEquals(Color.WHITE,tab(p,0).getCurrentTextColor());
        p.selectSection(0,false);assertEquals(1,changes[0]);assertEquals(Color.YELLOW,tab(p,0).getCurrentTextColor());
        assertEquals(0,((ColorDrawable)p.getBackground()).getColor());
    }
    @Test public void underlineIsVisualAndIndependentOfTextAccent() {
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();SectionPager p=new SectionPager(a);
        p.addSection("One",new View(a));p.addSection("Two",new View(a));
        TextView active=tab(p,0);int left=active.getPaddingLeft(),top=active.getPaddingTop();
        int right=active.getPaddingRight(),bottom=active.getPaddingBottom();
        active.measure(0,0);int width=active.getMeasuredWidth(),height=active.getMeasuredHeight();
        p.setAppearance(new SectionAppearance(0,Color.YELLOW,Color.WHITE,Color.RED,0));
        active.measure(0,0);
        assertEquals(width,active.getMeasuredWidth());assertEquals(height,active.getMeasuredHeight());
        assertEquals(left,active.getPaddingLeft());assertEquals(top,active.getPaddingTop());
        assertEquals(right,active.getPaddingRight());assertEquals(bottom,active.getPaddingBottom());
        assertEquals(Color.YELLOW,active.getCurrentTextColor());
        assertEquals(0,background(active).getPixel(120,40));
        assertEquals(Color.RED,background(active).getPixel(120,79));
        assertEquals(0,background(active).getPixel(0,79));
        Bitmap inactive=background(tab(p,1));
        for(int y=0;y<80;y++)for(int x=0;x<240;x++)assertEquals(0,inactive.getPixel(x,y));
        tab(p,1).performClick();assertEquals(1,p.getCurrentSection());
        assertEquals(Color.YELLOW,tab(p,1).getCurrentTextColor());
        assertEquals(Color.WHITE,active.getCurrentTextColor());
        assertEquals(0,background(active).getPixel(120,79));
    }

}
