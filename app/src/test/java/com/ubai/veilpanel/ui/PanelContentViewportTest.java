package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RenderNode;
import android.view.View;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PanelContentViewportTest {
    @Test public void mainWindowAndStatusStripReplayOneChildDrawing(){
        Context context=RuntimeEnvironment.getApplication();
        int[] draws={0};
        PanelContentViewport viewport=new PanelContentViewport(context);
        View child=new View(context){@Override protected void onDraw(Canvas canvas){draws[0]++;canvas.drawColor(0xff336699);}};
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
}
