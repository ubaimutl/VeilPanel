package com.ubai.veilpanel.ui;
import android.view.*;import android.widget.*;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class NotificationActionRowTest {
 @Test public void fiveActionsWrapCompactlyAndKeepIndependentClickTargets(){
  var context=RuntimeEnvironment.getApplication();NotificationActionRow row=new NotificationActionRow(context);AtomicInteger clicked=new AtomicInteger();
  for(String text:new String[]{"Archive","Mark as read","Reply","Open in app","Settings"})row.addAction(text,text,false,clicked::incrementAndGet);
  int width=Math.round(280*context.getResources().getDisplayMetrics().density);row.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.AT_MOST));row.layout(0,0,width,row.getMeasuredHeight());
  float density=context.getResources().getDisplayMetrics().density;assertTrue(row.getMeasuredHeight()<=140*density);assertEquals(row.getChildAt(0).getTop(),row.getChildAt(1).getTop());
  for(int i=0;i<5;i++){View action=row.getChildAt(i);assertTrue(action.getMeasuredHeight()>=44*density);assertTrue(action.getRight()<=width);assertEquals(android.widget.Button.class.getName(),action.getAccessibilityClassName());action.performClick();}assertEquals(5,clicked.get());
 }
}
