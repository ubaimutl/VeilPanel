package com.ubai.veilpanel.image;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import java.io.*;
/** User-selected image only. No wallpaper-file, storage or screenshot permission. */
public final class PowerSaverBackdrop {
 private PowerSaverBackdrop(){}
 private static volatile Bitmap cached;
 private static Bitmap source,adjusted;private static int adjustedRadius=-1;
 private static File file(Context context){return new File(context.getFilesDir(),"power-saver-backdrop.png");}
 private static File sourceFile(Context context){return new File(context.getFilesDir(),"panel-wallpaper-source.png");}
 public static boolean adjustable(Context context){return sourceFile(context).isFile();}
 public static boolean available(Context context){return file(context).isFile();}
 public static synchronized void invalidateCache(){cached=null;source=null;adjusted=null;adjustedRadius=-1;}
 public static synchronized void clear(Context context){file(context).delete();sourceFile(context).delete();cached=null;source=null;adjusted=null;adjustedRadius=-1;}
 public static Bitmap load(Context context){if(cached!=null)return cached;Bitmap image=BitmapFactory.decodeFile(file(context).getPath());if(image!=null)cached=image;return image;}
 /** Runs on the image executor, never while drawing a frame. */
 public static synchronized Bitmap loadAtBlur(Context context,int strength){
  if(!adjustable(context))return load(context);
  if(source==null)source=BitmapFactory.decodeFile(sourceFile(context).getPath());
  if(source==null)return load(context);
  int radius=Math.round(Math.max(0,Math.min(64,strength))*.5f);
  if(radius==0)return source;
  if(adjusted==null||adjustedRadius!=radius){adjusted=blur(source,radius);adjustedRadius=radius;}
  return adjusted;
 }
 public static void importImage(Context context,Uri uri) throws IOException {
  Bitmap decoded=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(),uri),(decoder,info,source)->{int longest=Math.max(info.getSize().getWidth(),info.getSize().getHeight());decoder.setTargetSampleSize(Math.max(1,(longest+719)/720));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);});
  Bitmap blurred=blur(decoded,12);File temporary=new File(context.getFilesDir(),"power-saver-backdrop.tmp");
  try(FileOutputStream stream=new FileOutputStream(temporary)){if(!blurred.compress(Bitmap.CompressFormat.PNG,100,stream))throw new IOException("Image could not be saved");stream.getFD().sync();}
  File originalTemp=new File(context.getFilesDir(),"panel-wallpaper-source.tmp");
  try(FileOutputStream stream=new FileOutputStream(originalTemp)){if(!decoded.compress(Bitmap.CompressFormat.PNG,100,stream))throw new IOException("Source image could not be saved");stream.getFD().sync();}
  if(!temporary.renameTo(file(context))){temporary.delete();originalTemp.delete();throw new IOException("Image could not be stored");}
  if(!originalTemp.renameTo(sourceFile(context))){originalTemp.delete();sourceFile(context).delete();throw new IOException("Source image could not be stored");}
  synchronized(PowerSaverBackdrop.class){cached=blurred;source=decoded;adjusted=null;adjustedRadius=-1;}
 }
 public static Bitmap blur(Bitmap input,int radius){
  int width=input.getWidth(),height=input.getHeight();int[] pixels=new int[width*height],temp=new int[pixels.length];input.getPixels(pixels,0,width,0,0,width,height);
  for(int pass=0;pass<3;pass++){box(pixels,temp,width,height,Math.max(1,radius),true);box(temp,pixels,width,height,Math.max(1,radius),false);}
  return Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888);
 }
 private static void box(int[] source,int[] target,int width,int height,int radius,boolean horizontal){
  int lines=horizontal?height:width,length=horizontal?width:height;
  for(int line=0;line<lines;line++){
   long red=0,green=0,blue=0;int count=2*radius+1;
   for(int k=-radius;k<=radius;k++){int position=Math.max(0,Math.min(length-1,k)),color=source[horizontal?line*width+position:position*width+line];red+=(color>>16)&255;green+=(color>>8)&255;blue+=color&255;}
   for(int p=0;p<length;p++){
    target[horizontal?line*width+p:p*width+line]=Color.rgb((int)(red/count),(int)(green/count),(int)(blue/count));
    int left=Math.max(0,p-radius),right=Math.min(length-1,p+radius+1);
    int a=source[horizontal?line*width+left:left*width+line],b=source[horizontal?line*width+right:right*width+line];red+=((b>>16)&255)-((a>>16)&255);green+=((b>>8)&255)-((a>>8)&255);blue+=(b&255)-(a&255);
   }
  }
 }
}
