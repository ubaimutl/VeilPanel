package com.ubai.veilpanel.ui;
import android.content.Context;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
/** Consume the scroll range before resisting only the unconsumed edge distance. */
public final class ElasticPanelScrollView extends ScrollView {
 private float displacement,rawPull,velocity;
 private long previousFrame,lastPullAt;
 private boolean releasing,flingEdgeHandled;
 private final android.widget.OverScroller edgeFling;
 private final android.widget.OverScroller flingPrediction;
 private boolean boundedFling;
 private boolean frameManaged,scrollAdvanced;
 private final android.view.ViewTreeObserver.OnPreDrawListener advanceScroll=this::beginScrollFrame;
 private final Choreographer.FrameCallback spring=this::stepSpring;
 public ElasticPanelScrollView(Context context){super(context);setOverScrollMode(OVER_SCROLL_NEVER);setVerticalScrollBarEnabled(false);setHorizontalScrollBarEnabled(false);
  edgeFling=new android.widget.OverScroller(context,t->1-(1-t)*(1-t)*(1-t));
  flingPrediction=new android.widget.OverScroller(context);
 }
 private boolean bounceEnabled(){return getContext().getSharedPreferences("veil_panel",0).getBoolean("experimental_motion",false);}
 private void clearEdge(){cancelSpring();displacement=rawPull=velocity=0;View child=getChildAt(0);if(child!=null)child.setTranslationY(0);}
 private float dp(float value){return value*getResources().getDisplayMetrics().density;}
 public static float resist(float value,float limit){return (float)Math.copySign(limit*(1-Math.exp(-Math.abs(value)/limit)),value);}
 private void cancelSpring(){Choreographer.getInstance().removeFrameCallback(spring);releasing=false;previousFrame=0;}
 @Override public boolean dispatchTouchEvent(MotionEvent event){
  if(event.getActionMasked()==MotionEvent.ACTION_DOWN){stopBoundedFling();cancelSpring();flingEdgeHandled=false;velocity=0;lastPullAt=0;View child=getChildAt(0);if(child!=null){displacement=child.getTranslationY();float limit=dp(48);rawPull=(float)Math.copySign(-limit*Math.log(Math.max(.001,1-Math.abs(displacement)/limit)),displacement);}}
  boolean handled=super.dispatchTouchEvent(event);
  if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL)releaseEdge(android.os.SystemClock.uptimeMillis()-lastPullAt>80?0:velocity);
  return handled;
 }
 @Override protected boolean overScrollBy(int dx,int dy,int x,int y,int rangeX,int rangeY,int maxX,int maxY,boolean touch){
  if(!bounceEnabled()){
   if(releasing||displacement!=0||rawPull!=0)clearEdge();
   return super.overScrollBy(dx,dy,x,y,rangeX,rangeY,0,0,touch);
  }
  if(touch&&rawPull!=0){
   float proposed=rawPull-dy*.28f;
   if(Math.signum(proposed)==Math.signum(rawPull)){pull(proposed);return super.overScrollBy(dx,0,x,y,rangeX,rangeY,0,0,true);}
   dy=Math.round(-proposed/.28f);rawPull=0;displacement=0;View child=getChildAt(0);if(child!=null)child.setTranslationY(0);
  }
  int next=y+dy;int excess=next<0?next:next>rangeY?next-rangeY:0;
  boolean clamped=super.overScrollBy(dx,dy,x,y,rangeX,rangeY,0,0,touch);
  if(excess!=0){
   if(touch)pull(rawPull-excess*.28f);
   else if(!flingEdgeHandled){
    flingEdgeHandled=true;
    // One small absorption, using only the unconsumed edge movement. A
    // full-speed second spring after ScrollView clamps feels like a kick,
    // especially with short preset layouts. A fitting list has no impact.
    if(rangeY>0&&!releasing&&displacement==0)
     releaseEdge(Math.max(-dp(260),Math.min(dp(260),-excess*12f)));
   }
  }
  return clamped;
 }
 @Override public void fling(int velocityY){
  stopBoundedFling();flingEdgeHandled=false;
  if(bounceEnabled()||getChildAt(0)==null||velocityY==0){super.fling(velocityY);return;}
  View child=getChildAt(0);android.view.ViewGroup.MarginLayoutParams lp=(android.view.ViewGroup.MarginLayoutParams)child.getLayoutParams();
  int range=Math.max(0,child.getHeight()+lp.topMargin+lp.bottomMargin-(getHeight()-getPaddingTop()-getPaddingBottom()));
  int start=getScrollY();
  flingPrediction.fling(0,start,0,velocityY,0,0,Integer.MIN_VALUE,Integer.MAX_VALUE);
  int naturalEnd=flingPrediction.getFinalY();flingPrediction.forceFinished(true);
  int end=Math.max(0,Math.min(range,naturalEnd));
  if(end==naturalEnd){super.fling(velocityY);return;}
  // ScrollView otherwise reaches the boundary with residual velocity and
  // clamps abruptly. Fit a zero-terminal-speed curve to the available space.
  super.fling(0);super.computeScroll();
  int distance=end-start;if(distance==0)return;
  int duration=Math.max(1,Math.min(2000,Math.round(3000f*Math.abs(distance)/Math.abs((float)velocityY))));
  edgeFling.startScroll(0,start,0,distance,duration);boundedFling=true;postInvalidateOnAnimation();
 }
 @Override public void computeScroll(){
  // The under-status replay and the ordinary window draw visit the same
  // ScrollView. Advance it once, before glass materials read their positions,
  // so both portions use one scroll offset for this display frame.
  if(frameManaged&&scrollAdvanced)return;
  if(frameManaged)scrollAdvanced=true;
  if(!boundedFling){super.computeScroll();return;}
  if(edgeFling.computeScrollOffset()){
   scrollTo(0,edgeFling.getCurrY());
   if(!edgeFling.isFinished())postInvalidateOnAnimation();else boundedFling=false;
  }else boundedFling=false;
 }
 private boolean beginScrollFrame(){frameManaged=true;scrollAdvanced=false;computeScroll();return true;}
 private void stopBoundedFling(){edgeFling.forceFinished(true);boundedFling=false;}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();getViewTreeObserver().addOnPreDrawListener(advanceScroll);}
 private void pull(float value){
  cancelSpring();rawPull=value;float before=displacement;displacement=resist(rawPull,dp(48));long now=android.os.SystemClock.uptimeMillis();
  if(lastPullAt!=0)velocity=(displacement-before)*1000f/Math.max(8,now-lastPullAt);lastPullAt=now;
  View child=getChildAt(0);if(child!=null)child.setTranslationY(displacement);
 }
 private void releaseEdge(float speed){
  if(!bounceEnabled()){clearEdge();return;}
  if(releasing||getChildAt(0)==null||(displacement==0&&speed==0))return;
  rawPull=0;velocity=Math.max(-dp(1400),Math.min(dp(1400),speed));
  if(!android.animation.ValueAnimator.areAnimatorsEnabled()){displacement=0;getChildAt(0).setTranslationY(0);return;}
  releasing=true;previousFrame=0;Choreographer.getInstance().postFrameCallback(spring);
 }
 private void stepSpring(long frame){
  if(!bounceEnabled()){clearEdge();return;}
  View child=getChildAt(0);if(!releasing||child==null)return;
  float dt=previousFrame==0?1f/120f:Math.min(.032f,(frame-previousFrame)/1_000_000_000f);previousFrame=frame;
  boolean experimental=getContext().getSharedPreferences("veil_panel",0).getBoolean("experimental_motion",false);
  float stiffness=experimental?260f:420f;
  float damping=experimental?Math.max(.70f,Math.min(1.1f,getContext().getSharedPreferences("veil_panel",0).getInt("motion_damping",90)/100f)):1f;
  int steps=4;float h=dt/steps;
  for(int i=0;i<steps;i++){velocity+=(-stiffness*displacement-2*damping*(float)Math.sqrt(stiffness)*velocity)*h;displacement+=velocity*h;}
  child.setTranslationY(displacement);
  if(Math.abs(displacement)<.15f&&Math.abs(velocity)<2f){displacement=0;velocity=0;child.setTranslationY(0);releasing=false;previousFrame=0;}
  else Choreographer.getInstance().postFrameCallback(spring);
 }
 @Override protected void onDetachedFromWindow(){if(getViewTreeObserver().isAlive())getViewTreeObserver().removeOnPreDrawListener(advanceScroll);frameManaged=scrollAdvanced=false;stopBoundedFling();cancelSpring();View child=getChildAt(0);if(child!=null)child.setTranslationY(0);displacement=rawPull=velocity=0;super.onDetachedFromWindow();}
}
