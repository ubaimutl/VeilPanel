package com.ubai.veilpanel.ui;

import android.animation.ValueAnimator;
import android.view.Choreographer;
import java.util.function.Consumer;

/** Interruptible, critically damped spring. Position is 0 closed, 1 open. */
final class CurtainMotion {
 private final Consumer<Float> changed;
 private float position=1,velocity,target;
 private long lastFrame;
 private boolean running;
 private Runnable finished;
 private final Choreographer.FrameCallback frame=this::step;
 CurtainMotion(Consumer<Float> changed){this.changed=changed;}
 float position(){return position;}
 void cancel(){Choreographer.getInstance().removeFrameCallback(frame);running=false;lastFrame=0;finished=null;}
 void jump(float value){cancel();position=clamp(value);velocity=0;changed.accept(position);}
 void settle(float target,float releaseVelocity,Runnable finished){
  cancel();this.target=clamp(target);this.velocity=Math.max(-8,Math.min(8,releaseVelocity));this.finished=finished;
  if(!ValueAnimator.areAnimatorsEnabled()){position=this.target;velocity=0;changed.accept(position);complete();return;}
  running=true;Choreographer.getInstance().postFrameCallback(frame);
 }
 private void step(long nanos){
  if(!running)return;
  float dt=lastFrame==0?1f/120f:Math.max(.001f,Math.min(.032f,(nanos-lastFrame)/1_000_000_000f));lastFrame=nanos;
  for(int i=0;i<4;i++){
   float h=dt/4;
   velocity+=(-650*(position-target)-2*(float)Math.sqrt(650)*velocity)*h;
   position+=velocity*h;
   if(position<0){position=0;velocity=Math.max(0,velocity);}else if(position>1){position=1;velocity=Math.min(0,velocity);}
  }
  changed.accept(position);
  if(Math.abs(position-target)<.00035f&&Math.abs(velocity)<.003f){position=target;velocity=0;changed.accept(position);complete();}
  else Choreographer.getInstance().postFrameCallback(frame);
 }
 private void complete(){running=false;lastFrame=0;Runnable callback=finished;finished=null;if(callback!=null)callback.run();}
 private static float clamp(float value){return Math.max(0,Math.min(1,value));}
 static boolean shouldClose(float openAmount,float velocityPixels,float height,float density){
  float dragged=(1-openAmount)*height;
  if(velocityPixels>650*density)return false;
  return dragged>=height*.18f||(dragged>12*density&&velocityPixels<-650*density);
 }
}
