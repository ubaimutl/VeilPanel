package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/** One content display list, reused above and below the native status bar. */
final class PanelContentViewport extends FrameLayout {
    private RenderNode frame;
    private boolean shared;
    private int bottomClear,fadeDistance,fadeStart,fadeEnd;
    private final Paint mask=new Paint();

    PanelContentViewport(Context context){super(context);}

    void setBottomFade(int clearHeight,int fadeHeight){
        bottomClear=Math.max(0,clearHeight);fadeDistance=Math.max(0,fadeHeight);updateMask();invalidate();
    }
    private void updateMask(){
        fadeEnd=Math.max(0,getHeight()-bottomClear);fadeStart=Math.max(0,fadeEnd-fadeDistance);
        mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mask.setShader(new LinearGradient(0,fadeStart,0,Math.max(fadeStart+1,fadeEnd),0xffffffff,0x00ffffff,Shader.TileMode.CLAMP));
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(fadeDistance>0)updateMask();}
    private void drawContent(Canvas canvas){
        if(fadeDistance==0||bottomClear==0){super.dispatchDraw(canvas);return;}
        int layer=canvas.saveLayer(0,0,getWidth(),getHeight(),null);
        super.dispatchDraw(canvas);
        canvas.drawRect(0,fadeStart,getWidth(),getHeight(),mask);
        canvas.restoreToCount(layer);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        // Fully faded notification actions must not remain invisible tap targets.
        if(fadeDistance>0&&bottomClear>0&&event.getActionMasked()==MotionEvent.ACTION_DOWN&&event.getY()>=fadeEnd)return false;
        return super.dispatchTouchEvent(event);
    }

    boolean recordFrame(){
        if(getWidth()<1||getHeight()<1)return false;
        if(frame==null)frame=new RenderNode("VeilPanelContent");
        frame.setPosition(0,0,getWidth(),getHeight());
        RecordingCanvas canvas=frame.beginRecording(getWidth(),getHeight());
        try{
            // Record the complete content once. Recording only the top strip
            // visits and updates the same children again in the main draw.
            drawContent(canvas);
        }finally{frame.endRecording();}
        shared=true;
        return true;
    }

    void drawFrame(Canvas canvas){
        if(shared&&frame!=null&&canvas.isHardwareAccelerated())canvas.drawRenderNode(frame);
    }

    @Override protected void dispatchDraw(Canvas canvas){
        if(shared&&canvas.isHardwareAccelerated())drawFrame(canvas);
        else drawContent(canvas);
    }

    void releaseFrame(){
        shared=false;
        if(frame!=null){frame.discardDisplayList();frame=null;}
    }

    @Override protected void onDetachedFromWindow(){releaseFrame();super.onDetachedFromWindow();}
}
