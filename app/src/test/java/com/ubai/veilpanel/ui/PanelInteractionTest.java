package com.ubai.veilpanel.ui;
import android.app.*;import android.content.*;import android.service.notification.StatusBarNotification;import android.view.*;import android.widget.*;
import com.ubai.veilpanel.notifications.NotificationRepository;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class PanelInteractionTest {
 private final Context context=RuntimeEnvironment.getApplication();
 @Before public void settings(){context.getSharedPreferences("veil_panel",0).edit().clear().commit();}
 @After public void clean(){NotificationRepository.getInstance().replaceAll(new StatusBarNotification[0]);}
 @Test public void clearAllOnlyOccupiesItsLabelAtTheEndOfTheRow(){
  Notification n=new Notification.Builder(context,"test").setContentTitle("Clear target").setContentText("Sample").build();
  NotificationRepository.getInstance().replaceAll(new StatusBarNotification[]{new StatusBarNotification(context.getPackageName(),context.getPackageName(),73,null,10001,0,0,n,android.os.Process.myUserHandle(),1)},context);
  ShadeView shade=new ShadeView(context,()->{});shade.onShown();
  try{View clear=findText(shade,context.getString(com.ubai.veilpanel.R.string.veil_clear_all));assertNotNull(clear);assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT,clear.getLayoutParams().width);assertEquals(Gravity.END,((LinearLayout.LayoutParams)clear.getLayoutParams()).gravity);assertFalse(((View)clear.getParent()).isClickable());}finally{shade.onHidden();}
 }
 @Test public void holdExpandsWithoutLaunchingAndReleaseDoesNotBecomeAClick(){
  PendingIntent launch=PendingIntent.getActivity(context,42,new Intent("sample.OPEN_NOTIFICATION"),PendingIntent.FLAG_IMMUTABLE);
  Notification n=new Notification.Builder(context,"test").setContentTitle("Sample title").setContentText("Short preview").setContentIntent(launch).setStyle(new Notification.BigTextStyle().bigText("The complete long preview is available here.")).build();
  NotificationRepository.getInstance().replaceAll(new StatusBarNotification[]{new StatusBarNotification(context.getPackageName(),context.getPackageName(),42,null,10001,0,0,n,android.os.Process.myUserHandle(),1)},context);
  ShadeView shade=new ShadeView(context,()->{});shade.onShown();
  try{
   View title=findText(shade,"Sample title");assertNotNull(title);View card=title;
   while(card.getTransitionName()==null||!card.getTransitionName().startsWith("notification:"))card=(View)card.getParent();
   assertTrue(card.performLongClick());assertNotNull(findText(shade,"The complete long preview is available here."));assertNotNull(findText(shade,"Settings"));assertSame(card,findCard(shade));
   assertFalse(card.performClick());assertNull(Shadows.shadowOf((Application)context).getNextStartedActivity());
   MotionEvent down=MotionEvent.obtain(0,1,MotionEvent.ACTION_DOWN,4,4,0);card.dispatchTouchEvent(down);down.recycle();card.performClick();
   assertNotNull(Shadows.shadowOf((Application)context).getNextStartedActivity());
  }finally{shade.onHidden();}
 }
 @Test public void edgeResistanceIsBoundedAndAnUpwardSwipeDoesNotCloseTheList(){
  context.getSharedPreferences("veil_panel",0).edit().putBoolean("experimental_motion",true).commit();
  ElasticPanelScrollView scroll=new ElasticPanelScrollView(context);View content=new View(context);scroll.addView(content,new ScrollView.LayoutParams(500,1200));scroll.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));scroll.layout(0,0,500,700);
  edge(scroll,-300,0,500,true);assertTrue(content.getTranslationY()>0);assertTrue(content.getTranslationY()<=48*context.getResources().getDisplayMetrics().density);
  assertEquals(0,scroll.getScrollY());
  content.setTranslationY(0);ReflectionHelpers.setField(scroll,"displacement",0f);ReflectionHelpers.setField(scroll,"rawPull",0f);scroll.scrollTo(0,500);edge(scroll,300,500,500,true);assertTrue(content.getTranslationY()<0);assertEquals(500,scroll.getScrollY());assertEquals(View.VISIBLE,scroll.getVisibility());
 }
 @Test public void crossingBottomConsumesTheRemainingScrollBeforeResistance(){
  context.getSharedPreferences("veil_panel",0).edit().putBoolean("experimental_motion",true).commit();
  ElasticPanelScrollView scroll=new ElasticPanelScrollView(context);View content=new View(context);scroll.addView(content,new ScrollView.LayoutParams(500,1200));scroll.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));scroll.layout(0,0,500,700);scroll.scrollTo(0,480);
  edge(scroll,40,480,500,true);assertEquals(500,scroll.getScrollY());assertTrue(content.getTranslationY()<0);assertTrue(Math.abs(content.getTranslationY())<6);
 }
 @Test public void veryLongPreviewIsBoundedAndCollapseStaysAtTheTop(){
  String body="A sample long notification line.\n".repeat(70).strip();Notification n=new Notification.Builder(context,"test").setContentTitle("Long sample").setContentIntent(PendingIntent.getActivity(context,43,new Intent("sample.OPEN_LONG"),PendingIntent.FLAG_IMMUTABLE)).setContentText("Short preview").setStyle(new Notification.BigTextStyle().bigText(body)).build();
  NotificationRepository.getInstance().replaceAll(new StatusBarNotification[]{new StatusBarNotification(context.getPackageName(),context.getPackageName(),43,null,10001,0,0,n,android.os.Process.myUserHandle(),1)},context);
  ShadeView shade=new ShadeView(context,()->{});shade.onShown();try{View card=findCard(shade);card.performLongClick();TextView expanded=(TextView)findText(shade,NotificationRepository.getInstance().snapshot().get(0).getBigText().toString());assertNotNull("Expanded Android-supplied body must remain available",expanded);assertEquals(10,expanded.getMaxLines());assertNotNull("Full content action is available",findText(shade,"Open in app"));View collapse=findDescription(shade,"Collapse notification");assertNotNull("Collapse is available",collapse);android.widget.LinearLayout.LayoutParams params=(android.widget.LinearLayout.LayoutParams)((View)collapse.getParent()).getLayoutParams();assertEquals(Gravity.TOP,params.gravity);}finally{shade.onHidden();}
 }
 @Test public void controlBordersAreIndependentAndOffByDefault(){
  var prefs=context.getSharedPreferences("veil_panel",0);prefs.edit().remove("control_borders").putBoolean("experimental_glass",true).commit();View plain=new View(context);ExperimentalGlass.styleControl(plain);assertNull(plain.getBackground());assertEquals(18,ExperimentalGlass.iconSize(context));prefs.edit().putBoolean("control_borders",true).commit();View bordered=new View(context);ExperimentalGlass.styleControl(bordered);assertTrue(bordered.getBackground() instanceof android.graphics.drawable.InsetDrawable);prefs.edit().remove("control_borders").remove("experimental_glass").commit();
 }
 @Test public void timestampsAlignWhetherOrNotTheCardHasAnExpandControl(){
  Notification big=new Notification.Builder(context,"test").setContentTitle("Expandable title").setContentText("Preview").setStyle(new Notification.BigTextStyle().bigText("Longer expanded body")).build();Notification plain=new Notification.Builder(context,"test").setContentTitle("Plain title").setContentText("Body").build();
  StatusBarNotification first=new StatusBarNotification("sample.first","sample.first",1,null,10001,0,0,big,android.os.Process.myUserHandle(),1),second=new StatusBarNotification("sample.second","sample.second",2,null,10001,0,0,plain,android.os.Process.myUserHandle(),1);NotificationRepository.getInstance().replaceAll(new StatusBarNotification[]{first,second},context);
  ShadeView shade=new ShadeView(context,()->{});shade.onShown();try{shade.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1500,View.MeasureSpec.AT_MOST));shade.layout(0,0,500,shade.getMeasuredHeight());ViewGroup a=(ViewGroup)findText(shade,"Expandable title").getParent(),b=(ViewGroup)findText(shade,"Plain title").getParent();assertEquals(a.getMeasuredWidth(),b.getMeasuredWidth());assertEquals(a.getChildAt(1).getRight(),b.getChildAt(1).getRight());}finally{shade.onHidden();}
 }
 private View findDescription(View view,String description){if(description.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=findDescription(((ViewGroup)view).getChildAt(i),description);if(found!=null)return found;}return null;}
 @Test public void kineticImpactUsesOnlyTheRemainingEdgeDistanceAndDoesNotKickAgain(){
  context.getSharedPreferences("veil_panel",0).edit().putBoolean("experimental_motion",true).commit();
  ElasticPanelScrollView scroll=new ElasticPanelScrollView(context);View content=new View(context);scroll.addView(content,new ScrollView.LayoutParams(500,1200));scroll.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));scroll.layout(0,0,500,700);
  scroll.scrollTo(0,480);edge(scroll,25,480,500,false);
  float speed=ReflectionHelpers.getField(scroll,"velocity");assertEquals("Only five pixels crossed the edge",-60f,speed,.01f);
  ReflectionHelpers.callInstanceMethod(scroll,"cancelSpring");ReflectionHelpers.setField(scroll,"displacement",0f);ReflectionHelpers.setField(scroll,"velocity",0f);
  edge(scroll,300,500,500,false);assertEquals("A clamped fling must not start another bounce",0f,ReflectionHelpers.<Float>getField(scroll,"velocity"),.01f);
 }
 @Test public void aListThatFitsTheViewportDoesNotAcquireAFlingBounce(){
  context.getSharedPreferences("veil_panel",0).edit().putBoolean("experimental_motion",true).commit();
  ElasticPanelScrollView scroll=new ElasticPanelScrollView(context);scroll.addView(new View(context),new ScrollView.LayoutParams(500,700));scroll.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));scroll.layout(0,0,500,700);
  edge(scroll,200,0,0,false);assertEquals(0f,ReflectionHelpers.<Float>getField(scroll,"velocity"),.01f);assertFalse(ReflectionHelpers.<Boolean>getField(scroll,"releasing"));
 }
 @Test public void releasingAfterHoldingAtAnEdgeDropsOldPullVelocity(){
  context.getSharedPreferences("veil_panel",0).edit().putBoolean("experimental_motion",true).commit();
  ElasticPanelScrollView scroll=new ElasticPanelScrollView(context);scroll.addView(new View(context),new ScrollView.LayoutParams(500,1200));scroll.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));scroll.layout(0,0,500,700);
  edge(scroll,-100,0,500,true);ReflectionHelpers.setField(scroll,"velocity",1000f);
  Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(150));
  MotionEvent release=MotionEvent.obtain(0,android.os.SystemClock.uptimeMillis(),MotionEvent.ACTION_CANCEL,250,400,0);scroll.dispatchTouchEvent(release);release.recycle();
  assertEquals("A held finger has no remaining release speed",0f,ReflectionHelpers.<Float>getField(scroll,"velocity"),.01f);
 }
 @Test public void normalModeDoesNotRepositionAnyPresetAtEitherEdge(){
  for(String id:com.ubai.veilpanel.config.PanelThemePresets.IDS){
   com.ubai.veilpanel.config.PanelThemePresets.apply(context,id);
   ElasticPanelScrollView scroll=new ElasticPanelScrollView(context);View content=new View(context);scroll.addView(content,new ScrollView.LayoutParams(500,1200));scroll.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));scroll.layout(0,0,500,700);
   edge(scroll,-300,0,500,true);assertEquals(id,0f,content.getTranslationY(),.01f);assertEquals(0,scroll.getScrollY());
   scroll.scrollTo(0,480);edge(scroll,300,480,500,true);assertEquals(id,0f,content.getTranslationY(),.01f);assertEquals(500,scroll.getScrollY());
   edge(scroll,300,500,500,false);assertEquals(id,0f,content.getTranslationY(),.01f);assertFalse(ReflectionHelpers.<Boolean>getField(scroll,"releasing"));
  }
 }
 @Test public void boundaryFlingEasesToRestWithoutReboundingOrRestarting(){
  for(int direction:new int[]{-1,1}){
   ElasticPanelScrollView scroll=new ElasticPanelScrollView(context);View content=new View(context);content.setMinimumHeight(1700);scroll.addView(content,new ScrollView.LayoutParams(500,1700));scroll.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));scroll.layout(0,0,500,700);
   scroll.scrollTo(0,direction<0?300:700);assertEquals("Starting scroll",direction<0?300:700,scroll.getScrollY());scroll.fling(direction*3000);
   android.widget.OverScroller prediction=ReflectionHelpers.getField(scroll,"flingPrediction");
   assertTrue("direction="+direction+" prediction="+prediction.getFinalY(),ReflectionHelpers.<Boolean>getField(scroll,"boundedFling"));
   android.widget.OverScroller animation=ReflectionHelpers.getField(scroll,"edgeFling");
   assertEquals(direction<0?0:1000,animation.getFinalY());
   int previous=scroll.getScrollY();
   for(int frame=0;frame<30;frame++){
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(16));scroll.computeScroll();
    assertTrue(direction<0?scroll.getScrollY()<=previous:scroll.getScrollY()>=previous);previous=scroll.getScrollY();
    assertEquals(0f,content.getTranslationY(),.001f);
   }
   assertEquals(direction<0?0:1000,scroll.getScrollY());scroll.computeScroll();assertEquals(previous,scroll.getScrollY());
  }
 }
 @Test public void touchingDuringBoundaryFlingStopsItAtTheCurrentPosition(){
  ElasticPanelScrollView scroll=new ElasticPanelScrollView(context);View content=new View(context);content.setMinimumHeight(1700);scroll.addView(content,new ScrollView.LayoutParams(500,1700));scroll.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));scroll.layout(0,0,500,700);scroll.scrollTo(0,700);scroll.fling(3000);
  MotionEvent down=MotionEvent.obtain(0,1,MotionEvent.ACTION_DOWN,250,300,0);scroll.dispatchTouchEvent(down);down.recycle();int stopped=scroll.getScrollY();
  Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));scroll.computeScroll();assertEquals(stopped,scroll.getScrollY());assertFalse(ReflectionHelpers.<Boolean>getField(scroll,"boundedFling"));
 }
 @Test public void statusReplayAndMainDrawShareOneScrollPositionPerFrame(){
  ElasticPanelScrollView scroll=new ElasticPanelScrollView(context);View content=new View(context);content.setMinimumHeight(1700);scroll.addView(content,new ScrollView.LayoutParams(500,1700));scroll.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(700,View.MeasureSpec.EXACTLY));scroll.layout(0,0,500,700);scroll.scrollTo(0,700);scroll.fling(3000);
  Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(16));
  ReflectionHelpers.callInstanceMethod(scroll,"beginScrollFrame");int statusPosition=scroll.getScrollY();
  Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(8));scroll.computeScroll();
  assertEquals("Drawing the main window must not advance past its status strip",statusPosition,scroll.getScrollY());
  ReflectionHelpers.callInstanceMethod(scroll,"beginScrollFrame");assertTrue("The next frame still scrolls",scroll.getScrollY()>statusPosition);
 }
 private void edge(ElasticPanelScrollView view,int dy,int y,int range,boolean touch){ReflectionHelpers.callInstanceMethod(view,"overScrollBy",ReflectionHelpers.ClassParameter.from(int.class,0),ReflectionHelpers.ClassParameter.from(int.class,dy),ReflectionHelpers.ClassParameter.from(int.class,0),ReflectionHelpers.ClassParameter.from(int.class,y),ReflectionHelpers.ClassParameter.from(int.class,0),ReflectionHelpers.ClassParameter.from(int.class,range),ReflectionHelpers.ClassParameter.from(int.class,0),ReflectionHelpers.ClassParameter.from(int.class,0),ReflectionHelpers.ClassParameter.from(boolean.class,touch));}
 private View findCard(View view){if(view.getTransitionName()!=null&&view.getTransitionName().startsWith("notification:"))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=findCard(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}return null;}
 private View findText(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=findText(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
}
