package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.widget.FrameLayout;

/** One content display list, reused above and below the native status bar. */
final class PanelContentViewport extends FrameLayout {
    private RenderNode frame;
    private boolean shared;

    PanelContentViewport(Context context){super(context);}

    boolean recordFrame(){
        if(getWidth()<1||getHeight()<1)return false;
        if(frame==null)frame=new RenderNode("VeilPanelContent");
        frame.setPosition(0,0,getWidth(),getHeight());
        RecordingCanvas canvas=frame.beginRecording(getWidth(),getHeight());
        try{
            // Record the complete content once. Recording only the top strip
            // visits and updates the same children again in the main draw.
            super.dispatchDraw(canvas);
        }finally{frame.endRecording();}
        shared=true;
        return true;
    }

    void drawFrame(Canvas canvas){
        if(shared&&frame!=null&&canvas.isHardwareAccelerated())canvas.drawRenderNode(frame);
    }

    @Override protected void dispatchDraw(Canvas canvas){
        if(shared&&canvas.isHardwareAccelerated())drawFrame(canvas);
        else super.dispatchDraw(canvas);
    }

    void releaseFrame(){
        shared=false;
        if(frame!=null){frame.discardDisplayList();frame=null;}
    }

    @Override protected void onDetachedFromWindow(){releaseFrame();super.onDetachedFromWindow();}
}
