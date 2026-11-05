package com.ubai.veilpanel.ui;

import android.graphics.Canvas;
import android.view.View;
import android.view.ViewTreeObserver;

/** Replays just the scrolling status region below SystemUI. The interactive
 * accessibility window stays above heads-up banners and clips out real icons.
 * Records GPU display-list commands, never captures the screen or bitmaps. */
final class StatusBarContentUnderlay extends View {
    private final PanelContentViewport content;
    private final int statusHeight;
    private boolean closed,failed;
    private float curtainOffset;
    private final ViewTreeObserver.OnPreDrawListener update=this::record;
    StatusBarContentUnderlay(PanelContentViewport content,int statusHeight){
        super(content.getContext());this.content=content;this.statusHeight=statusHeight;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        content.getViewTreeObserver().addOnPreDrawListener(update);
    }
    void setCurtainOffset(float offset){curtainOffset=offset;}
    private boolean record(){
        int width=content.getWidth(),height=content.getHeight();
        if(closed||failed||width<1||height<1||!content.isHardwareAccelerated())return true;
        try{
            if(content.recordFrame())invalidate();
        }catch(RuntimeException unavailable){
            failed=true;content.releaseFrame();
            android.util.Log.w("VeilStatusContent","Under-status rendering unavailable");
        }
        return true;
    }
    @Override protected void onDraw(Canvas canvas){
        if(!closed&&!failed&&canvas.isHardwareAccelerated()){
            int save=canvas.save();
            canvas.clipRect(0,-curtainOffset,getWidth(),statusHeight-curtainOffset);
            content.drawFrame(canvas);
            canvas.restoreToCount(save);
        }
    }
    void close(){
        closed=true;
        if(content.getViewTreeObserver().isAlive())content.getViewTreeObserver().removeOnPreDrawListener(update);
        content.releaseFrame();
    }
}
