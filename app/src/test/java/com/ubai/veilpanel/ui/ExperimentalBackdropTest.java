package com.ubai.veilpanel.ui;
import android.graphics.*;import android.content.Context;
import com.ubai.veilpanel.WallpaperBackdropActivity;
import com.ubai.veilpanel.image.PowerSaverBackdrop;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ExperimentalBackdropTest {
 @Test public void powerSaverUsesSelectedImageWithoutDependingOnSystemBlur(){
  assertTrue(WallpaperBackdropActivity.shouldUseImage(false,true,false,true));assertTrue(WallpaperBackdropActivity.shouldUseImage(true,true,false,true));assertTrue(WallpaperBackdropActivity.shouldUseImage(true,false,true,true));assertFalse(WallpaperBackdropActivity.shouldUseImage(true,false,false,true));assertFalse(WallpaperBackdropActivity.shouldUseImage(false,true,true,false));
 }
 @Test public void softwareBlurSoftensPixelsWithoutModifyingTheSource(){
  Bitmap original=Bitmap.createBitmap(41,41,Bitmap.Config.ARGB_8888);original.eraseColor(Color.BLACK);for(int y=0;y<41;y++)for(int x=20;x<41;x++)original.setPixel(x,y,Color.WHITE);
  Bitmap blurred=PowerSaverBackdrop.blur(original,4);int left=Color.red(blurred.getPixel(18,20)),right=Color.red(blurred.getPixel(22,20));assertTrue(left>0&&left<255);assertTrue(right>left&&right<255);assertEquals(Color.BLACK,original.getPixel(18,20));assertEquals(41,blurred.getWidth());
 }
 @Test public void selectedImageIsImportedPrivatelyAndCanBeRemoved() throws Exception{
  Context context=RuntimeEnvironment.getApplication();PowerSaverBackdrop.clear(context);Bitmap source=Bitmap.createBitmap(20,30,Bitmap.Config.ARGB_8888);source.eraseColor(Color.BLUE);java.io.File original=new java.io.File(context.getCacheDir(),"source.png");try(java.io.FileOutputStream output=new java.io.FileOutputStream(original)){source.compress(Bitmap.CompressFormat.PNG,100,output);}
  PowerSaverBackdrop.importImage(context,android.net.Uri.fromFile(original));assertTrue(PowerSaverBackdrop.available(context));assertNotNull(PowerSaverBackdrop.load(context));assertTrue(original.exists());PowerSaverBackdrop.clear(context);assertFalse(PowerSaverBackdrop.available(context));assertNull(PowerSaverBackdrop.load(context));original.delete();
 }
 @Test public void savedOriginalSupportsZeroAndAdjustableBlur() throws Exception{
  Context context=RuntimeEnvironment.getApplication();PowerSaverBackdrop.clear(context);Bitmap original=Bitmap.createBitmap(41,41,Bitmap.Config.ARGB_8888);original.eraseColor(Color.BLACK);for(int y=0;y<41;y++)for(int x=20;x<41;x++)original.setPixel(x,y,Color.WHITE);
  java.io.File selected=new java.io.File(context.getCacheDir(),"adjustable.png");try(java.io.FileOutputStream output=new java.io.FileOutputStream(selected)){original.compress(Bitmap.CompressFormat.PNG,100,output);}PowerSaverBackdrop.importImage(context,android.net.Uri.fromFile(selected));
  assertTrue(PowerSaverBackdrop.adjustable(context));assertEquals(Color.BLACK,PowerSaverBackdrop.loadAtBlur(context,0).getPixel(18,20));int soft=Color.red(PowerSaverBackdrop.loadAtBlur(context,12).getPixel(18,20));assertTrue(soft>0&&soft<255);assertSame(PowerSaverBackdrop.loadAtBlur(context,12),PowerSaverBackdrop.loadAtBlur(context,12));assertEquals(Color.BLACK,PowerSaverBackdrop.loadAtBlur(context,0).getPixel(18,20));PowerSaverBackdrop.clear(context);assertFalse(PowerSaverBackdrop.adjustable(context));selected.delete();
 }
 @Test public void experimentsDefaultOffAndDoNotResetExistingPreferences(){
  Context context=RuntimeEnvironment.getApplication();var prefs=context.getSharedPreferences("veil_panel",0);prefs.edit().clear().putInt("glass_opacity",72).putInt("icon_size",44).commit();
  assertFalse(ExperimentalGlass.enabled(context));prefs.edit().putBoolean("experimental_glass",true).putBoolean("experimental_motion",true).commit();assertTrue(ExperimentalGlass.enabled(context));prefs.edit().putBoolean("experimental_glass",false).putBoolean("experimental_motion",false).commit();assertFalse(ExperimentalGlass.enabled(context));assertEquals(72,prefs.getInt("glass_opacity",0));assertEquals(44,prefs.getInt("icon_size",0));prefs.edit().clear().commit();
 }
 @Test public void edgeResistanceHasNoHardCapDiscontinuity(){assertEquals(0,ElasticPanelScrollView.resist(0,48),.001f);assertTrue(ElasticPanelScrollView.resist(400,48)<48);assertTrue(ElasticPanelScrollView.resist(50,48)>ElasticPanelScrollView.resist(49,48));assertEquals(-ElasticPanelScrollView.resist(50,48),ElasticPanelScrollView.resist(-50,48),.001f);}
}
