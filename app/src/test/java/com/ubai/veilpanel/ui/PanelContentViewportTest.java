package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RenderNode;
import android.view.View;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PanelContentViewportTest {
    @Test public void mainWindowAndStatusStripReplayOneChildDrawing(){assertSingleDrawing(false);assertSingleDrawing(true);}
    private void assertSingleDrawing(boolean fade){
        Context context=RuntimeEnvironment.getApplication();
        int[] draws={0};
        PanelContentViewport viewport=new PanelContentViewport(context);
        View child=new View(context){@Override protected void onDraw(Canvas canvas){draws[0]++;canvas.drawColor(0xff336699);}};
        if(fade)viewport.setBottomFade(80,60);
        viewport.addView(child,new FrameLayout.LayoutParams(200,300));
        viewport.measure(View.MeasureSpec.makeMeasureSpec(200,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY));
        viewport.layout(0,0,200,300);
        assertTrue(viewport.recordFrame());assertEquals(1,draws[0]);
        RenderNode target=new RenderNode("two-window-test");target.setPosition(0,0,200,300);
        Canvas canvas=target.beginRecording(200,300);
        try{viewport.draw(canvas);viewport.drawFrame(canvas);viewport.drawFrame(canvas);}
        finally{target.endRecording();}
        assertEquals("Replaying either part must not draw or update children again",1,draws[0]);
        viewport.releaseFrame();
        canvas=target.beginRecording(200,300);
        try{viewport.drawFrame(canvas);}finally{target.endRecording();}
        assertEquals(1,draws[0]);target.discardDisplayList();
    }
    @Test public void optionalFadeClearsOnlyContentAndLeavesWallpaperVisible(){
        Context context=RuntimeEnvironment.getApplication();PanelContentViewport viewport=new PanelContentViewport(context);View child=new View(context);child.setBackgroundColor(android.graphics.Color.RED);viewport.addView(child,new FrameLayout.LayoutParams(200,300));
        viewport.measure(View.MeasureSpec.makeMeasureSpec(200,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY));viewport.layout(0,0,200,300);
        android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(200,300,android.graphics.Bitmap.Config.ARGB_8888);image.eraseColor(android.graphics.Color.BLUE);viewport.draw(new Canvas(image));assertEquals(android.graphics.Color.RED,image.getPixel(100,280));
        viewport.setBottomFade(80,60);image.eraseColor(android.graphics.Color.BLUE);viewport.draw(new Canvas(image));
        assertEquals(android.graphics.Color.RED,image.getPixel(100,100));int transition=image.getPixel(100,190);assertTrue(android.graphics.Color.red(transition)>0&&android.graphics.Color.blue(transition)>0);assertEquals(android.graphics.Color.BLUE,image.getPixel(100,280));image.recycle();
    }
    @Test public void fullyFadedCardsAreNotInvisibleTapTargets(){
        try(var host=Robolectric.buildActivity(android.app.Activity.class).setup()){
            Context context=host.get();PanelContentViewport viewport=new PanelContentViewport(context);View child=new View(context);int[] clicks={0};child.setOnClickListener(v->clicks[0]++);viewport.addView(child,new FrameLayout.LayoutParams(200,300));viewport.setBottomFade(80,60);host.get().setContentView(viewport);
            viewport.measure(View.MeasureSpec.makeMeasureSpec(200,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY));viewport.layout(0,0,200,300);
            android.view.MotionEvent event=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,100,280,0);assertFalse(viewport.dispatchTouchEvent(event));event.recycle();assertEquals(0,clicks[0]);
            for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}){event=android.view.MotionEvent.obtain(0,10,action,100,100,0);viewport.dispatchTouchEvent(event);event.recycle();}
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(1,clicks[0]);
        }
    }

}
