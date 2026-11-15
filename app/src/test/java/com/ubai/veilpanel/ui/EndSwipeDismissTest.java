package com.ubai.veilpanel.ui;

import android.content.Context;
import android.view.*;
import android.widget.ScrollView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class EndSwipeDismissTest {
 private Context context;private ElasticPanelScrollView list;private int closes;private long time;
 @Before public void setup(){
  context=RuntimeEnvironment.getApplication();Shadows.shadowOf(context.getSystemService(android.os.VibratorManager.class).getDefaultVibrator()).setHasVibrator(true);android.provider.Settings.System.putInt(context.getContentResolver(),android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED,1);context.getSharedPreferences("veil_panel",0).edit().clear().putBoolean("swipe_up_at_end",true).commit();
  list=new ElasticPanelScrollView(context);View child=new View(context);child.setMinimumHeight(1700);
  list.addView(child,new ScrollView.LayoutParams(500,1700));list.setOnEndSwipeDismiss(()->closes++);
  list.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));list.layout(0,0,500,700);
 }
 private void send(int action,float x,float y){MotionEvent event=MotionEvent.obtain(0,time+=50,action,x,y,0);list.dispatchTouchEvent(event);event.recycle();}
 private void swipe(){send(MotionEvent.ACTION_DOWN,200,600);send(MotionEvent.ACTION_MOVE,200,450);send(MotionEvent.ACTION_UP,200,450);}
 @Test public void reachingBottomDuringASwipeNeverClosesButAFreshSwipeDoes(){
  send(MotionEvent.ACTION_DOWN,200,600);list.scrollTo(0,1000);send(MotionEvent.ACTION_MOVE,200,450);send(MotionEvent.ACTION_UP,200,450);assertEquals(0,closes);
  swipe();assertEquals(1,closes);assertTrue(Shadows.shadowOf(context.getSystemService(android.os.VibratorManager.class).getDefaultVibrator()).isVibrating());
 }
 @Test public void disabledByDefaultAndFlingingToTheEndNeverCloses(){
  context.getSharedPreferences("veil_panel",0).edit().remove("swipe_up_at_end").commit();list.scrollTo(0,1000);swipe();assertEquals(0,closes);
  context.getSharedPreferences("veil_panel",0).edit().putBoolean("swipe_up_at_end",true).commit();list.fling(5000);assertEquals(0,closes);
 }
 @Test public void cancelReverseAndMultitouchNeverClose(){
  list.scrollTo(0,1000);send(MotionEvent.ACTION_DOWN,200,600);send(MotionEvent.ACTION_MOVE,200,450);send(MotionEvent.ACTION_CANCEL,200,450);assertEquals(0,closes);
  send(MotionEvent.ACTION_DOWN,200,600);send(MotionEvent.ACTION_MOVE,200,450);send(MotionEvent.ACTION_MOVE,200,580);send(MotionEvent.ACTION_UP,200,580);assertEquals(0,closes);
  send(MotionEvent.ACTION_DOWN,200,600);send(MotionEvent.ACTION_MOVE,200,450);send(MotionEvent.ACTION_POINTER_DOWN,200,450);send(MotionEvent.ACTION_UP,200,450);assertEquals(0,closes);
 }
 @Test public void horizontalSwipeAndScrollingAwayFromBottomAreNotDismissGestures(){
  list.scrollTo(0,1000);send(MotionEvent.ACTION_DOWN,200,600);send(MotionEvent.ACTION_MOVE,350,590);send(MotionEvent.ACTION_MOVE,350,400);send(MotionEvent.ACTION_UP,350,400);assertEquals(0,closes);
  list.scrollTo(0,1000);send(MotionEvent.ACTION_DOWN,200,450);send(MotionEvent.ACTION_MOVE,200,550);send(MotionEvent.ACTION_MOVE,200,200);send(MotionEvent.ACTION_UP,200,200);assertEquals(0,closes);
 }
 @Test public void respectsChildGestureOwnershipAndCanBeDisabledMidGesture(){
  list.scrollTo(0,1000);send(MotionEvent.ACTION_DOWN,200,600);list.requestDisallowInterceptTouchEvent(true);send(MotionEvent.ACTION_MOVE,200,450);send(MotionEvent.ACTION_UP,200,450);assertEquals(0,closes);
  send(MotionEvent.ACTION_DOWN,200,600);send(MotionEvent.ACTION_MOVE,200,450);context.getSharedPreferences("veil_panel",0).edit().putBoolean("swipe_up_at_end",false).commit();send(MotionEvent.ACTION_UP,200,450);assertEquals(0,closes);
 }
 @Test public void worksWithBounceAndWithAListThatFitsOnScreen(){
  context.getSharedPreferences("veil_panel",0).edit().putBoolean("experimental_motion",true).commit();list.scrollTo(0,1000);swipe();assertEquals(1,closes);assertEquals(0,list.getChildAt(0).getTranslationY(),0);
  list.getChildAt(0).setMinimumHeight(200);list.getChildAt(0).setLayoutParams(new ScrollView.LayoutParams(500,200));list.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));list.layout(0,0,500,700);list.scrollTo(0,0);swipe();assertEquals(2,closes);
 }
}
