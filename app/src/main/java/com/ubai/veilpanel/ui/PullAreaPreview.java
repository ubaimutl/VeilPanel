package com.ubai.veilpanel.ui;

import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** An activity-only, noninteractive outline. Never adds a system overlay. */
public final class PullAreaPreview extends Drawable {
    private final Activity activity;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final View decor;
    private boolean shown;
    private final View.OnLayoutChangeListener resized=(v,l,t,r,b,ol,ot,or,ob)->refresh();
    public PullAreaPreview(Activity activity) {
        this.activity=activity;decor=activity.getWindow().getDecorView();
    }
    public boolean isShown(){return shown;}
    public void show(){if(!shown){shown=true;decor.getOverlay().add(this);decor.addOnLayoutChangeListener(resized);}refresh();}
    public void hide(){shown=false;decor.getOverlay().remove(this);decor.removeOnLayoutChangeListener(resized);}
    public void refresh(){
        // Decor is edge-to-edge; account for any OEM offset into the screen.
        int[] position=new int[2];decor.getLocationOnScreen(position);
        int width=decor.getWidth()>0?decor.getWidth():activity.getResources().getDisplayMetrics().widthPixels;
        setBounds(-position[0],-position[1],PanelPullArea.width(activity,width)-position[0],PanelPullArea.height(activity)-position[1]);
        invalidateSelf();
    }
    @Override public void draw(Canvas canvas){
        Rect bounds=getBounds();int color=SettingsStyle.accent(activity);
        paint.setStyle(Paint.Style.FILL);paint.setColor((color&0x00FFFFFF)|0x33000000);canvas.drawRect(bounds,paint);
        float stroke=2*activity.getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setColor(color);
        canvas.drawRect(bounds.left+stroke/2,bounds.top+stroke/2,bounds.right-stroke/2,bounds.bottom-stroke/2,paint);
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
