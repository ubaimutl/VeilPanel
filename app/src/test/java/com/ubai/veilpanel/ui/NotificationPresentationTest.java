package com.ubai.veilpanel.ui;
import android.app.*;import android.content.Context;import android.graphics.*;import android.service.notification.StatusBarNotification;import android.view.*;import android.widget.*;
import com.ubai.veilpanel.notifications.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class NotificationPresentationTest {
 private final Context context=RuntimeEnvironment.getApplication();
 @Before public void setup(){context.getSharedPreferences("veil_panel",0).edit().clear().commit();}
 @After public void clear(){com.ubai.veilpanel.image.PowerSaverBackdrop.clear(context);NotificationRepository.getInstance().replaceAll(new StatusBarNotification[0]);}
 private StatusBarNotification alert(int id,Notification n){return new StatusBarNotification(context.getPackageName(),context.getPackageName(),id,null,10001,0,0,n,android.os.Process.myUserHandle(),id);}
 private View transition(View view,String name){if(name.equals(view.getTransitionName()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=transition(((ViewGroup)view).getChildAt(i),name);if(found!=null)return found;}return null;}
 private TextView text(View view,String value){if(view instanceof TextView&&value.contentEquals(((TextView)view).getText()))return (TextView)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){TextView found=text(((ViewGroup)view).getChildAt(i),value);if(found!=null)return found;}return null;}
 @Test public void stackShowsTwoCardsForTwoNotificationsAndCapsAtThree(){
  for(int count:new int[]{2,3,5}){
   StatusBarNotification[] notifications=new StatusBarNotification[count];for(int i=0;i<count;i++)notifications[i]=alert(i,new Notification.Builder(context,"test").setContentTitle("Item "+i).setContentText("Message").build());NotificationRepository.getInstance().replaceAll(notifications,context);
   ShadeView view=new ShadeView(context,()->{});view.onShown();try{
    ViewGroup stack=(ViewGroup)transition(view,"stack:0:"+context.getPackageName());assertNotNull(stack);assertEquals(Math.min(3,count),stack.getChildCount());view.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2000,View.MeasureSpec.AT_MOST));view.layout(0,0,400,view.getMeasuredHeight());assertEquals(Math.min(3,count),stack.getChildCount());
   }finally{view.onHidden();}
  }
 }
 @Test public void conversationCanExpandToShowEarlierMessagesInsteadOfOnlyTheLastOne(){
  Person sender=new Person.Builder().setName("Alice").build();Notification n=new Notification.Builder(context,"test").setStyle(new Notification.MessagingStyle(new Person.Builder().setName("Me").build()).addMessage("first",1,sender).addMessage("second",2,sender).addMessage("latest",3,sender)).build();StatusBarNotification notification=alert(1,n);NotificationRepository.getInstance().replaceAll(new StatusBarNotification[]{notification},context);
  ShadeView view=new ShadeView(context,()->{});view.onShown();try{assertNotNull(text(view,"second\nlatest"));View card=transition(view,"notification:"+notification.getKey());card.performLongClick();assertNotNull(text(view,"first\nsecond\nlatest"));}finally{view.onHidden();}
 }
 @Test public void plainOpacityUsesTheSavedWallpaperInsteadOfClampingEveryValueToOpaque(){
  TestWallpaper.install(context);assertEquals(45,GlassContrast.safeOpacity(context,45));context.getSharedPreferences("veil_panel",0).edit().putInt("glass_opacity",45).commit();android.graphics.drawable.GradientDrawable low=(android.graphics.drawable.GradientDrawable)NotificationCardSurface.create(context);context.getSharedPreferences("veil_panel",0).edit().putInt("glass_opacity",100).commit();android.graphics.drawable.GradientDrawable high=(android.graphics.drawable.GradientDrawable)NotificationCardSurface.create(context);assertTrue(Color.alpha(low.getColor().getDefaultColor())<Color.alpha(high.getColor().getDefaultColor()));
 }
 @Test public void cardOpacityAllowsZeroAndVeryLowValues(){
  assertEquals(0,GlassContrast.safeOpacity(context,0));assertEquals(10,GlassContrast.safeOpacity(context,10));
  context.getSharedPreferences("veil_panel",0).edit().putInt("glass_opacity",0).commit();
  android.graphics.drawable.GradientDrawable background=(android.graphics.drawable.GradientDrawable)NotificationCardSurface.create(context);
  assertEquals(0,Color.alpha(background.getColor().getDefaultColor()));
 }
 @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 public void zeroOpacityRemovesTheFillForEveryNonLiquidMaterial(){
  var prefs=context.getSharedPreferences("veil_panel",0);
  for(String style:new String[]{"plain","reference_glass","experimental_glass"}){
   prefs.edit().putInt("glass_opacity",0).putBoolean("reference_glass",style.equals("reference_glass")).putBoolean("experimental_glass",style.equals("experimental_glass")).commit();
   var material=NotificationCardSurface.create(context);material.setBounds(0,0,200,120);Bitmap image=Bitmap.createBitmap(200,120,Bitmap.Config.ARGB_8888);material.draw(new Canvas(image));assertEquals(style,0,Color.alpha(image.getPixel(100,60)));image.recycle();
  }
 }
 @Test public void cardBlurUsesItsOwnMaterialWithoutLiquidGlass(){
  context.getSharedPreferences("veil_panel",0).edit().putInt("card_blur",32).putInt("liquid_blur",0).putBoolean("kyant_liquid_glass",false).commit();
  StatusBarNotification n=alert(1,new Notification.Builder(context,"test").setContentTitle("blurred").build());NotificationRepository.getInstance().replaceAll(new StatusBarNotification[]{n},context);
  ShadeView view=new ShadeView(context,()->{});view.onShown();try{assertTrue(transition(view,"notification:"+n.getKey()).getBackground() instanceof CardBlurDrawable);}finally{view.onHidden();}
 }
 @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 public void cardBlurDrawsWallpaperInsideCardEvenAtZeroTint() throws Exception {
  context.getSharedPreferences("veil_panel",0).edit().putInt("glass_opacity",0).commit();
  View card=new View(context);card.layout(0,0,200,120);LiquidGlassBackdrop backdrop=new LiquidGlassBackdrop(card,"card_blur");
  Bitmap wallpaper=Bitmap.createBitmap(16,24,Bitmap.Config.ARGB_8888);wallpaper.eraseColor(Color.RED);
  java.lang.reflect.Field field=LiquidGlassBackdrop.class.getDeclaredField("bitmap");field.setAccessible(true);field.set(backdrop,wallpaper);
  CardBlurDrawable drawable=new CardBlurDrawable(context,card,backdrop,NotificationCardSurface.create(context));drawable.setBounds(0,0,200,120);
  Bitmap pixels=Bitmap.createBitmap(200,120,Bitmap.Config.ARGB_8888);drawable.draw(new Canvas(pixels));assertEquals(255,Color.alpha(pixels.getPixel(100,60)));assertTrue(Color.red(pixels.getPixel(100,60))>0);assertEquals(0,Color.alpha(pixels.getPixel(0,0)));
  wallpaper.recycle();pixels.recycle();
 }
 @Test public void selectedWallpaperIsNotCoveredWhenWallpaperColoursAreDisabled(){
  TestWallpaper.install(context);context.getSharedPreferences("veil_panel",0).edit().putBoolean("dynamic",false).commit();ShadeView view=new ShadeView(context,()->{});assertEquals(Color.TRANSPARENT,((android.graphics.drawable.ColorDrawable)view.getBackground()).getColor());
 }
 @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 public void rearStackLayersDoNotFillTheTransparentCentreOfTheFrontCard(){
  StatusBarNotification[] notifications={alert(1,new Notification.Builder(context,"test").setContentTitle("one").build()),alert(2,new Notification.Builder(context,"test").setContentTitle("two").build())};NotificationRepository.getInstance().replaceAll(notifications,context);ShadeView view=new ShadeView(context,()->{});view.onShown();try{
   ViewGroup stack=(ViewGroup)transition(view,"stack:0:"+context.getPackageName());View back=stack.getChildAt(0);back.measure(View.MeasureSpec.makeMeasureSpec(200,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(120,View.MeasureSpec.EXACTLY));back.layout(0,0,200,120);Bitmap pixels=Bitmap.createBitmap(200,120,Bitmap.Config.ARGB_8888);back.draw(new Canvas(pixels));assertEquals(0,Color.alpha(pixels.getPixel(100,50)));assertTrue(Color.alpha(pixels.getPixel(100,119))>0);pixels.recycle();
  }finally{view.onHidden();}
 }
 @Test public void brightWallpaperReceivesEnoughMaterialTintForAllNotificationText(){
  float tint=GlassContrast.cardTint(context,Color.WHITE);int background=PanOSPTheme.blend(Color.WHITE,PanOSPTheme.notificationBackground(context),tint);assertTrue(GlassContrast.ratio(PanOSPTheme.notificationPrimaryText(context),background)>=4.5);assertTrue(GlassContrast.ratio(PanOSPTheme.notificationSecondaryText(context),background)>=4.5);assertTrue(GlassContrast.ratio(PanOSPTheme.notificationDescriptionText(context),background)>=4.5);
  Bitmap wallpaper=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);wallpaper.eraseColor(Color.WHITE);int alpha=GlassContrast.wallpaperScrim(context,wallpaper);assertTrue(GlassContrast.ratio(PanOSPTheme.primaryText(context),PanOSPTheme.blend(Color.WHITE,Color.BLACK,alpha/255f))>=4.5);wallpaper.eraseColor(Color.BLACK);assertEquals(51,GlassContrast.wallpaperScrim(context,wallpaper));wallpaper.recycle();
 }
}
