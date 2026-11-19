package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Blurred saved wallpaper clipped to a card; text and controls stay sharp. */
final class CardBlurDrawable extends Drawable {
 private final View card;private final LiquidGlassBackdrop backdrop;private final Drawable material;
 private final Paint image=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG),scrim=new Paint();
 private final Matrix matrix=new Matrix();private final RectF shape=new RectF();private final Path clip=new Path();
 private final int[] location=new int[2],origin=new int[2];private final float radius;
 private int scrimAlpha;private Bitmap bitmap;private BitmapShader shader;private int alpha=255;
 CardBlurDrawable(Context context,View card,LiquidGlassBackdrop backdrop,Drawable material){
  this.card=card;this.backdrop=backdrop;this.material=material;radius=NotificationCardSurface.radius(context);
 }
 @Override protected void onBoundsChange(Rect bounds){shape.set(bounds);clip.reset();clip.addRoundRect(shape,radius,radius,Path.Direction.CW);material.setBounds(bounds);}
 @Override public void draw(Canvas canvas){
  Bitmap source=backdrop.bitmap();
  if(source!=null&&!source.isRecycled()){
   if(bitmap!=source){bitmap=source;shader=new BitmapShader(source,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);image.setShader(shader);scrimAlpha=GlassContrast.wallpaperScrim(card.getContext(),source);scrim.setColor(Color.BLACK);}
   View root=card.getRootView();card.getLocationOnScreen(location);root.getLocationOnScreen(origin);
   LiquidGlassDrawable.wallpaperMatrix(matrix,source.getWidth(),source.getHeight(),Math.max(1,root.getWidth()),Math.max(1,root.getHeight()),location[0]-origin[0],location[1]-origin[1]);shader.setLocalMatrix(matrix);
   int save=canvas.save();canvas.clipPath(clip);image.setAlpha(alpha);scrim.setAlpha(Math.round(scrimAlpha*alpha/255f));canvas.drawRect(shape,image);canvas.drawRect(shape,scrim);canvas.restoreToCount(save);
  }
  material.draw(canvas);
 }
 @Override public void setAlpha(int value){alpha=value;material.setAlpha(value);invalidateSelf();}
 @Override public void setColorFilter(ColorFilter filter){image.setColorFilter(filter);material.setColorFilter(filter);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
