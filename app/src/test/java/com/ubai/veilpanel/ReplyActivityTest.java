package com.ubai.veilpanel;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.ubai.veilpanel.ui.PanelStyle;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class ReplyActivityTest {
 private Context context;private PendingIntent pending;
 @Before public void setup(){
  context=RuntimeEnvironment.getApplication();Shadows.shadowOf(context.getSystemService(KeyguardManager.class)).setIsDeviceLocked(false);
  context.getSharedPreferences("veil_panel",0).edit().clear().commit();
  pending=PendingIntent.getBroadcast(context,77,new Intent("test.REPLY").setPackage(context.getPackageName()),PendingIntent.FLAG_MUTABLE);
 }
 private Intent intent(){
  Notification.Action action=new Notification.Action.Builder(null,"Reply",pending).addRemoteInput(new RemoteInput.Builder("message").setAllowFreeFormInput(true).build()).build();
  return new Intent(context,ReplyActivity.class).putExtra("action",action).putExtra("app","Example chat").putExtra("conversation","Example conversation").putExtra("message","A sample message");
 }
 private <T extends View>T view(ReplyActivity activity,String tag){return activity.getWindow().getDecorView().findViewWithTag(tag);}
 @Test public void replyIsAPrivateFloatingSheetWithContextAndPanelTypography(){
  context.getSharedPreferences("veil_panel",0).edit().putString("font_family","serif").commit();
  try(var controller=Robolectric.buildActivity(ReplyActivity.class,intent()).setup()){
   ReplyActivity activity=controller.get();assertTrue(activity.getWindow().isFloating());assertTrue((activity.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);
   assertEquals(WindowManager.LayoutParams.WRAP_CONTENT,activity.getWindow().getAttributes().height);assertTrue(activity.getWindow().getAttributes().width>0);
   assertEquals("Example conversation",this.<TextView>view(activity,"reply_title").getText().toString());assertEquals("A sample message",this.<TextView>view(activity,"reply_preview").getText().toString());
   EditText editor=view(activity,"reply_input");assertFalse(editor.isSaveEnabled());assertEquals(PanelStyle.font(activity),editor.getTypeface());assertFalse(this.<Button>view(activity,"reply_send").isEnabled());
  }
 }
 @Test public void outerReplyUsesSavedNotificationRadiusAndMinimalSendByDefault(){
  context.getSharedPreferences("veil_panel",0).edit().putInt("card_radius",13).commit();
  try(var controller=Robolectric.buildActivity(ReplyActivity.class,intent()).setup()){
   ReplyActivity activity=controller.get();android.graphics.drawable.GradientDrawable background=(android.graphics.drawable.GradientDrawable)view(activity,"reply_sheet").getBackground();assertEquals(13*context.getResources().getDisplayMetrics().density,background.getCornerRadius(),.01f);
   android.graphics.drawable.RippleDrawable button=(android.graphics.drawable.RippleDrawable)view(activity,"reply_send").getBackground();assertEquals(android.graphics.Color.TRANSPARENT,((android.graphics.drawable.GradientDrawable)button.getDrawable(0)).getColor().getDefaultColor());
   assertNull(view(activity,"reply_preview").getBackground());
  }
 }
 @Test public void replyControlsAllowFilledButtonHiddenPreviewAndCompactSpacing(){
  context.getSharedPreferences("veil_panel",0).edit().putString("reply_send_style","filled").putBoolean("reply_show_cancel",false).putBoolean("reply_show_preview",false).putInt("reply_text_size",19).putInt("reply_spacing",8).commit();
  try(var controller=Robolectric.buildActivity(ReplyActivity.class,intent()).setup()){
   ReplyActivity activity=controller.get();assertEquals(View.GONE,view(activity,"reply_cancel").getVisibility());assertNull(view(activity,"reply_preview"));
   assertEquals(19*context.getResources().getDisplayMetrics().scaledDensity,this.<EditText>view(activity,"reply_input").getTextSize(),.1f);assertEquals(Math.round(8*context.getResources().getDisplayMetrics().density),((ScrollView)view(activity,"reply_sheet")).getChildAt(0).getPaddingLeft());
   android.graphics.drawable.RippleDrawable button=(android.graphics.drawable.RippleDrawable)view(activity,"reply_send").getBackground();assertEquals(com.ubai.veilpanel.ui.PanOSPTheme.accent(activity)|0xff000000,((android.graphics.drawable.GradientDrawable)button.getDrawable(0)).getColor().getDefaultColor());
  }
 }
 @Test public void explicitSendCarriesReplyAndClosesWithoutSavingTheDraft(){
  try(var controller=Robolectric.buildActivity(ReplyActivity.class,intent()).setup()){
   ReplyActivity activity=controller.get();EditText input=view(activity,"reply_input");input.setText("A test reply");Button send=view(activity,"reply_send");assertTrue(send.isEnabled());send.performClick();
   Intent sent=Shadows.shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents().get(0);assertEquals("A test reply",RemoteInput.getResultsFromIntent(sent).getCharSequence("message").toString());assertTrue(activity.isFinishing());assertEquals("",input.getText().toString());
  }
 }
 @Test public void whitespaceAndCancelNeverSend(){
  try(var controller=Robolectric.buildActivity(ReplyActivity.class,intent()).setup()){
   ReplyActivity activity=controller.get();this.<EditText>view(activity,"reply_input").setText(" \n\u00a0");assertFalse(this.<Button>view(activity,"reply_send").isEnabled());view(activity,"reply_send").performClick();view(activity,"reply_cancel").performClick();assertTrue(activity.isFinishing());assertTrue(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents().isEmpty());
  }
 }
 @Test public void expiredActionKeepsTheDraftAndShowsAnInlineError(){
  Intent intent=intent();pending.cancel();
  try(var controller=Robolectric.buildActivity(ReplyActivity.class,intent).setup()){
   ReplyActivity activity=controller.get();EditText editor=view(activity,"reply_input");editor.setText("Keep this draft");view(activity,"reply_send").performClick();assertFalse(activity.isFinishing());assertEquals("Keep this draft",editor.getText().toString());assertEquals(View.VISIBLE,view(activity,"reply_error").getVisibility());assertTrue(this.<Button>view(activity,"reply_send").isEnabled());
  }
 }
 @Test public void lockedDeviceCannotDisplayOrSendReply(){
  Shadows.shadowOf(context.getSystemService(KeyguardManager.class)).setIsDeviceLocked(true);
  try(var controller=Robolectric.buildActivity(ReplyActivity.class,intent()).setup()){assertTrue(controller.get().isFinishing());assertNull(view(controller.get(),"reply_input"));}
  Shadows.shadowOf(context.getSystemService(KeyguardManager.class)).setIsDeviceLocked(false);
  try(var controller=Robolectric.buildActivity(ReplyActivity.class,intent()).setup()){
   ReplyActivity activity=controller.get();this.<EditText>view(activity,"reply_input").setText("Do not send");Shadows.shadowOf(context.getSystemService(KeyguardManager.class)).setIsDeviceLocked(true);view(activity,"reply_send").performClick();assertTrue(activity.isFinishing());assertTrue(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents().isEmpty());
  }
 }
 @Test public void draftIsNotRestoredAfterRecreationAndSheetCanScrollInLimitedSpace(){
  Intent intent=intent();Bundle state=new Bundle();
  try(var controller=Robolectric.buildActivity(ReplyActivity.class,intent).setup()){
   ReplyActivity activity=controller.get();this.<EditText>view(activity,"reply_input").setText("Private draft");controller.saveInstanceState(state);
   ScrollView sheet=view(activity,"reply_sheet");sheet.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(180,View.MeasureSpec.AT_MOST));sheet.layout(0,0,sheet.getMeasuredWidth(),sheet.getMeasuredHeight());assertTrue(sheet.getMeasuredHeight()<=180);assertTrue(sheet.canScrollVertically(1));
  }
  try(var controller=Robolectric.buildActivity(ReplyActivity.class,intent).setup(state)){assertEquals("",this.<EditText>view(controller.get(),"reply_input").getText().toString());}
 }
}
