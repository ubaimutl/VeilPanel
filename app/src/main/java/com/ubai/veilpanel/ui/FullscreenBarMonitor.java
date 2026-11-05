package com.ubai.veilpanel.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.*;
import java.util.function.Consumer;

/** Insets sensor below SystemUI. Accessibility overlays are above the status
 * bar and therefore do not receive its visibility in their own inset state. */
final class FullscreenBarMonitor {
    private final Context context;
    private final WindowManager manager;
    private final Consumer<Boolean> changed;
    private View sensor;
    private Boolean last;
    FullscreenBarMonitor(Context service, Consumer<Boolean> changed) {
        android.view.Display display=service.getSystemService(android.hardware.display.DisplayManager.class).getDisplay(android.view.Display.DEFAULT_DISPLAY);
        context=service.createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,null);
        manager=context.getSystemService(WindowManager.class);this.changed=changed;
    }
    void start() {
        if(sensor!=null||!Settings.canDrawOverlays(context))return;
        sensor=new View(context);
        sensor.setOnApplyWindowInsetsListener((v,insets)->{publish(insets.isVisible(WindowInsets.Type.statusBars()));return insets;});
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(1,1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;p.alpha=0;p.setFitInsetsTypes(0);p.setTitle("VeilPanelBarSensor");
        p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        try {manager.addView(sensor,p);sensor.requestApplyInsets();}
        catch(RuntimeException unavailable){sensor=null;publish(true);}
    }
    private void publish(boolean visible){if(last==null||last!=visible){last=visible;android.util.Log.d("VeilFullscreen","statusBarVisible="+visible);changed.accept(visible);}}
    void close(){if(sensor!=null){manager.removeViewImmediate(sensor);sensor=null;}}
}
