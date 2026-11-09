package com.ubai.veilpanel.ui;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Reference-inspired frosted tint, soft inner shading and a thin lit rim. */
public final class ReferenceGlassDrawable extends Drawable {
 private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG),rim=new Paint(Paint.ANTI_ALIAS_FLAG),inner=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final RectF shape=new RectF(),inside=new RectF();
 private final float radius,density;private final int base,opacity;private int alpha=255;
 public ReferenceGlassDrawable(Context context,float radius,int opacity){
  this.radius=radius;density=context.getResources().getDisplayMetrics().density;
  int palette=PanOSPTheme.notificationBackground(context);boolean dark=Color.luminance(palette)<.4;
  int neutral=dark?0xFF414141:0xFFEAEAEA;
  base=blend(neutral,palette,.12f);this.opacity=GlassContrast.safeOpacity(context,Math.max(45,Math.min(100,opacity)));
 }
 private static int blend(int a,int b,float t){return Color.rgb(Math.round(Color.red(a)*(1-t)+Color.red(b)*t),Math.round(Color.green(a)*(1-t)+Color.green(b)*t),Math.round(Color.blue(a)*(1-t)+Color.blue(b)*t));}
 private static int withAlpha(int color,int a){return (color&0xFFFFFF)|(Math.max(0,Math.min(255,a))<<24);}
 @Override protected void onBoundsChange(Rect bounds){
  shape.set(bounds);float inset=.5f*density;shape.inset(inset,inset);inside.set(shape);inside.inset(density,density);
  int a=Math.min(255,Math.round(opacity*2.55f)+8);
  fill.setShader(new LinearGradient(0,shape.top,0,shape.bottom,new int[]{withAlpha(blend(base,Color.WHITE,.08f),a),withAlpha(base,a),withAlpha(blend(base,Color.BLACK,.05f),a)},new float[]{0,.42f,1},Shader.TileMode.CLAMP));
  rim.setStyle(Paint.Style.STROKE);rim.setStrokeWidth(density);
  rim.setShader(new LinearGradient(0,shape.top,0,shape.bottom,new int[]{0x72FFFFFF,0x1CFFFFFF,0x14FFFFFF,0x46FFFFFF},new float[]{0,.18f,.76f,1},Shader.TileMode.CLAMP));
  inner.setStyle(Paint.Style.STROKE);inner.setStrokeWidth(density);
  inner.setShader(new LinearGradient(0,inside.top,0,inside.bottom,new int[]{0x16FFFFFF,0x00FFFFFF,0x20000000},new float[]{0,.28f,1},Shader.TileMode.CLAMP));
 }
 @Override public void draw(Canvas canvas){fill.setAlpha(alpha);rim.setAlpha(alpha);inner.setAlpha(alpha);canvas.drawRoundRect(shape,radius,radius,fill);canvas.drawRoundRect(shape,radius,radius,rim);canvas.drawRoundRect(inside,Math.max(0,radius-density),Math.max(0,radius-density),inner);}
 @Override public void setAlpha(int a){alpha=a;invalidateSelf();}
 @Override public void setColorFilter(ColorFilter f){fill.setColorFilter(f);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
