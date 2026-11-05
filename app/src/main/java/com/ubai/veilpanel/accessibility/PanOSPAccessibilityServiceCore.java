package com.ubai.veilpanel.accessibility;
import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.accessibility.AccessibilityEvent;
import com.ubai.veilpanel.system.SystemShadeController;
import com.ubai.veilpanel.ui.ShadeWindowController;
/** Top-edge overlay only. Does not retrieve any app's view tree or typed text. */
public class PanOSPAccessibilityServiceCore extends AccessibilityService {
    private ShadeWindowController panel;
    private SystemShadeController system;
    private boolean registered;
    private static java.lang.ref.WeakReference<PanOSPAccessibilityServiceCore> active=new java.lang.ref.WeakReference<>(null);
    public static boolean requestPanelOpen() {
        PanOSPAccessibilityServiceCore service=active.get();
        if(service==null||service.panel==null)return false;
        service.wakeHandler.postDelayed(()->{service.updateScreen();if(active.get()==service&&service.panel!=null)service.panel.openFromShortcut();},80);
        return true;
    }
    private final android.os.Handler wakeHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable wakeCheck=this::updateScreen;
    private void recoverAfterWake(){
        wakeHandler.removeCallbacks(wakeCheck);updateScreen();
        for(long delay:new long[]{100,300,800,1600,3200,6000})wakeHandler.postDelayed(wakeCheck,delay);
    }
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) {
            if ((getPackageName()+".PAUSE").equals(intent.getAction())) { if (panel != null) panel.stop(); disableSelf(); return; }
            if ((getPackageName()+".CLOSE_PANEL").equals(intent.getAction())) { if(panel!=null)panel.collapse();return; }
            if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){wakeHandler.removeCallbacks(wakeCheck);if(panel!=null)panel.setSuspended(true);}
            else recoverAfterWake();
        }
    };
    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        if (panel != null) panel.stop();
        system = new SystemShadeController(this);
        panel = new ShadeWindowController(this, system);
        if (!registered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(getPackageName()+".PAUSE");
            filter.addAction(getPackageName()+".CLOSE_PANEL");
            registerReceiver(screen, filter, Context.RECEIVER_NOT_EXPORTED);
            registered = true;
        }
        panel.start();recoverAfterWake();
        active=new java.lang.ref.WeakReference<>(this);
        com.ubai.veilpanel.notifications.PanOSPNotificationListener.reconnectIfNeeded(this);
    }
    private void updateScreen() {
        if (panel == null) return;
        android.os.PowerManager power = getSystemService(android.os.PowerManager.class);
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        panel.setSuspended(power == null || !power.isInteractive() || keyguard == null || keyguard.isKeyguardLocked());
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (panel == null || event == null || system == null) return;
        if(event.getEventType()==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED&&android.util.Log.isLoggable("VEIL_SHADE_DEBUG",android.util.Log.DEBUG))
            android.util.Log.d("VEIL_SHADE_DEBUG","system event class="+event.getClassName()+" window="+event.getWindowId()+" expanded="+panel.isExpanded());
        updateScreen();
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        if (keyguard == null || keyguard.isKeyguardLocked()) return;
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && event.getPackageName() != null && !getPackageName().contentEquals(event.getPackageName())
                && !system.isSystemUiEvent(event) && panel.isExpanded()) { panel.collapse(); return; }
        if (panel.shouldSuppressSystemShade() && system.isSystemUiEvent(event)
                && system.shouldDismissSystemShade(event)) system.dismissSystemShade();
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (panel != null) panel.stop();
        panel = new ShadeWindowController(this,system);
        panel.start();recoverAfterWake();
    }
    @Override public void onInterrupt() { if (panel != null) panel.collapse(); }
    @Override public void onDestroy() {
        if(active.get()==this)active.clear();
        wakeHandler.removeCallbacksAndMessages(null);
        if (registered) { unregisterReceiver(screen); registered=false; }
        if (panel != null) { panel.stop(); panel=null; }
        super.onDestroy();
    }
}
