package com.ubai.veilpanel.ui;
import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.*;
import android.widget.*;
import com.ubai.veilpanel.WallpaperBackdropActivity;
import com.ubai.veilpanel.config.PanelRouting;
import com.ubai.veilpanel.system.SystemShadeController;
import com.ubai.veilpanel.system.SystemWindowMetrics;
/** A left-side catcher and one scrolling panel. Right-side input stays with SystemUI. */
public final class ShadeWindowController {
 private final AccessibilityService service;
 private final SystemShadeController system;
 private final WindowManager manager;
 private final int statusHeight, slop;
 private FrameLayout root;
 private WindowManager.LayoutParams params;
 private ScrollView scroll;
 private PanelContentViewport viewport;
 private ShadeView content;
 private View dismissHandle;
 private View dismissMark;
 private FrameLayout handleGuard;
 private CurtainMotion curtain;
 private VelocityTracker handleVelocity;
 private float handleStartX,handleStartY,handleStartAmount;
 private boolean handleDragging,handleAborted;
 private PanelBackdropWindow backdrop;
 private FrameLayout topGuard;
 private WindowManager.LayoutParams topGuardParams;
 private boolean routePending,edgeRight,edgePull,edgeStreamActive;
 private float edgeX,edgeY;
 private final Runnable nativeHandoff=this::handoffNativeQuickSettings;
 private void handoffNativeQuickSettings(){
  if(!expanded||!routePending)return;
  system.dismissSystemShade("prepare native right swipe");
  collapse("right-edge quick settings");
  // Let the removed guard's input window settle for two frames, without a
  // separate dismiss-and-wait phase exposing the underlying app for 200ms.
  handler.postDelayed(this::dispatchNativeRightPull,32);
 }
 private boolean canHandoff(){KeyguardManager guard=service.getSystemService(KeyguardManager.class);android.os.PowerManager power=service.getSystemService(android.os.PowerManager.class);return !expanded&&guard!=null&&!guard.isKeyguardLocked()&&power!=null&&power.isInteractive();}
 private void dispatchNativeRightPull(){
  if(!canHandoff())return;
  android.graphics.Path path=new android.graphics.Path();float x=width()*.90f;path.moveTo(x,2);path.lineTo(x,Math.min(service.getResources().getDisplayMetrics().heightPixels*.38f,dp(420)));
  android.accessibilityservice.GestureDescription gesture=new android.accessibilityservice.GestureDescription.Builder().addStroke(new android.accessibilityservice.GestureDescription.StrokeDescription(path,0,180)).build();
  boolean sent=service.dispatchGesture(gesture,null,handler);
  if(!sent)android.widget.Toast.makeText(service,"Gesture control unavailable. Re-enable Veil Panel in Accessibility.",android.widget.Toast.LENGTH_LONG).show();
 }
 private volatile boolean expanded;
 private boolean tracking,suspended,closing,openingStreamActive,recoveringNativeShade;
 private boolean statusBarVisible=true,navigationBarVisible,navigationBarPresent=true,fullscreenRevealOnly;
 private long fullscreenArmedUntil;
 private final Runnable expireFullscreenReveal=()->{fullscreenArmedUntil=0;if(!expanded)updateBounds();};
 // With no navigation inset source, a hidden status bar may mean immersive mode
 // (SystemUI only reveals bars) or a status-only app (SystemUI opens its shade).
 // The first pull then stays reveal-only, and Veil watches briefly: if SystemUI
 // registers its public dismiss-shade action, that pull opened the native shade,
 // so Veil replaces it. Otherwise the existing second-pull rule applies.
 private boolean observingNativeShade;
 private long observeNativeShadeUntil;
 private static final long OBSERVE_WHILE_HELD_MS=3000,OBSERVE_AFTER_RELEASE_MS=600,OBSERVE_POLL_MS=48;
 private final Runnable observeNativeShade=this::checkObservedNativeShade;
 private void startObservingNativeShade(){
  observingNativeShade=true;observeNativeShadeUntil=android.os.SystemClock.uptimeMillis()+OBSERVE_WHILE_HELD_MS;
  handler.removeCallbacks(observeNativeShade);handler.postDelayed(observeNativeShade,OBSERVE_POLL_MS);
 }
 private void observeAfterRelease(){
  if(observingNativeShade)observeNativeShadeUntil=Math.min(observeNativeShadeUntil,android.os.SystemClock.uptimeMillis()+OBSERVE_AFTER_RELEASE_MS);
 }
 private void stopObservingNativeShade(){observingNativeShade=false;handler.removeCallbacks(observeNativeShade);}
 /** Also called for SystemUI accessibility events, so detection is not limited to the poll interval. */
 public void checkObservedNativeShade(){
  if(!observingNativeShade)return;
  handler.removeCallbacks(observeNativeShade);
  if(expanded||suspended||android.os.SystemClock.uptimeMillis()>observeNativeShadeUntil){stopObservingNativeShade();return;}
  if(system.hasDismissAction()){
   stopObservingNativeShade();fullscreenRevealOnly=false;
   open();if(expanded){openingStreamActive=false;startNativeRecovery();}
   return;
  }
  handler.postDelayed(observeNativeShade,OBSERVE_POLL_MS);
 }
 private boolean fullscreenProtection(){return service.getSharedPreferences("veil_panel",0).getBoolean("fullscreen_two_swipe",true);}
 private boolean fullscreenArmed(){return android.os.SystemClock.uptimeMillis()<fullscreenArmedUntil;}
 private void armFullscreenSecondPull(){
  android.view.accessibility.AccessibilityManager accessibility=service.getSystemService(android.view.accessibility.AccessibilityManager.class);
  int timeout=accessibility==null?2250:Math.max(2250,accessibility.getRecommendedTimeoutMillis(2250,android.view.accessibility.AccessibilityManager.FLAG_CONTENT_CONTROLS));
  fullscreenArmedUntil=android.os.SystemClock.uptimeMillis()+timeout;handler.removeCallbacks(expireFullscreenReveal);handler.postDelayed(expireFullscreenReveal,timeout);updateBounds();
 }
 private FullscreenBarMonitor barMonitor;
 private float startX,startY;
 private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable retryDismiss=this::dismissIfExpanded;
 private final Runnable nativeRecovery=this::recoverNativeShade;
 private boolean statusRecoveryCovered;
 private long nativeShadeLastSeen;
 private final Runnable uncoverStatus=()->setStatusRecoveryCovered(false);
 private void setStatusRecoveryCovered(boolean covered){
  statusRecoveryCovered=covered&&expanded&&PanelSystemBars.visible(service)&&service.getSharedPreferences("veil_panel",0).getBoolean("recovery_status_cover",true);
  if(topGuard!=null)topGuard.invalidate();
 }
 private void stopNativeRecovery(){
  recoveringNativeShade=false;handler.removeCallbacks(nativeRecovery);handler.removeCallbacks(uncoverStatus);
  if(statusRecoveryCovered)handler.postDelayed(uncoverStatus,120);
 }
 private void startNativeRecovery(){
  if(!canSuppressOpening()||!service.getSharedPreferences("veil_panel",0).getBoolean("native_edge_recovery",true))return;
  recoveringNativeShade=true;handler.removeCallbacks(uncoverStatus);nativeShadeLastSeen=android.os.SystemClock.uptimeMillis();setStatusRecoveryCovered(true);handler.removeCallbacks(nativeRecovery);recoverNativeShade();
 }
 private void recoverNativeShade(){
  if(!recoveringNativeShade)return;
  if(!canSuppressOpening()||!service.getSharedPreferences("veil_panel",0).getBoolean("native_edge_recovery",true)){stopNativeRecovery();return;}
  // Only after Android CANCELLED a captured left pull. Once ownership is lost,
  // UP is not delivered. End this UI-scoped recovery on the next fresh input,
  // close/lock or right handoff rather than guessing when that finger lifted.
  if(system.hasDismissAction()){
   nativeShadeLastSeen=android.os.SystemClock.uptimeMillis();setStatusRecoveryCovered(true);
   system.dismissSystemShade("cancelled edge recovery");
  }else if(android.os.SystemClock.uptimeMillis()-nativeShadeLastSeen>=360){
   // Action availability is only a signal, not compositor completion. Require
   // a quiet interval instead of flashing the mask with each dismissal.
   setStatusRecoveryCovered(false);
  }
  if(statusRecoveryCovered&&topGuard!=null)topGuard.invalidate();
  handler.postDelayed(nativeRecovery,120);
 }
 private void observeTouch(String source,MotionEvent event){
  traceTouch(source,event);
  if(expanded&&event.getActionMasked()==MotionEvent.ACTION_DOWN&&recoveringNativeShade){
   system.dismissSystemShade("fresh input after cancelled edge");stopNativeRecovery();
  }
 }
 private void resumeClosingPanel(){
  if(!closing)return;
  closing=false;
  if(scroll!=null)scroll.animate().withEndAction(null).cancel();
  if(curtain!=null){curtain.cancel();curtain.settle(1,0,null);}
  else if(scroll!=null)scroll.animate().translationY(0).setDuration(160).start();
 }

