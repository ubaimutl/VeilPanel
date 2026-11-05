package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.ubai.veilpanel.R;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Android Views adapter for Kyant's Apache-2.0 lens/highlight shaders.
 * Only the privately selected wallpaper is sampled. Text/icons are never part
 * of the shader input, so group and curtain animations retain their content. */
final class LiquidGlassDrawable extends Drawable {
    private static String lensSource,highlightSource;
    private static boolean reportedReady,reportedFallback;
    private final View card;
    private final LiquidGlassBackdrop backdrop;
    private final Paint material=new Paint(Paint.ANTI_ALIAS_FLAG),tint=new Paint(Paint.ANTI_ALIAS_FLAG),rim=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mapping=new Matrix();
    private final RectF shape=new RectF(),rimShape=new RectF();
    private final int[] location=new int[2],origin=new int[2];
    private final float radius,density,edge,refraction,highlight,tintAmount;
    private final boolean depth,dispersion;
    private final Drawable fallback;
    private RuntimeShader lens,lighting;
    private BitmapShader wallpaper;
    private Bitmap bitmap;
    private boolean failed;
    private int alpha=255;

    LiquidGlassDrawable(Context context,View card,LiquidGlassBackdrop backdrop,float radius){
        this.card=card;this.backdrop=backdrop;this.radius=radius;
        density=context.getResources().getDisplayMetrics().density;
        edge=LiquidGlassOptions.get(context,"liquid_edge")*density;
        refraction=LiquidGlassOptions.get(context,"liquid_refraction")*density;
        highlight=LiquidGlassOptions.get(context,"liquid_highlight")/100f;
        tintAmount=LiquidGlassOptions.get(context,"liquid_tint")/100f;
        depth=LiquidGlassOptions.depth(context);dispersion=LiquidGlassOptions.dispersion(context);
        fallback=new ReferenceGlassDrawable(context,radius,context.getSharedPreferences("veil_panel",0).getInt("glass_opacity",66));
        tint.setColor((Math.round(tintAmount*255)<<24)|(PanOSPTheme.notificationBackground(context)&0xffffff));
        rim.setStyle(Paint.Style.STROKE);rim.setStrokeWidth(Math.max(1,density));
        try{
            synchronized(LiquidGlassDrawable.class){
                if(lensSource==null)lensSource=read(context,R.raw.kyant_liquid_lens);
                if(highlightSource==null)highlightSource=read(context,R.raw.kyant_liquid_highlight);
            }
            lens=new RuntimeShader(lensSource);lighting=new RuntimeShader(highlightSource);
            lighting.setColorUniform("color",Color.WHITE);
            lighting.setFloatUniform("angle",(float)(Math.PI/4));lighting.setFloatUniform("falloff",1f);
        }catch(RuntimeException unavailable){fail(unavailable);}
    }
    private static String read(Context context,int resource){
        try(InputStream stream=context.getResources().openRawResource(resource)){
            return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }catch(java.io.IOException unavailable){throw new IllegalStateException("Liquid Glass shader unavailable",unavailable);}
    }
    private void fail(RuntimeException error){
        failed=true;lens=null;lighting=null;
        if(!reportedFallback){reportedFallback=true;android.util.Log.w("VeilLiquidGlass","Shader unavailable; keeping frosted cards",error);}
    }
    @Override protected void onBoundsChange(Rect bounds){
        shape.set(bounds);float stroke=Math.max(1,density);shape.inset(stroke*.5f,stroke*.5f);
        rimShape.set(shape);fallback.setBounds(bounds);
    }
    /** CENTER_CROP mapping anchored to the same root as the wallpaper cover. */
    static void wallpaperMatrix(Matrix result,int imageWidth,int imageHeight,int rootWidth,int rootHeight,float x,float y){
        float scale=Math.max(rootWidth/(float)Math.max(1,imageWidth),rootHeight/(float)Math.max(1,imageHeight));
        result.setScale(scale,scale);
        result.postTranslate((rootWidth-imageWidth*scale)*.5f-x,(rootHeight-imageHeight*scale)*.5f-y);
    }
    @Override public void draw(Canvas canvas){
        if(getBounds().isEmpty())return;
        Bitmap source=backdrop.bitmap();
        if(failed||source==null||source.isRecycled()){fallback.setAlpha(alpha);fallback.draw(canvas);return;}
        if(source!=bitmap){bitmap=source;wallpaper=new BitmapShader(source,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);}
        View root=card.getRootView();card.getLocationOnScreen(location);root.getLocationOnScreen(origin);
        wallpaperMatrix(mapping,source.getWidth(),source.getHeight(),Math.max(1,root.getWidth()),Math.max(1,root.getHeight()),location[0]-origin[0],location[1]-origin[1]);
        wallpaper.setLocalMatrix(mapping);
        float width=getBounds().width(),height=getBounds().height(),r=Math.min(radius,Math.min(width,height)*.5f);
        try{
            if(canvas.isHardwareAccelerated()){
                lens.setInputShader("content",wallpaper);
                lens.setFloatUniform("size",width,height);lens.setFloatUniform("offset",0,0);
                lens.setFloatUniform("cornerRadii",r,r,r,r);
                lens.setFloatUniform("refractionHeight",Math.max(.5f,Math.min(edge,Math.min(width,height)*.5f-.5f)));
                lens.setFloatUniform("refractionAmount",-refraction);
                lens.setFloatUniform("depthEffect",depth?1:0);lens.setFloatUniform("chromaticAberration",dispersion?1:0);
                material.setShader(lens);
                lighting.setFloatUniform("size",width,height);lighting.setFloatUniform("cornerRadii",r,r,r,r);rim.setShader(lighting);
                if(!reportedReady){reportedReady=true;android.util.Log.i("VeilLiquidGlass","Kyant lens and highlight shaders active");}
            }else{
                material.setShader(wallpaper);rim.setShader(null);rim.setColor(Color.WHITE);
            }
            material.setAlpha(alpha);tint.setAlpha(Math.round(tintAmount*alpha));rim.setAlpha(Math.round(highlight*alpha));
            canvas.drawRoundRect(shape,r,r,material);canvas.drawRoundRect(shape,r,r,tint);
            canvas.drawRoundRect(rimShape,r,r,rim);
        }catch(RuntimeException unavailable){fail(unavailable);fallback.setAlpha(alpha);fallback.draw(canvas);}
    }
    @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){material.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
