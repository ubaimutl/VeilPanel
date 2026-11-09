package com.ubai.veilpanel.notifications;
import android.app.*;
import android.content.Context;
import android.os.*;
import android.service.notification.StatusBarNotification;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class MessageContentTest {
 private final Context context=RuntimeEnvironment.getApplication();
 private NotificationEntry entry(Notification notification){return NotificationEntry.from(new StatusBarNotification(context.getPackageName(),context.getPackageName(),1,null,10001,0,0,notification,android.os.Process.myUserHandle(),1));}
 @Test public void oneConversationRetainsAllActiveMessagesAndSkipsHistoricMessages(){
  Person sender=new Person.Builder().setName("Alice").build();Notification.MessagingStyle style=new Notification.MessagingStyle(new Person.Builder().setName("Me").build())
   .addHistoricMessage(new Notification.MessagingStyle.Message("already read",0,sender)).addMessage("first",1,sender).addMessage("second",2,sender).addMessage("latest",3,sender);
  Notification n=new Notification.Builder(context,"test").setStyle(style).build();NotificationEntry snapshot=entry(n);
  assertEquals(3,snapshot.getMessageLines().size());assertTrue(snapshot.getMessageText(2).toString().endsWith("second\nlatest"));assertFalse(snapshot.getMessageText(Integer.MAX_VALUE).toString().contains("already read"));assertTrue(snapshot.getMessageText(Integer.MAX_VALUE).toString().contains("first"));
  n.extras.clear();assertEquals(3,snapshot.getMessageLines().size());
 }
 @Test public void groupConversationKeepsSenderNamesAndRepeatedMessages(){
  Person alice=new Person.Builder().setName("Alice").build(),bob=new Person.Builder().setName("Bob").build();Notification.MessagingStyle style=new Notification.MessagingStyle(new Person.Builder().setName("Me").build()).setConversationTitle("Team").setGroupConversation(true).addMessage("hello",1,alice).addMessage("hello",2,alice).addMessage("reply",3,bob);
  NotificationEntry snapshot=entry(new Notification.Builder(context,"test").setStyle(style).build());assertEquals("Alice: hello\nAlice: hello\nBob: reply",snapshot.getMessageText(10).toString());
 }
 @Test public void inboxLinesAreDisplayedAndMalformedMessagePayloadFallsBackToText(){
  Notification inbox=new Notification.Builder(context,"test").setStyle(new Notification.InboxStyle().addLine("item one").addLine("item two")).build();assertEquals("item one\nitem two",entry(inbox).getMessageText(10).toString());
  Notification malformed=new Notification.Builder(context,"test").setContentText("fallback").build();malformed.extras.putString(Notification.EXTRA_MESSAGES,"wrong type");NotificationEntry snapshot=entry(malformed);assertTrue(snapshot.getMessageLines().isEmpty());assertEquals("fallback",snapshot.getText());
 }
}
