package com.ubai.veilpanel.ui;
final class TestWallpaper {
 static void install(android.content.Context context){
  com.ubai.veilpanel.image.PowerSaverBackdrop.clear(context);
  android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(16,24,android.graphics.Bitmap.Config.ARGB_8888);image.eraseColor(android.graphics.Color.DKGRAY);
  try(java.io.FileOutputStream stream=new java.io.FileOutputStream(new java.io.File(context.getFilesDir(),"power-saver-backdrop.png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}catch(java.io.IOException error){throw new AssertionError(error);}
 }
}
