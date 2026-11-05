package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.widget.TextView;

/** Optional shaded lettering. Static shader, no bitmap rendering or animation. */
public final class SculptedClockView extends TextView {
    private final boolean sculpted;
    private final LiquidClockMaterial liquid;
    private float shaderSize=-1;
    public SculptedClockView(Context context,boolean sculpted){this(context,sculpted?"sculpted":"plain",null);}
    SculptedClockView(Context context,String style,LiquidGlassBackdrop backdrop){
        super(context);sculpted=!"plain".equals(style);
        liquid="liquid".equals(style)&&backdrop!=null?new LiquidClockMaterial(this,backdrop):null;
        if(liquid!=null)backdrop.watchPersistent(this);
    }
    @Override protected void onDraw(Canvas canvas){
        if(liquid!=null&&liquid.draw(canvas))return;
        if(sculpted){
            float size=getTextSize();
            if(size!=shaderSize){
                shaderSize=size;
                float top=getBaseline()+getPaint().ascent();
                getPaint().setShader(new LinearGradient(0,top,0,top+size,
                    new int[]{0xffeeeeee,0xff858585,0xffaaaaaa,0xff666666,0xffdedede},
                    new float[]{0,.12f,.48f,.88f,1},Shader.TileMode.CLAMP));
                setShadowLayer(getResources().getDisplayMetrics().density,0,getResources().getDisplayMetrics().density,Color.BLACK);
            }
        }
        super.onDraw(canvas);
    }
}
