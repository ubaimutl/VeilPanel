package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.*;

/** Insets sensor below SystemUI. Accessibility overlays are above the status
 * bar and therefore do not receive its visibility in their own inset state. */
final class FullscreenBarMonitor {
    /** navigationPresent=false means the display has no navigation inset source
     * (gesture replacements, hidden navigation). Bar visibility alone then cannot
     * tell immersive apps from apps that hide only the status bar. */
    interface Listener { void changed(boolean status,boolean navigationVisible,boolean navigationPresent); }
    private final Context context;
    private final WindowManager manager;
    private final Listener changed;
    private View sensor;
    private Boolean lastStatus,lastNavigation,lastPresent;
    FullscreenBarMonitor(Context service, Listener changed) {
        android.view.Display display=service.getSystemService(android.hardware.display.DisplayManager.class).getDisplay(android.view.Display.DEFAULT_DISPLAY);
        context=service.createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,null);
        manager=context.getSystemService(WindowManager.class);this.changed=changed;
    }
    void start() {
        if(sensor!=null||!Settings.canDrawOverlays(context))return;
        sensor=new View(context);
        sensor.setOnApplyWindowInsetsListener((v,insets)->{publish(insets.isVisible(WindowInsets.Type.statusBars()),insets.isVisible(WindowInsets.Type.navigationBars()),navigationPresent(insets));return insets;});
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(1,WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;p.alpha=0;p.setFitInsetsTypes(0);p.setTitle("VeilPanelBarSensor");
        p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        try {manager.addView(sensor,p);sensor.requestApplyInsets();}
        catch(RuntimeException unavailable){sensor=null;publish(true,true,true);}
    }
    // Gesture-navigation replacements may remove the navigation source entirely.
    // An absent bar is not evidence either way about immersive mode.
    private static boolean navigationPresent(WindowInsets insets){
        return insets.isVisible(WindowInsets.Type.navigationBars())
                || !android.graphics.Insets.NONE.equals(insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()));
    }
    private void publish(boolean status,boolean navigation,boolean present){if(lastStatus==null||lastStatus!=status||lastNavigation!=navigation||lastPresent!=present){lastStatus=status;lastNavigation=navigation;lastPresent=present;if(android.util.Log.isLoggable("VEIL_SHADE_DEBUG",android.util.Log.DEBUG))android.util.Log.d("VEIL_SHADE_DEBUG","bar sensor status="+status+" navigation="+navigation+" navigationPresent="+present);changed.changed(status,navigation,present);}}
    void close(){if(sensor!=null){manager.removeViewImmediate(sensor);sensor=null;}}
}