 // Bounded fallback for OEM shade races. The permanent catcher keeps the
 // original pointer stream; no normal-path polling or injected tap.
 private static final long[] OPENING_DISMISS_DELAYS_MS={64,180,360,600,900,1250,1600,2000};
 private void scheduleShadeSuppression(){
  handler.removeCallbacks(retryDismiss);
  for(long delay:OPENING_DISMISS_DELAYS_MS)handler.postDelayed(retryDismiss,delay);
 }
 private void stopOpeningSuppression(){
  handler.removeCallbacks(retryDismiss);stopNativeRecovery();
 }
 private boolean canSuppressOpening(){
  KeyguardManager guard=service.getSystemService(KeyguardManager.class);
  return expanded&&!suspended&&!closing&&!routePending&&guard!=null&&!guard.isKeyguardLocked();
 }
 private void dismissIfExpanded(){
  if(canSuppressOpening())system.dismissSystemShade("opening race check");
  else stopOpeningSuppression();
 }
 private void traceTouch(String source,MotionEvent event){
  if(android.util.Log.isLoggable("VEIL_SHADE_DEBUG",android.util.Log.DEBUG))
   android.util.Log.d("VEIL_SHADE_DEBUG","uptime="+android.os.SystemClock.uptimeMillis()+" source="+source+" action="+MotionEvent.actionToString(event.getAction())+" x="+event.getRawX()+" y="+event.getRawY()+" expanded="+expanded+" revealOnly="+fullscreenRevealOnly);
 }
 private final android.content.SharedPreferences.OnSharedPreferenceChangeListener changed=(p,k)->{if("native_edge_recovery".equals(k)&&!p.getBoolean(k,true)){stopNativeRecovery();handler.removeCallbacks(uncoverStatus);setStatusRecoveryCovered(false);}else if("recovery_status_cover".equals(k)&&!p.getBoolean(k,true)){setStatusRecoveryCovered(false);}else if("fullscreen_two_swipe".equals(k)){tracking=false;fullscreenRevealOnly=false;fullscreenArmedUntil=0;stopObservingNativeShade();handler.removeCallbacks(expireFullscreenReveal);updateBounds();}else if("split".equals(k)&&!expanded)updateBounds();};
 public ShadeWindowController(AccessibilityService service,SystemShadeController system) {
  this.service=service;this.system=system;manager=service.getSystemService(WindowManager.class);
  statusHeight=SystemWindowMetrics.getStatusBarHeight(service);slop=ViewConfiguration.get(service).getScaledTouchSlop();
 }
 public void start() {
  if(root!=null)return;
  service.getSharedPreferences("veil_panel",0).registerOnSharedPreferenceChangeListener(changed);
  root=new FrameLayout(service){
   @Override public boolean dispatchTouchEvent(MotionEvent event){observeTouch("panel",event);return super.dispatchTouchEvent(event);}
  };root.setBackgroundColor(Color.TRANSPARENT);root.setFocusableInTouchMode(true);
  root.setOnTouchListener(this::touch);
  root.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_BACK&&expanded){if(event.getAction()==KeyEvent.ACTION_UP)animateClose();return true;}return false;});
  params=new WindowManager.LayoutParams(triggerWidth(),statusHeight+dp(8),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,PixelFormat.TRANSLUCENT);
  params.gravity=Gravity.TOP|Gravity.LEFT;params.setFitInsetsTypes(0);params.setTitle("VeilPanelWindow");
  params.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
  manager.addView(root,params);addTopGuard();barMonitor=new FullscreenBarMonitor(service,(status,navigation,present)->{navigationBarVisible=navigation;navigationBarPresent=present;statusBarVisibilityChanged(status);});barMonitor.start();setSuspended(suspended);
 }
 public void stop(){handler.removeCallbacksAndMessages(null);if(barMonitor!=null){barMonitor.close();barMonitor=null;}service.getSharedPreferences("veil_panel",0).unregisterOnSharedPreferenceChangeListener(changed);collapse();if(topGuard!=null){manager.removeViewImmediate(topGuard);topGuard=null;topGuardParams=null;}if(root!=null){manager.removeViewImmediate(root);root=null;params=null;}}
 public void setSuspended(boolean value) {
  if(root!=null && suspended==value && ((root.getVisibility()==View.GONE)==value))return;
  suspended=value;if(value){fullscreenArmedUntil=0;fullscreenRevealOnly=false;stopObservingNativeShade();handler.removeCallbacks(expireFullscreenReveal);}if(root==null)return;
  if(value){collapse();params.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;root.setVisibility(View.GONE);}
  else {params.flags&=~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;root.setVisibility(View.VISIBLE);}
  updateBounds();
 }
 private void statusBarVisibilityChanged(boolean visible){if(statusBarVisible!=visible){statusBarVisible=visible;if(!expanded){tracking=false;fullscreenRevealOnly=false;fullscreenArmedUntil=0;handler.removeCallbacks(expireFullscreenReveal);}updateBounds();}}
 public boolean isExpanded(){return expanded;}
 public boolean shouldSuppressSystemShade(){return !suspended&&((tracking&&!fullscreenRevealOnly)||expanded);}
 private int dp(int value){return SystemWindowMetrics.dp(service,value);}
 private int width(){return service.getResources().getDisplayMetrics().widthPixels;}
 private boolean curtainEnabled(){return service.getSharedPreferences("veil_panel",0).getBoolean("curtain_motion",true);}
 private int curtainHeight(){return params!=null&&params.height>0?params.height:service.getResources().getDisplayMetrics().heightPixels;}
 private void setCurtainPosition(float amount){
  if(root==null||!expanded)return;
  int height=curtainHeight();float offset=-(1-amount)*height;
  root.setTranslationY(offset);
  // Keep all high-layer content out of the real status icons while sliding.
  int top=PanelSystemBars.visible(service)?statusHeight:0;
  root.setClipBounds(new android.graphics.Rect(0,Math.min(height,Math.round(top-offset)),width(),height));
  if(backdrop!=null)backdrop.setCurtainOffset(offset);
 }
 private int expandedEdgeHeight(){return PanelSystemBars.visible(service)?statusHeight+dp(8):dp(12);}
 private int triggerWidth(){return Math.round(width()*PanelRouting.split(service)/100f);}
 private void updateBounds(){
  if(root==null||params==null)return;
  // Blur lives in the wallpaper host below SystemUI, so real status icons stay sharp.
  params.flags&=~(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER|WindowManager.LayoutParams.FLAG_BLUR_BEHIND|WindowManager.LayoutParams.FLAG_DIM_BEHIND);
  params.setBlurBehindRadius(0);
  // Claim transparent system-bar backgrounds only while this panel is open.
  if(expanded)params.flags|=WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
  else params.flags&=~WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
  // Keep the complete status-bar touch band in fullscreen: real fingers
  // frequently start below the first four dp. The first stream is reveal-only;
  // native SystemUI observes it while a fresh second stream opens Veil.
  if(suspended)params.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
  else params.flags&=~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
  params.width=expanded?WindowManager.LayoutParams.MATCH_PARENT:triggerWidth();
  params.height=expanded?SystemWindowMetrics.getUsableScreenHeight(service,manager,statusHeight):statusHeight+dp(8);
  manager.updateViewLayout(root,params);
  if(topGuard!=null&&topGuardParams!=null){
   topGuardParams.width=expanded?-1:triggerWidth();topGuardParams.height=expanded?expandedEdgeHeight():statusHeight+dp(8);
   if(suspended)topGuardParams.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;else topGuardParams.flags&=~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
   topGuard.setVisibility(suspended?View.GONE:View.VISIBLE);manager.updateViewLayout(topGuard,topGuardParams);
  }
 }
 private boolean touch(View view,MotionEvent event) {
  if(suspended){tracking=false;return false;}
  if(expanded){
   if(event.getRawY()<expandedEdgeHeight())return topEdgeTouch(view,event);
   return true;
  }
  switch(event.getActionMasked()){
   case MotionEvent.ACTION_DOWN:
    KeyguardManager guard=service.getSystemService(KeyguardManager.class);
    if(guard==null||guard.isKeyguardLocked())return false;
    if(PanelRouting.isQuickSettings(event.getRawX(),width(),PanelRouting.split(service)))return false;
    startX=event.getRawX();startY=event.getRawY();tracking=true;fullscreenRevealOnly=fullscreenProtection()&&!statusBarVisible&&!navigationBarVisible&&!fullscreenArmed();
    if(fullscreenRevealOnly&&!navigationBarPresent)startObservingNativeShade();else stopObservingNativeShade();return true;
   case MotionEvent.ACTION_MOVE:
    if(tracking&&!fullscreenRevealOnly&&event.getRawY()-startY>slop&&event.getRawY()-startY>Math.abs(event.getRawX()-startX))open();return true;
   case MotionEvent.ACTION_CANCEL:
    // SystemUI's spy window can claim the stream before sending a MOVE.
    // The DOWN was already confined to the custom notification half.
    if(tracking){if(fullscreenRevealOnly){armFullscreenSecondPull();observeAfterRelease();}else{open();if(expanded){openingStreamActive=false;startNativeRecovery();}}}tracking=false;fullscreenRevealOnly=false;return true;
   case MotionEvent.ACTION_UP:
    if(tracking&&event.getRawY()-startY>slop){if(fullscreenRevealOnly)armFullscreenSecondPull();else open();}
    if(tracking&&fullscreenRevealOnly)observeAfterRelease();tracking=false;fullscreenRevealOnly=false;return true;
   default:return tracking;
  }
 }
 public void openFromShortcut(){open();}
 private void open(){
  if(expanded||suspended||root==null)return;
  KeyguardManager guard=service.getSystemService(KeyguardManager.class);if(guard==null||guard.isKeyguardLocked()){tracking=false;return;}
  fullscreenArmedUntil=0;handler.removeCallbacks(expireFullscreenReveal);stopObservingNativeShade();fullscreenRevealOnly=false;openingStreamActive=tracking;expanded=true;tracking=false;edgeStreamActive=false;edgeRight=false;edgePull=false;routePending=false;handler.removeCallbacks(nativeHandoff);
  system.dismissSystemShade("left pull");
  backdrop=new PanelBackdropWindow(service);
  if(!com.ubai.veilpanel.image.PowerSaverBackdrop.available(service)||!backdrop.show()){
   backdrop=null;
   WallpaperBackdropActivity.begin();
   try{service.startActivity(new Intent(service,WallpaperBackdropActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_ANIMATION|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));}
   catch(RuntimeException failed){collapse();return;}
  }
  params.flags&=~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;updateBounds();
  if(backdrop!=null)backdrop.addPopupCover(root,statusHeight);
  scroll=new ElasticPanelScrollView(service);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);scroll.setHorizontalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setClipToPadding(false);
  content=new ShadeView(service,this::animateClose);scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
  viewport=new PanelContentViewport(service);viewport.setClipChildren(true);viewport.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
  boolean beneathStatus=PanelSystemBars.visible(service)&&backdrop!=null&&service.getSharedPreferences("veil_panel",0).getBoolean("scroll_under_status_bar",true);
  FrameLayout.LayoutParams placement=new FrameLayout.LayoutParams(-1,-1);
  placement.topMargin=PanelSystemBars.visible(service)&&!beneathStatus?statusHeight:0;
  placement.bottomMargin=0;root.addView(viewport,placement);addTopGuard();
  if(beneathStatus){
   // Preserve resting position and replay only the scrolling status strip
   // below SystemUI, keeping the main panel above native shade surfaces.
   scroll.setPadding(0,statusHeight,0,0);
   root.setClipBounds(new android.graphics.Rect(0,statusHeight,width(),curtainHeight()));
  }
  content.setPadding(content.getPaddingLeft(),content.getPaddingTop(),content.getPaddingRight(),content.getPaddingBottom()+dp(88));addDismissHandle();
  content.onShown();root.requestFocus();scroll.setAlpha(1f);
  // Material position listeners run before recording the shared content.
  if(beneathStatus)backdrop.mirrorStatusContent(viewport,statusHeight);
  if(curtainEnabled()){
   curtain=new CurtainMotion(this::setCurtainPosition);curtain.jump(0);curtain.settle(1,0,null);
  }else{
   scroll.setTranslationY(-dp(18));scroll.animate().translationY(0f).setDuration(220).setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f)).start();
  }
  root.post(()->PanelSystemBars.apply(root));
  // Bounded retries only during opening, covering late OEM shade expansion.
  scheduleShadeSuppression();
 }
 /** Permanent input window. Never translate or replace the window holding
  * the opening DOWN: SystemUI can otherwise take its continuing edge drag.
  * Only the content/backdrop participate in curtain motion. */
 private void addTopGuard(){
  if(topGuard!=null)return;
  topGuard=new FrameLayout(service){
   @Override protected void dispatchDraw(android.graphics.Canvas canvas){
    if(statusRecoveryCovered&&backdrop!=null){
     int save=canvas.save();canvas.clipRect(0,0,getWidth(),statusHeight);
     backdrop.drawStatusCover(canvas,width(),curtainHeight());canvas.restoreToCount(save);
    }
    super.dispatchDraw(canvas);
   }
  };topGuard.setOnTouchListener((view,event)->{observeTouch("edge catcher",event);return expanded?topEdgeTouch(view,event):touch(view,event);});
  WindowManager.LayoutParams edge=new WindowManager.LayoutParams(expanded?-1:triggerWidth(),expanded?expandedEdgeHeight():statusHeight+dp(8),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
  edge.gravity=Gravity.TOP|Gravity.LEFT;edge.setFitInsetsTypes(0);edge.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;edge.setTitle("VeilPanelTopGuard");topGuardParams=edge;manager.addView(topGuard,edge);
 }
 private void suppressLeftEdge(){
  system.dismissSystemShade("expanded left edge");scheduleShadeSuppression();
 }
 private boolean topEdgeTouch(View view,MotionEvent event){
  traceTouch("top guard",event);
  if(!expanded||suspended)return false;
  switch(event.getActionMasked()){
   case MotionEvent.ACTION_DOWN:
    resumeClosingPanel();openingStreamActive=false;
    handler.removeCallbacks(nativeHandoff);routePending=false;edgeStreamActive=true;
    edgeX=event.getRawX();edgeY=event.getRawY();edgeRight=PanelRouting.isQuickSettings(edgeX,width(),PanelRouting.split(service));edgePull=false;return true;
   case MotionEvent.ACTION_MOVE:
    if(!edgeStreamActive)return true;
    if(!edgePull&&event.getRawY()-edgeY>slop && event.getRawY()-edgeY>Math.abs(event.getRawX()-edgeX)){edgePull=true;if(!edgeRight)suppressLeftEdge();else stopOpeningSuppression();}return true;
   case MotionEvent.ACTION_CANCEL:
    if(edgeStreamActive&&edgeRight){stopOpeningSuppression();routePending=true;handler.removeCallbacks(nativeHandoff);handler.postDelayed(nativeHandoff,320);}else{boolean lostLeft=openingStreamActive||(edgeStreamActive&&!edgeRight);suppressLeftEdge();if(lostLeft)startNativeRecovery();}openingStreamActive=false;edgeStreamActive=false;return true;
   case MotionEvent.ACTION_UP:
    if(edgeStreamActive&&edgeRight&&edgePull){stopOpeningSuppression();routePending=true;handler.removeCallbacks(nativeHandoff);handler.post(nativeHandoff);}else if(!edgeRight&&edgePull)suppressLeftEdge();openingStreamActive=false;edgeStreamActive=false;return true;
   default:return true;
  }
 }
 private void addDismissHandle(){
  FrameLayout handle=new FrameLayout(service);handle.setContentDescription("Close notifications. Drag upward or tap.");handle.setClickable(true);handle.setFocusable(true);
  View mark=new View(service);android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();shape.setColor(0xB3FFFFFF);shape.setCornerRadius(dp(3));mark.setBackground(shape);
  int length=Math.max(24,Math.min(100,service.getSharedPreferences("veil_panel",0).getInt("dismiss_handle_width",48)));
  int bottom=Math.max(0,Math.min(100,service.getSharedPreferences("veil_panel",0).getInt("dismiss_handle_bottom",42)));
  int below=Math.min(bottom,28);
  FrameLayout.LayoutParams line=new FrameLayout.LayoutParams(dp(length),dp(3),Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM);line.bottomMargin=dp(below);handle.addView(mark,line);dismissMark=mark;
  handle.setBackgroundColor(Color.TRANSPARENT);
  handle.setOnClickListener(v->animateClose());handle.setOnTouchListener(this::handleTouch);
  int grabWidth=Math.max(96,Math.min(320,service.getSharedPreferences("veil_panel",0).getInt("dismiss_grab_width",220)));
  int grabHeight=Math.max(48,Math.min(100,service.getSharedPreferences("veil_panel",0).getInt("dismiss_grab_height",72)));
  int actualWidth=Math.min(width(),dp(Math.max(grabWidth,length+24)));
  FrameLayout.LayoutParams placement=new FrameLayout.LayoutParams(actualWidth,dp(grabHeight),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);placement.bottomMargin=dp(bottom-below);root.addView(handle,placement);dismissHandle=handle;
  // This fresh transparent window sits above replacement system gestures.
  // The visible line stays in the moving sheet; the captured touch stream
  // keeps reaching this fixed guard even after the finger leaves its bounds.
  handleGuard=new FrameLayout(service);handleGuard.setContentDescription(handle.getContentDescription());handleGuard.setClickable(true);handleGuard.setFocusable(true);handleGuard.setOnClickListener(v->handle.performClick());handleGuard.setOnTouchListener(this::handleTouch);handle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  WindowManager.LayoutParams grab=new WindowManager.LayoutParams(actualWidth,dp(grabHeight),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
  int navigationInset=Math.max(0,manager.getCurrentWindowMetrics().getBounds().height()-curtainHeight());
  grab.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;grab.y=dp(bottom-below)+navigationInset;grab.setFitInsetsTypes(0);grab.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;grab.setTitle("VeilPanelHandleGuard");manager.addView(handleGuard,grab);
 }
 private boolean handleTouch(View view,MotionEvent event){
  if(!expanded)return false;
  if(handleAborted&&event.getActionMasked()!=MotionEvent.ACTION_DOWN){if(event.getActionMasked()==MotionEvent.ACTION_UP)handleAborted=false;return true;}
  switch(event.getActionMasked()){
   case MotionEvent.ACTION_DOWN:
    if(handleVelocity!=null)handleVelocity.recycle();handleVelocity=VelocityTracker.obtain();handleVelocity.addMovement(event);
    handleStartX=event.getRawX();handleStartY=event.getRawY();handleDragging=false;handleAborted=false;
    handleStartAmount=curtain!=null?curtain.position():1;
    if(curtain!=null){curtain.cancel();closing=false;}
    if(dismissMark!=null){dismissMark.animate().cancel();dismissMark.setAlpha(1);dismissMark.setScaleX(1.10f);dismissMark.setScaleY(1.4f);}return true;
   case MotionEvent.ACTION_MOVE:
    if(handleVelocity!=null)handleVelocity.addMovement(event);
    float dy=event.getRawY()-handleStartY,dx=event.getRawX()-handleStartX;
    if(!handleDragging&&Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)){handleDragging=true;view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START);}
    if(handleDragging&&curtain!=null)curtain.jump(handleStartAmount+dy/curtainHeight());return true;
   case MotionEvent.ACTION_POINTER_DOWN:
   case MotionEvent.ACTION_CANCEL:
    handleAborted=true;releaseHandle(false,0);return true;
   case MotionEvent.ACTION_UP:
    float speed=0;if(handleVelocity!=null){handleVelocity.addMovement(event);handleVelocity.computeCurrentVelocity(1000,dp(8000));speed=handleVelocity.getYVelocity();}
    boolean tap=!handleDragging&&Math.abs(event.getRawX()-handleStartX)<slop&&Math.abs(event.getRawY()-handleStartY)<slop;
    boolean dismiss=handleDragging&&(curtain!=null?CurtainMotion.shouldClose(curtain.position(),speed,curtainHeight(),service.getResources().getDisplayMetrics().density):event.getRawY()-handleStartY<-dp(18));
    releaseHandle(tap||dismiss,speed);return true;
   default:return true;
  }
 }
 private void releaseHandle(boolean dismiss,float speed){
  if(handleVelocity!=null){handleVelocity.recycle();handleVelocity=null;}
  handleDragging=false;
  if(dismissMark!=null)dismissMark.animate().alpha(1).scaleX(1).scaleY(1).setDuration(120).start();
  if(curtain!=null){closing=dismiss;curtain.settle(dismiss?0:1,speed/curtainHeight(),dismiss?()->collapse("curtain close"):null);}
  else if(dismiss)animateClose();
 }
 private void animateClose(){
  if(!expanded||closing||scroll==null)return;
  if(curtain!=null){stopOpeningSuppression();closing=true;curtain.settle(0,0,()->collapse("curtain close"));return;}
  stopOpeningSuppression();closing=true;scroll.animate().cancel();
  boolean experimental=service.getSharedPreferences("veil_panel",0).getBoolean("experimental_motion",false);
  scroll.animate().translationY(experimental?-scroll.getHeight():-dp(24)).setDuration(experimental?220:140)
   .setInterpolator(new android.view.animation.AccelerateInterpolator()).withEndAction(()->collapse("animated close")).start();
 }
 public void collapse(){collapse("close");}
 public void collapse(String reason){
  if(android.util.Log.isLoggable("VEIL_SHADE_DEBUG",android.util.Log.DEBUG))android.util.Log.d("VEIL_SHADE_DEBUG","collapse reason="+reason);
  stopOpeningSuppression();handler.removeCallbacks(uncoverStatus);setStatusRecoveryCovered(false);handler.removeCallbacks(nativeHandoff);routePending=false;edgePull=false;edgeRight=false;edgeStreamActive=false;tracking=false;openingStreamActive=false;expanded=false;closing=false;
  if(curtain!=null){curtain.cancel();curtain=null;}
  if(handleVelocity!=null){handleVelocity.recycle();handleVelocity=null;}handleDragging=false;
  if(handleGuard!=null){manager.removeViewImmediate(handleGuard);handleGuard=null;}
  if(content!=null){content.onHidden();content=null;}
  if(root!=null&&scroll!=null){scroll.animate().withEndAction(null).cancel();if(viewport!=null)root.removeView(viewport);}scroll=null;viewport=null;
  if(root!=null&&dismissHandle!=null)root.removeView(dismissHandle);dismissHandle=null;
  dismissMark=null;if(root!=null){root.setTranslationY(0);root.setClipBounds(null);}
  WallpaperBackdropActivity.close();
  if(backdrop!=null){backdrop.close();backdrop=null;}
  if(params!=null){params.flags|=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;updateBounds();}
 }
}
