package com.ubai.veilpanel.ui;

import android.graphics.*;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

/** A wallpaper lens clipped to cached clock glyphs. No continuous animation,
 * screen capture, background-app blur, or bitmap allocation on each frame. */
final class LiquidClockMaterial {
    private static final String SOURCE="""
        uniform shader wallpaper;
        uniform shader glyph;
        uniform float density;
        uniform float frost;
        uniform float highlight;
        uniform float refraction;
        uniform float bevel;
        uniform float textTop;
        uniform float textHeight;
        float a(float2 p) { return glyph.eval(p).a; }
        half4 main(float2 p) {
            float coverage=a(p);
            if(coverage<0.002) return half4(0);
            float d=max(1.0,density*bevel);
            float2 n=float2(a(p-float2(d,0))-a(p+float2(d,0)),
                            a(p-float2(0,d))-a(p+float2(0,d)));
            float q=d*0.707107;
            float surrounding=(a(p+float2(d,0))+a(p-float2(d,0))+
                a(p+float2(0,d))+a(p-float2(0,d))+
                a(p+float2(q,q))+a(p-float2(q,q))+
                a(p+float2(q,-q))+a(p-float2(q,-q)))*0.125;
            float edge=clamp((coverage-surrounding)*2.5,0.0,1.0);
            float2 normal=n/max(length(n),0.001);
            half3 colour=wallpaper.eval(p-normal*refraction*edge).rgb;
            float vertical=clamp((p.y-textTop)/max(textHeight,1.0),0.0,1.0);
            float light=0.57+0.08*cos(vertical*6.28318);
            colour=mix(colour,half3(light),half(frost));
            float facing=dot(normal,normalize(float2(-0.7,-1.0)));
            float specular=max(facing,0.0);
            float reflection=max(-facing,0.0)*0.32;
            colour=mix(colour,half3(1),half(highlight*edge*(0.08+0.92*specular+reflection)));
            colour*=half(1.0-highlight*edge*(1.0-specular)*0.12);
            return half4(colour*half(coverage),half(coverage));
        }
        """;
    private final TextView clock;
    private final LiquidGlassBackdrop backdrop;
    private final Paint material=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint lettering=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mapping=new Matrix();
    private final int[] location=new int[2],origin=new int[2];
    private RuntimeShader shader;
    private Bitmap source,mask;
    private BitmapShader wallpaper,glyph;
    private String text="";
    private Typeface face;
    private float size=-1,baseline,x;
    private int width,height,gravity,direction;
    private boolean failed;
    LiquidClockMaterial(TextView clock,LiquidGlassBackdrop backdrop){this.clock=clock;this.backdrop=backdrop;}
    private void updateGlyph(){
        String next=clock.getText().toString();int w=clock.getWidth(),h=clock.getHeight();
        float nextSize=clock.getTextSize(),base=clock.getBaseline();
        int alignment=Gravity.getAbsoluteGravity(clock.getGravity(),clock.getLayoutDirection())&Gravity.HORIZONTAL_GRAVITY_MASK;
        if(mask!=null&&next.equals(text)&&w==width&&h==height&&nextSize==size&&base==baseline&&face==clock.getTypeface()&&alignment==gravity&&direction==clock.getLayoutDirection())return;
        text=next;width=w;height=h;size=nextSize;baseline=base;face=clock.getTypeface();gravity=alignment;direction=clock.getLayoutDirection();
        // Replace only when clock text or geometry changes (normally once/minute).
        // Let GC reclaim the previous bitmap after the renderer releases it.
        mask=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        lettering.set(clock.getPaint());lettering.setShader(null);lettering.clearShadowLayer();lettering.setColor(Color.WHITE);
        float left=clock.getCompoundPaddingLeft(),right=w-clock.getCompoundPaddingRight(),length=lettering.measureText(text);
        x=alignment==Gravity.CENTER_HORIZONTAL?(left+right-length)/2:alignment==Gravity.RIGHT?right-length:left;
        new Canvas(mask).drawText(text,x,baseline,lettering);
        glyph=new BitmapShader(mask,Shader.TileMode.DECAL,Shader.TileMode.DECAL);
    }
    boolean draw(Canvas canvas){
        Bitmap bitmap=backdrop.bitmap();
        if(failed||!canvas.isHardwareAccelerated()||bitmap==null||bitmap.isRecycled()||clock.getWidth()<1||clock.getHeight()<1)return false;
        try{
            updateGlyph();
            if(shader==null)shader=new RuntimeShader(SOURCE);
            if(source!=bitmap){source=bitmap;wallpaper=new BitmapShader(bitmap,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);}
            View root=clock.getRootView();clock.getLocationOnScreen(location);root.getLocationOnScreen(origin);
            LiquidGlassDrawable.wallpaperMatrix(mapping,bitmap.getWidth(),bitmap.getHeight(),Math.max(1,root.getWidth()),Math.max(1,root.getHeight()),location[0]-origin[0],location[1]-origin[1]);
            wallpaper.setLocalMatrix(mapping);
            android.content.SharedPreferences prefs=clock.getContext().getSharedPreferences("veil_panel",0);
            float density=clock.getResources().getDisplayMetrics().density;
            shader.setInputShader("wallpaper",wallpaper);shader.setInputShader("glyph",glyph);
            shader.setFloatUniform("density",density);
            shader.setFloatUniform("frost",Math.max(0,Math.min(100,prefs.getInt("clock_glass_frost",35)))/100f);
            shader.setFloatUniform("highlight",Math.max(0,Math.min(100,prefs.getInt("clock_glass_highlight",70)))/100f);
            shader.setFloatUniform("refraction",Math.max(0,Math.min(20,prefs.getInt("clock_glass_refraction",8)))*density);
            shader.setFloatUniform("bevel",Math.max(1,Math.min(8,prefs.getInt("clock_glass_bevel",3))));
            shader.setFloatUniform("textTop",baseline+lettering.ascent());shader.setFloatUniform("textHeight",lettering.descent()-lettering.ascent());
            material.setShader(shader);canvas.drawRect(0,0,width,height,material);return true;
        }catch(RuntimeException|OutOfMemoryError unavailable){
            failed=true;shader=null;mask=null;glyph=null;wallpaper=null;
            android.util.Log.w("VeilLiquidGlass","Clock glass unavailable; using shaded lettering");return false;
        }
    }
}
