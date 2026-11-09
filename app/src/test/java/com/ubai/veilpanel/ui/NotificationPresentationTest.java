package com.ubai.veilpanel.ui;
import android.app.*;import android.content.Context;import android.graphics.*;import android.service.notification.StatusBarNotification;import android.view.*;import android.widget.*;
import com.ubai.veilpanel.notifications.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class NotificationPresentationTest {
 private final Context context=RuntimeEnvironment.getApplication();
 @Before public void setup(){context.getSharedPreferences("veil_panel",0).edit().clear().commit();}
 @After public void clear(){NotificationRepository.getInstance().replaceAll(new StatusBarNotification[0]);}
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
 @Test public void brightWallpaperReceivesEnoughMaterialTintForAllNotificationText(){
  float tint=GlassContrast.cardTint(context,Color.WHITE);int background=PanOSPTheme.blend(Color.WHITE,PanOSPTheme.notificationBackground(context),tint);assertTrue(GlassContrast.ratio(PanOSPTheme.notificationPrimaryText(context),background)>=4.5);assertTrue(GlassContrast.ratio(PanOSPTheme.notificationSecondaryText(context),background)>=4.5);assertTrue(GlassContrast.ratio(PanOSPTheme.notificationDescriptionText(context),background)>=4.5);
  Bitmap wallpaper=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888);wallpaper.eraseColor(Color.WHITE);int alpha=GlassContrast.wallpaperScrim(context,wallpaper);assertTrue(GlassContrast.ratio(PanOSPTheme.primaryText(context),PanOSPTheme.blend(Color.WHITE,Color.BLACK,alpha/255f))>=4.5);wallpaper.eraseColor(Color.BLACK);assertEquals(51,GlassContrast.wallpaperScrim(context,wallpaper));wallpaper.recycle();
 }
}
