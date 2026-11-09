package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

/** Contrast protection for translucent materials. Sampling happens once per loaded wallpaper. */
final class GlassContrast {
 private GlassContrast(){}
 static int brightest(Bitmap image){
  int red=0,green=0,blue=0;
  for(int y=0;y<32;y++)for(int x=0;x<32;x++){
   int color=image.getPixel(x*(image.getWidth()-1)/31,y*(image.getHeight()-1)/31);
   red=Math.max(red,Color.red(color));green=Math.max(green,Color.green(color));blue=Math.max(blue,Color.blue(color));
  }
  return Color.rgb(red,green,blue);
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
 static int safeOpacity(Context context,int requested){return Math.max(requested,(int)Math.ceil(cardTint(context,Color.WHITE)*100));}
 static int wallpaperScrim(Context context,Bitmap wallpaper){
  return Math.max(51,(int)Math.ceil(255*requiredTint(brightest(wallpaper),Color.BLACK,PanOSPTheme.primaryText(context))));
 }
}
