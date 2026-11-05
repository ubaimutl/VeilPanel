package com.ubai.veilpanel.ui;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.widget.FrameLayout;
import com.ubai.veilpanel.R;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LiquidGlassRenderingTest {
    private Context context;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("veil_panel",0).edit().clear().commit();}
    private String shader(int id)throws Exception{try(var input=context.getResources().openRawResource(id)){return new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}}
    @Test public void upstreamLensAndHighlightCompileAndRecordOnHardwareCanvas()throws Exception{
        RuntimeShader lens=new RuntimeShader(shader(R.raw.kyant_liquid_lens));
        Bitmap source=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);source.eraseColor(Color.BLUE);
        lens.setInputShader("content",new BitmapShader(source,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP));
        lens.setFloatUniform("size",200,100);lens.setFloatUniform("offset",0,0);lens.setFloatUniform("cornerRadii",20,20,20,20);
        lens.setFloatUniform("refractionHeight",18);lens.setFloatUniform("refractionAmount",-12);lens.setFloatUniform("depthEffect",1);lens.setFloatUniform("chromaticAberration",1);
        RuntimeShader highlight=new RuntimeShader(shader(R.raw.kyant_liquid_highlight));
        highlight.setFloatUniform("size",200,100);highlight.setFloatUniform("cornerRadii",20,20,20,20);
        highlight.setColorUniform("color",Color.WHITE);highlight.setFloatUniform("angle",.785f);highlight.setFloatUniform("falloff",1);
        RenderNode node=new RenderNode("liquid-glass-test");node.setPosition(0,0,200,100);
        RecordingCanvas canvas=node.beginRecording(200,100);assertTrue(canvas.isHardwareAccelerated());
        Paint paint=new Paint();paint.setShader(lens);canvas.drawRoundRect(0,0,200,100,20,20,paint);
        paint.setShader(highlight);paint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(0,0,200,100,20,20,paint);
        node.endRecording();assertTrue(node.hasDisplayList());
    }
    @Test public void wallpaperCropStaysAnchoredWhileTheCardScrolls(){
        Matrix first=new Matrix(),scrolled=new Matrix();
        LiquidGlassDrawable.wallpaperMatrix(first,400,600,200,500,10,100);
        LiquidGlassDrawable.wallpaperMatrix(scrolled,400,600,200,500,10,140);
        float[] before={200,300},after={200,300};first.mapPoints(before);scrolled.mapPoints(after);
        assertEquals(before[0],after[0],.001f);assertEquals(before[1]-40,after[1],.001f);
    }
    @Test public void missingImageAndSoftwareRenderingStayVisibleInsteadOfDroppingCards(){
        FrameLayout card=new FrameLayout(context);card.layout(0,0,200,100);
        LiquidGlassBackdrop backdrop=new LiquidGlassBackdrop(card);
        LiquidGlassDrawable drawable=new LiquidGlassDrawable(context,card,backdrop,20);drawable.setBounds(0,0,200,100);
        Bitmap output=Bitmap.createBitmap(200,100,Bitmap.Config.ARGB_8888);drawable.draw(new Canvas(output));
        assertTrue(Color.alpha(output.getPixel(100,50))>0);
        Bitmap selected=Bitmap.createBitmap(20,20,Bitmap.Config.ARGB_8888);selected.eraseColor(Color.BLUE);
        ReflectionHelpers.setField(backdrop,"bitmap",selected);output.eraseColor(Color.TRANSPARENT);drawable.draw(new Canvas(output));
        assertTrue(Color.blue(output.getPixel(100,50))>100);
        backdrop.close();output.eraseColor(Color.TRANSPARENT);drawable.draw(new Canvas(output));assertTrue(Color.alpha(output.getPixel(100,50))>0);
    }
    @Test public void experimentDefaultsOffAndOptionsCannotOverwriteExistingAppearance(){
        var prefs=context.getSharedPreferences("veil_panel",0);prefs.edit().putBoolean("reference_glass",true).putInt("glass_opacity",78).putInt("blur",37).putInt("liquid_refraction",10000).putInt("liquid_tint",-50).commit();
        assertFalse(LiquidGlassOptions.enabled(context));assertEquals(32,LiquidGlassOptions.get(context,"liquid_refraction"));assertEquals(0,LiquidGlassOptions.get(context,"liquid_tint"));
        assertTrue(prefs.getBoolean("reference_glass",false));assertEquals(78,prefs.getInt("glass_opacity",0));assertEquals(37,prefs.getInt("blur",0));
    }
    @Test public void attributionAndFullLicenseAreBundledWithTheApp()throws Exception{
        try(var notice=context.getAssets().open("third_party/android-liquid-glass/NOTICE.txt");var license=context.getAssets().open("third_party/android-liquid-glass/LICENSE.txt")){
            assertTrue(new String(notice.readAllBytes()).contains("65ab177e90e5c1d8c62e70cf7755841982da65f6"));
            assertTrue(new String(license.readAllBytes()).contains("Apache License"));
        }
    }
}
