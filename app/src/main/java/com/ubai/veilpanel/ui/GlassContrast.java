package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

/** Contrast protection for translucent materials. Sampling happens once per loaded wallpaper. */
final class GlassContrast {
 private GlassContrast(){}
 private static final java.util.WeakHashMap<Bitmap,Integer> samples=new java.util.WeakHashMap<>();
 static synchronized int brightest(Bitmap image){
  // Imported/decoded originals are immutable. Caller-editable images must be resampled.
  Integer sample=image.isMutable()?null:samples.get(image);if(sample!=null)return sample;
  int red=0,green=0,blue=0;
  for(int y=0;y<32;y++)for(int x=0;x<32;x++){
   int color=image.getPixel(x*(image.getWidth()-1)/31,y*(image.getHeight()-1)/31);
   red=Math.max(red,Color.red(color));green=Math.max(green,Color.green(color));blue=Math.max(blue,Color.blue(color));
  }
  int result=Color.rgb(red,green,blue);if(!image.isMutable())samples.put(image,result);return result;
 }
 static double ratio(int a,int b){double x=Color.luminance(a|0xff000000),y=Color.luminance(b|0xff000000);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
 static float requiredTint(int source,int tint,int... foregrounds){
  for(int alpha=0;alpha<=255;alpha++){
   int background=PanOSPTheme.blend(source,tint,alpha/255f);boolean readable=true;
   for(int foreground:foregrounds)if(ratio(foreground,background)<4.5){readable=false;break;}
   if(readable)return alpha/255f;
  }
  return 1;
 }
 static float cardTint(Context context,int source){
  return requiredTint(source,PanOSPTheme.notificationBackground(context),PanOSPTheme.notificationPrimaryText(context),PanOSPTheme.notificationSecondaryText(context),PanOSPTheme.notificationDescriptionText(context));
 }
 private static int renderedWallpaper(Context context){
  Bitmap image=com.ubai.veilpanel.image.PowerSaverBackdrop.peekSource();int source=image==null||image.isRecycled()?Color.WHITE:brightest(image);
  int scrim=Math.max(51,(int)Math.ceil(255*requiredTint(source,Color.BLACK,PanOSPTheme.primaryText(context))));
  return PanOSPTheme.blend(source,Color.BLACK,scrim/255f);
 }
 static int textBackground(Context context){
  // Include the frosted material's lightest fill, rather than the thin border.
  int material=PanOSPTheme.blend(PanOSPTheme.notificationBackground(context),Color.WHITE,.16f);
  if(!com.ubai.veilpanel.image.PowerSaverBackdrop.available(context))return material;
  int wallpaper=renderedWallpaper(context);return Color.luminance(wallpaper)>Color.luminance(material)?wallpaper:material;
 }
 static int safeOpacity(Context context,int requested){
  return Math.max(0,Math.min(100,requested));
 }
 static int wallpaperScrim(Context context,Bitmap wallpaper){
  return Math.max(51,(int)Math.ceil(255*requiredTint(brightest(wallpaper),Color.BLACK,PanOSPTheme.primaryText(context))));
 }
}
