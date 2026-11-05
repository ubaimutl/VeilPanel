// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.ui.navigation;
import android.app.Activity;
import android.os.Looper;
import java.time.Duration;
import android.view.MotionEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.viewpager.widget.ViewPager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.Shadows;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w320dp-h600dp-mdpi")
public class SectionPagerTest {
    Activity activity; SectionPager pager; TextView overflow;
    @Before public void setup() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        pager = new SectionPager(activity); pager.setSwipeEnabled(true);
        overflow = new TextView(activity); overflow.setText("+ >"); overflow.setMinWidth(48);
        pager.setRightOverflowIndicator(overflow);
        activity.setContentView(pager);
    }
    void layout(int width) {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        pager.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
        pager.layout(0, 0, width, 600);
    }
    ViewPager body() { return (ViewPager) pager.getChildAt(1); }
    HorizontalScrollView menu() { return (HorizontalScrollView)((FrameLayout)pager.getChildAt(0)).getChildAt(0); }
    void pages(int count) {
        for(int i=0;i<count;i++)pager.addSection("Section title " + i,new LinearLayout(activity));
        LinearLayout tabs=(LinearLayout)menu().getChildAt(0);
        // Explicit tab widths avoid dependence on the test runtime's font measurement.
        for(int i=0;i<count;i++)((TextView)tabs.getChildAt(i)).setMinWidth(120);
        layout(320);
    }
    void event(int action,float x,float y) {
        event(0,30,action,x,y);
    }
    void event(long downTime,long eventTime,int action,float x,float y) {
        int[] location=new int[2];body().getLocationOnScreen(location);
        MotionEvent e=MotionEvent.obtain(downTime,eventTime,action,x+location[0],y+location[1],0);
        e.offsetLocation(-location[0],-location[1]);body().dispatchTouchEvent(e);e.recycle();
    }
    void finishAnimations() { Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1)); }
    void swipe(float start,float end,float y) {
        event(MotionEvent.ACTION_DOWN,start,y);event(MotionEvent.ACTION_MOVE,end,y);event(MotionEvent.ACTION_UP,end,y);
    }
    @Test public void eachTabSelectsExactlyOnePage() {
        pages(5);LinearLayout tabs=(LinearLayout)menu().getChildAt(0);
        for(int i=0;i<5;i++) {
            tabs.getChildAt(i).performClick();
            finishAnimations();
            assertEquals(i,pager.getCurrentSection());
            assertEquals(i,body().getCurrentItem());
        }
    }
    @Test public void swipeOutsideControlsAndStopsAtEdges() {
        pages(5);swipe(260,30,250);finishAnimations();assertEquals(1,pager.getCurrentSection());
        swipe(30,260,250);finishAnimations();assertEquals(0,pager.getCurrentSection());
        swipe(30,260,250);finishAnimations();assertEquals(0,pager.getCurrentSection());
    }
    @Test public void usesNativeAndroidXViewPagerEngine() {
        pages(3);
        assertTrue(body() instanceof ViewPager);
        assertSame(body().getAdapter(), body().getAdapter());
        assertTrue(body().getOffscreenPageLimit() >= 2);
    }
    @Test public void insufficientDragReturnsToCurrentPage() {
        pages(3);event(0,200,250);event(2,150,250);event(1,150,250);finishAnimations();
        assertEquals(0,pager.getCurrentSection());
        assertEquals(0,body().getCurrentItem());
    }
    @Test public void selectionCallbackRunsOnlyForCommittedDrag() {
        pages(3);final int[] calls={0};pager.setOnSectionSelectedListener(index->calls[0]++);
        swipe(200,150,250);finishAnimations();assertEquals(0,calls[0]);
        swipe(260,30,250);finishAnimations();assertEquals(1,calls[0]);
    }
    @Test public void shortSlowDragReturnsButShortFastFlingCommits() {
        pages(3);
        event(0,0,MotionEvent.ACTION_DOWN,260,250);
        event(0,1000,MotionEvent.ACTION_MOVE,200,250);
        event(0,1100,MotionEvent.ACTION_UP,200,250);
        finishAnimations();assertEquals(0,pager.getCurrentSection());
        event(2000,2000,MotionEvent.ACTION_DOWN,260,250);
        event(2000,2020,MotionEvent.ACTION_MOVE,220,250);
        event(2000,2040,MotionEvent.ACTION_UP,200,250);
        finishAnimations();assertEquals(1,pager.getCurrentSection());
    }
    @Test public void disabledSwipeAndCancelDoNotSelect() {
        pages(5);pager.setSwipeEnabled(false);swipe(260,30,250);assertEquals(0,pager.getCurrentSection());
        pager.setSwipeEnabled(true);event(0,260,250);event(2,30,250);event(3,30,250);
        assertEquals(0,pager.getCurrentSection());
    }
    @Test public void seekBarBandIsProtectedButOtherAreasSwipe() {
        LinearLayout page=new LinearLayout(activity);SeekBar seek=new SeekBar(activity);
        page.addView(seek,new LinearLayout.LayoutParams(300,48));
        pager.addSection("One",page);pager.addSection("Two",new View(activity));layout(320);
        seek.setMax(100);seek.setProgress(50);
        swipe(160,250,24);assertEquals(0,pager.getCurrentSection());assertTrue(seek.getProgress()>50);
        swipe(260,20,220);assertEquals(1,pager.getCurrentSection());
    }
    @Test public void customControlCanOwnGesture() {
        View control=new View(activity);pager.addSection("One",control);pager.addSection("Two",new View(activity));
        pager.setGestureProtection(view -> view==control);layout(320);
        swipe(250,40,100);assertEquals(0,pager.getCurrentSection());
    }
    @Test public void verticalGestureDoesNotBecomePageSwipe() {
        pages(5);
        event(0,260,100);
        event(2,255,260);
        event(1,250,420);
        finishAnimations();
        assertEquals(0,pager.getCurrentSection());
        assertEquals(0,body().getCurrentItem());
    }
    @Test public void indicatorVisibilityAndActiveTabReveal() {
        pages(5);assertEquals(View.VISIBLE,overflow.getVisibility());
        pager.selectSection(4,false);Shadows.shadowOf(Looper.getMainLooper()).idle();
        // Trigger deterministic layout reveal, without relying on animation wall time.
        layout(321);assertTrue(menu().getScrollX()>0);
        menu().scrollTo(10000,0);assertEquals(View.INVISIBLE,overflow.getVisibility());
        layout(2000);assertEquals(View.INVISIBLE,overflow.getVisibility());
    }
    @Test public void pagesRetainIndependentVerticalScroll() {
        ScrollView first=new ScrollView(activity),second=new ScrollView(activity);
        LinearLayout firstContent=new LinearLayout(activity),secondContent=new LinearLayout(activity);
        firstContent.setMinimumHeight(2000);secondContent.setMinimumHeight(2000);
        first.addView(firstContent);second.addView(secondContent);
        pager.addSection("One",first);pager.addSection("Two",second);layout(320);
        first.scrollTo(0,200);assertEquals(200,first.getScrollY());
        pager.selectSection(1,false);layout(320);
        second.scrollTo(0,400);assertEquals(400,second.getScrollY());
        pager.selectSection(0,false);layout(320);
        assertEquals(200,first.getScrollY());assertEquals(400,second.getScrollY());
    }
    @Test public void primaryNavigationUsesIconAboveLabelAndTallerHeader() {
        pager.setPrimaryNavigation(true);
        pager.addSection(
                "Shapes",
                android.R.drawable.ic_menu_gallery,
                new View(activity)
        );
        layout(320);

        LinearLayout tabs = (LinearLayout) menu().getChildAt(0);
        TextView tab = (TextView) tabs.getChildAt(0);

        assertTrue(pager.isPrimaryNavigation());
        assertNotNull(tab.getCompoundDrawablesRelative()[1]);
        assertEquals(76, pager.getChildAt(0).getHeight());
        assertEquals(Gravity.CENTER, tab.getGravity());
        assertEquals(2, tab.getMaxLines());
        assertEquals(72, tab.getMinWidth());
    }

    @Test(expected = IllegalStateException.class)
    public void primaryNavigationMustBeConfiguredBeforeAddingSections() {
        pager.addSection("One", new View(activity));

        pager.setPrimaryNavigation(true);
    }

    @Test public void overflowIsNotAnotherSection() {
        pages(5);overflow.performClick();assertEquals(5,pager.getSectionCount());assertEquals(0,pager.getCurrentSection());
    }

    @Test
    public void navigationVisibilityDoesNotChangePagingState() {
        int selected = pager.getCurrentSection();
        boolean swipe = pager.isSwipeEnabled();

        pager.setNavigationVisible(false);

        assertFalse(pager.isNavigationVisible());
        assertEquals(selected, pager.getCurrentSection());
        assertEquals(swipe, pager.isSwipeEnabled());

        pager.setNavigationVisible(true);

        assertTrue(pager.isNavigationVisible());
        assertEquals(selected, pager.getCurrentSection());
        assertEquals(swipe, pager.isSwipeEnabled());
    }

}
