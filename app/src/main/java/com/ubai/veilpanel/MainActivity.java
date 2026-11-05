package com.ubai.veilpanel;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.LocaleManager;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.ubai.veilpanel.config.PanOSPPreferences;
import com.ubai.veilpanel.image.ImageCropActivity;
import com.ubai.veilpanel.image.BackgroundImageStore;
import com.ubai.veilpanel.config.PanOSPConfigBackup;
import com.ubai.veilpanel.notifications.PanOSPNotificationListener;
import com.ubai.veilpanel.ui.PanOSPDialogStyler;
import fr.gibapps.ui.background.GibBackgroundDrawable;

/**
 * PanOSP setup and panel-configuration screen.
 *
 * The accessibility overlay can launch this Activity directly in
 * configuration-only mode. Opening the normal launcher icon still shows the
 * permission/setup section followed by the same panel editor.
 */
public class MainActivity extends Activity {

    public static final String EXTRA_OPEN_PANEL_CONFIGURATION =
            "com.ubai.veilpanel.OPEN_PANEL_CONFIGURATION";

    private static final int REQUEST_TRANSFER_SETTINGS = 2012;
    private static final int REQUEST_CUSTOM_FONT = 2011;
    private static final int REQUEST_POWER_BACKDROP = 2010;
    private static final int REQUEST_CAMERA = 1001;
    private static final int REQUEST_BLUETOOTH = 1002;
    public static final int REQUEST_BACKGROUND_IMAGE = 2001;
    private static final int REQUEST_BACKGROUND_IMAGE_CROP = 2004;
    public static final int REQUEST_EXPORT_CONFIGURATION = 2002;
    public static final int REQUEST_IMPORT_CONFIGURATION = 2003;

    private Button accessibilityButton;
    private Button notificationButton;
    private Button writeSettingsButton;
    private Button cameraButton;
    private Button bluetoothButton;

    private View appearanceRoot;
    private String pendingBackgroundImageUri;
    private static final java.util.concurrent.ExecutorService IMAGE_WORK =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final java.util.concurrent.atomic.AtomicLong IMAGE_OPERATION =
            new java.util.concurrent.atomic.AtomicLong();

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString("pending_background", pendingBackgroundImageUri);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        IMAGE_OPERATION.incrementAndGet();
        if (!isChangingConfigurations()) {
            String pending = pendingBackgroundImageUri;
            pendingBackgroundImageUri = null;
            releaseUnusedPermission(pending);
        }
        super.onDestroy();
    }

    private boolean isCurrentOperation(long operation) {
        return operation == IMAGE_OPERATION.get() && !isFinishing() && !isDestroyed();
    }

    private void releaseUnusedPermission(String uri) {
        if (uri == null || uri.equals(pendingBackgroundImageUri)
                || uri.equals(new PanOSPPreferences(this).getPanelBackgroundImageUri())) return;
        if ("content".equals(Uri.parse(uri).getScheme())) releaseBackgroundImagePermission(uri);
    }

    private void cleanOldBackground(String oldUri, String newUri) {
        if (oldUri == null || oldUri.equals(newUri)) return;
        releaseUnusedPermission(oldUri);
        BackgroundImageStore store = new BackgroundImageStore(this);
        IMAGE_WORK.execute(() -> store.deleteManaged(oldUri));
    }

    private void refreshConfiguration() {
        if (appearanceRoot != null) appearanceRoot.invalidate();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setNavigationBarContrastEnforced(false);

        if (getIntent().getBooleanExtra("com.ubai.veilpanel.INTERNAL_BOOT_REFRESH", false)) {
            finish();
            return;
        }

        if (savedInstanceState != null) pendingBackgroundImageUri = savedInstanceState.getString("pending_background");
        setContentView(buildContent());
        if(savedInstanceState==null&&getIntent().getBooleanExtra("open_transfer_settings",false))startActivityForResult(new Intent(this,SettingsTransferActivity.class),REQUEST_TRANSFER_SETTINGS);
        if(getIntent().getBooleanExtra("open_liquid_settings",false)){
            View section=getWindow().getDecorView().findViewWithTag("section:liquid");
            if(section instanceof com.ubai.veilpanel.ui.SettingsSection){((com.ubai.veilpanel.ui.SettingsSection)section).setExpanded(true,true);section.post(()->section.getParent().requestChildFocus(section,section));}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        PanOSPNotificationListener.reconnectIfNeeded(this);


    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REQUEST_TRANSFER_SETTINGS){recreate();return;}
        if(requestCode==REQUEST_CUSTOM_FONT){
            if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
                Uri selected=data.getData();
                IMAGE_WORK.execute(()->{
                    try{com.ubai.veilpanel.ui.CustomFont.importFont(getApplicationContext(),selected);runOnUiThread(()->{if(!isDestroyed()&&!isFinishing()){android.widget.Toast.makeText(this,"Custom font saved",android.widget.Toast.LENGTH_SHORT).show();recreate();}});}
                    catch(Exception failure){runOnUiThread(()->{if(!isDestroyed()&&!isFinishing())android.widget.Toast.makeText(this,"Couldn't use that font. Choose a valid TTF or OTF file up to 20 MB.",android.widget.Toast.LENGTH_LONG).show();});}
                });
            }return;
        }
        if(requestCode==REQUEST_POWER_BACKDROP){
            if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
                Uri selected=data.getData();
                IMAGE_WORK.execute(()->{
                    try{com.ubai.veilpanel.image.PowerSaverBackdrop.importImage(getApplicationContext(),selected);runOnUiThread(()->{if(!isDestroyed()&&!isFinishing()){android.widget.Toast.makeText(this,"Panel wallpaper saved",android.widget.Toast.LENGTH_SHORT).show();recreate();}});}
                    catch(Exception failure){runOnUiThread(()->{if(!isDestroyed()&&!isFinishing())android.widget.Toast.makeText(this,"Couldn't use that image. Please choose another.",android.widget.Toast.LENGTH_LONG).show();});}
                });
            }return;
        }
        if (requestCode == REQUEST_BACKGROUND_IMAGE_CROP) {
            if (resultCode == RESULT_OK && data != null) {
                handleBackgroundImageCropResult(data);
            } else {
                if (pendingBackgroundImageUri != null) {
                    PanOSPPreferences preferences =
                            new PanOSPPreferences(this);

                    String currentUri =
                            preferences.getPanelBackgroundImageUri();

                    if (!pendingBackgroundImageUri.equals(currentUri)) {
                        releaseBackgroundImagePermission(
                                pendingBackgroundImageUri
                        );
                    }
                }

                pendingBackgroundImageUri = null;
            }
            return;
        }

        if (resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        if (requestCode == REQUEST_BACKGROUND_IMAGE) {
            handleBackgroundImageSelected(data, uri);
            return;
        }

        if (requestCode == REQUEST_EXPORT_CONFIGURATION) {
            exportConfigurationTo(uri);
            return;
        }

        if (requestCode == REQUEST_IMPORT_CONFIGURATION) {
            importConfigurationFrom(uri);
        }
    }

    private void handleBackgroundImageSelected(
            Intent data,
            Uri newUri) {

        PanOSPPreferences preferences = new PanOSPPreferences(this);

        int takeFlags =
                data.getFlags()
                        & Intent.FLAG_GRANT_READ_URI_PERMISSION;

        if (takeFlags == 0) {
            takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        }

        try {
            getContentResolver().takePersistableUriPermission(
                    newUri,
                    takeFlags
            );
        } catch (SecurityException ignored) {
            // The selected provider may already expose a durable grant.
        }

        pendingBackgroundImageUri = newUri.toString();
        openBackgroundImageCropper(newUri);


    }


    private void openBackgroundImageCropper(Uri uri) {
        Intent intent = new Intent(this, ImageCropActivity.class);
        intent.putExtra(
                ImageCropActivity.EXTRA_SOURCE_URI,
                uri.toString()
        );

        float ratio =
                (float) getResources().getDisplayMetrics().widthPixels
                        / (float) getResources().getDisplayMetrics().heightPixels;

        intent.putExtra(
                ImageCropActivity.EXTRA_TARGET_ASPECT_RATIO,
                ratio
        );

        startActivityForResult(
                intent,
                REQUEST_BACKGROUND_IMAGE_CROP
        );
    }

    private void handleBackgroundImageCropResult(Intent data) {
        String sourceUri = data.getStringExtra(
                ImageCropActivity.EXTRA_SOURCE_URI
        );

        if (sourceUri == null || sourceUri.trim().isEmpty()) {
            String abandoned = pendingBackgroundImageUri;
            pendingBackgroundImageUri = null;
            releaseUnusedPermission(abandoned);
            return;
        }

        // Once returned, the crop is no longer awaiting an Activity result.
        pendingBackgroundImageUri = null;
        final long operation = IMAGE_OPERATION.incrementAndGet();
        final java.util.Map<String, Object> values = new java.util.HashMap<>();
        String[] corners = {"top_left", "top_right", "bottom_left", "bottom_right"};
        float[] defaults = {0, 0, 1, 0, 0, 1, 1, 1};
        int index = 0;
        for (String corner : corners) {
            for (String axis : new String[]{"x", "y"}) {
                float uv = data.getFloatExtra("uv_" + corner + "_" + axis, defaults[index++]);
                if (!Float.isFinite(uv)) {
                    pendingBackgroundImageUri = null;
                    releaseUnusedPermission(sourceUri);
                    showConfigBackupMessage(R.string.panel_config_import_failed);
                    return;
                }
                values.put("panel_background_image_uv_" + corner + "_" + axis, uv);
            }
        }
        BackgroundImageStore store = new BackgroundImageStore(this);
        IMAGE_WORK.execute(() -> {
            try {
                String prepared = store.prepare(Uri.parse(sourceUri));
                runOnUiThread(() -> {
                    if (!isCurrentOperation(operation)) {
                        IMAGE_WORK.execute(() -> store.deleteManaged(prepared));
                        releaseUnusedPermission(sourceUri);
                        return;
                    }
                    PanOSPPreferences preferences = new PanOSPPreferences(this);
                    String old = preferences.getPanelBackgroundImageUri();
                    values.put(PanOSPConfigBackup.IMAGE_URI, prepared);
                    pendingBackgroundImageUri = null;
                    if (preferences.publishConfiguration(values)) {
                        cleanOldBackground(old, prepared);
                        releaseUnusedPermission(sourceUri);
                        refreshConfiguration();
                    } else {
                        IMAGE_WORK.execute(() -> store.deleteManaged(prepared));
                        releaseUnusedPermission(sourceUri);
                        showConfigBackupMessage(R.string.panel_config_import_failed);
                    }
                });
            } catch (Exception | OutOfMemoryError error) {
                runOnUiThread(() -> {
                    if (isCurrentOperation(operation)) {
                        pendingBackgroundImageUri = null;
                        showConfigBackupMessage(R.string.panel_config_import_failed);
                    }
                    releaseUnusedPermission(sourceUri);
                });
            }
        });
    }


    private View buildContent() {
        // Legacy editor extras intentionally route to the notification settings.

        LinearLayout windowRoot = new LinearLayout(this);
        appearanceRoot = windowRoot;
        windowRoot.setOrientation(LinearLayout.VERTICAL);
        PanOSPPreferences appearance = new PanOSPPreferences(this);
        windowRoot.setBackground(new GibBackgroundDrawable(
                appearance.getThemeBackgroundType(),
                appearance.getThemeBackgroundColor(),
                appearance.getThemeBackgroundColor2(),
                appearance.getThemeBackgroundDirection(),
                0f));

        View statusBarScrim = new View(this);
        statusBarScrim.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        View navigationBarScrim = new View(this);
        navigationBarScrim.setBackgroundColor(0xff101216);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);scrollView.setVerticalScrollBarEnabled(false);scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        final int horizontalPadding = dp(24);
        final int topPadding = dp(28);
        final int baseBottomPadding = dp(28);

        root.setPadding(
                horizontalPadding,
                topPadding,
                horizontalPadding,
                baseBottomPadding
        );

        windowRoot.addView(
                statusBarScrim,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0
                )
        );

        windowRoot.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        windowRoot.addView(
                navigationBarScrim,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0
                )
        );

        windowRoot.setOnApplyWindowInsetsListener((view, insets) -> {
            android.view.WindowInsetsController controller = view.getWindowInsetsController();
            if (controller != null) controller.setSystemBarsAppearance(0,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            int statusTop =
                    insets.getInsets(
                            WindowInsets.Type.statusBars()
                    ).top;

            int navigationBottom =
                    insets.getInsets(
                            WindowInsets.Type.navigationBars()
                    ).bottom;

            ViewGroup.LayoutParams statusParams =
                    statusBarScrim.getLayoutParams();
            statusParams.height = statusTop;
            statusBarScrim.setLayoutParams(statusParams);

            ViewGroup.LayoutParams navigationParams =
                    navigationBarScrim.getLayoutParams();
            navigationParams.height = navigationBottom;
            navigationBarScrim.setLayoutParams(navigationParams);

            return insets;
        });


        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(com.ubai.veilpanel.ui.PanOSPTheme.primaryText(this));
        title.setGravity(Gravity.START);title.setTextSize(30);title.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));root.addView(title);

        addSetupContent(root);

        windowRoot.setBackgroundColor(com.ubai.veilpanel.ui.PanOSPTheme.panelBackground(this));
        scrollView.addView(root);


        windowRoot.post(windowRoot::requestApplyInsets);
        com.ubai.veilpanel.ui.PanelStyle.apply(windowRoot);
        return windowRoot;
    }

    public void refreshGlobalAppearance() {
        if (appearanceRoot == null) return;
        PanOSPPreferences appearance = new PanOSPPreferences(this);
        appearanceRoot.setBackground(new GibBackgroundDrawable(
                appearance.getThemeBackgroundType(),
                appearance.getThemeBackgroundColor(),
                appearance.getThemeBackgroundColor2(),
                appearance.getThemeBackgroundDirection(),
                0f));
    }

    private LinearLayout section(LinearLayout root,String key,String title,String summary,boolean initial) {
        com.ubai.veilpanel.ui.SettingsSection section=new com.ubai.veilpanel.ui.SettingsSection(this,key,title,summary,initial);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(12);root.addView(section,params);return section.content();
    }
    private void help(LinearLayout root,String text){TextView note=new TextView(this);note.setText(text);note.setTextSize(14);note.setTextColor(com.ubai.veilpanel.ui.PanOSPTheme.secondaryText(this));note.setPadding(0,dp(8),0,dp(12));root.addView(note);}
    private Button action(LinearLayout root,String title,Runnable run){
        Button button=new Button(this);button.setText(title);styleAction(button);button.setOnClickListener(v->run.run());LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(8);root.addView(button,params);return button;
    }
    private void styleAction(Button button){
        button.setAllCaps(false);button.setTextSize(15);button.setTextColor(com.ubai.veilpanel.ui.PanOSPTheme.primaryText(this));button.setMinHeight(dp(48));button.setMinimumHeight(dp(48));button.setPadding(dp(16),dp(12),dp(16),dp(12));button.setStateListAnimator(null);
        GradientDrawable background=new GradientDrawable();background.setColor(com.ubai.veilpanel.ui.PanOSPTheme.blend(com.ubai.veilpanel.ui.PanOSPTheme.panelBackground(this),com.ubai.veilpanel.ui.PanOSPTheme.primaryText(this),.09f));background.setCornerRadius(dp(12));button.setBackground(background);
    }
    private void addOption(LinearLayout root,String key){for(com.ubai.veilpanel.config.PanelAppearance.Option option:com.ubai.veilpanel.config.PanelAppearance.OPTIONS)if(option.key.equals(key)){addGlassSlider(root,option.label,option.min,option.max,option.key,option.initial);return;}}
    private void addThemePresets(LinearLayout root){
        LinearLayout box=new LinearLayout(this);box.setTag("theme_presets");box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(18),dp(16),dp(16));
        int accent=com.ubai.veilpanel.ui.PanOSPTheme.accent(this),background=com.ubai.veilpanel.ui.PanOSPTheme.panelBackground(this);
        GradientDrawable surface=new GradientDrawable();surface.setColor(com.ubai.veilpanel.ui.PanOSPTheme.blend(background,accent,.09f));surface.setCornerRadius(dp(20));surface.setStroke(dp(1),com.ubai.veilpanel.ui.PanOSPTheme.blend(background,accent,.5f));box.setBackground(surface);
        TextView heading=new TextView(this);heading.setText("Start with a theme");heading.setTextSize(21);heading.setTextColor(com.ubai.veilpanel.ui.PanOSPTheme.primaryText(this));heading.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL),600,false));box.addView(heading);
        help(box,"Choose a look, then fine-tune it below. Themes change appearance only. Undo brings back your previous look.");
        for(int row=0;row<2;row++){
            LinearLayout choices=new LinearLayout(this);box.addView(choices,new LinearLayout.LayoutParams(-1,-2));
            for(int column=0;column<2;column++){
                final int index=row*2+column;Button choice=new Button(this);styleAction(choice);
                choice.setTag("theme:"+com.ubai.veilpanel.config.PanelThemePresets.IDS[index]);
                choice.setText(com.ubai.veilpanel.config.PanelThemePresets.NAMES[index]+"\n"+com.ubai.veilpanel.config.PanelThemePresets.DESCRIPTIONS[index]);choice.setMinHeight(dp(100));choice.setTextSize(14);
                LinearLayout.LayoutParams placement=new LinearLayout.LayoutParams(0,-2,1);placement.topMargin=dp(8);if(column==0)placement.setMarginEnd(dp(8));choices.addView(choice,placement);
                choice.setOnClickListener(v->{try{
                    com.ubai.veilpanel.config.PanelThemePresets.apply(this,com.ubai.veilpanel.config.PanelThemePresets.IDS[index]);
                    android.widget.Toast.makeText(this,com.ubai.veilpanel.config.PanelThemePresets.NAMES[index]+" theme applied",android.widget.Toast.LENGTH_SHORT).show();recreate();
                }catch(IllegalStateException unavailable){android.widget.Toast.makeText(this,"Could not save your previous look. Theme kept unchanged.",android.widget.Toast.LENGTH_LONG).show();}});
            }
        }
        help(box,"Glass uses your saved panel wallpaper; without one, cards use a frosted fallback. Gestures, recovery options and access stay as configured.");
        if(com.ubai.veilpanel.config.PanelThemePresets.canUndo(this)){
            Button undo=action(box,"Undo last theme",()->{if(com.ubai.veilpanel.config.PanelThemePresets.undo(this))recreate();else android.widget.Toast.makeText(this,"Previous look could not be restored",android.widget.Toast.LENGTH_SHORT).show();});undo.setTag("theme:undo");
        }
        LinearLayout.LayoutParams placement=new LinearLayout.LayoutParams(-1,-2);placement.bottomMargin=dp(12);root.addView(box,placement);
    }

    private void addSetupContent(LinearLayout root) {
        addThemePresets(root);
        help(root,"Customize your notification panel. Your changes apply the next time it opens.");
        action(root,getString(R.string.shortcut_open_notifications),()->{if(!com.ubai.veilpanel.accessibility.PanOSPAccessibilityServiceCore.requestPanelOpen())android.widget.Toast.makeText(this,R.string.shortcut_enable_service,android.widget.Toast.LENGTH_LONG).show();});
        boolean ready=isAccessibilityServiceEnabled()&&isNotificationListenerEnabled()&&Settings.canDrawOverlays(this);
        LinearLayout access=section(root,"access","Setup & access",ready?"Required access is enabled":"Enable the panel and its wallpaper",!ready);
        accessibilityButton=addSetupButton(access,R.string.setup_accessibility,v->openAccessibilitySettings());styleAction(accessibilityButton);
        notificationButton=addSetupButton(access,R.string.setup_notification_access,v->openNotificationListenerSettings());styleAction(notificationButton);
        action(access,Settings.canDrawOverlays(this)?"Display over apps · Enabled":"Enable display over apps",()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        help(access,"Accessibility handles gestures. Notification access supplies the cards. Display-over-apps access and a saved wallpaper let apps keep playing underneath. The lock screen stays with Android.");
        action(access,"Pause panel",()->sendBroadcast(new Intent(getPackageName()+".PAUSE").setPackage(getPackageName())));

        LinearLayout gestures=section(root,"gestures","Gestures & shortcuts","Pull-down split and launcher actions",false);
        help(gestures,"Pull from the left for notifications and from the right for your original quick settings.");
        addGlassSlider(gestures,"Left notification area (%)",20,80,"split",50);
        addPanelSwitch(gestures,"Two swipes in fullscreen apps","fullscreen_two_swipe",true);
        help(gestures,"The first fullscreen pull reveals Android's status bar. A fresh second pull opens Veil. A top-edge tap does not open Veil while the bar is hidden.");
        help(gestures,"In any compatible launcher, choose this app’s Open notifications shortcut. No particular launcher is required.");

        LinearLayout handle=section(root,"handle","Dismiss handle","Line position, length and grab area",false);
        addGlassSlider(handle,"Line length",24,100,"dismiss_handle_width",48);
        addGlassSlider(handle,"Distance from bottom",0,100,"dismiss_handle_bottom",42);
        addGlassSlider(handle,"Grab width",96,320,"dismiss_grab_width",220);
        addGlassSlider(handle,"Grab height",48,100,"dismiss_grab_height",72);
        help(handle,"The grab area is invisible and larger than the line. Drag up to close, pull back down to return, or tap to dismiss. A bottom distance of zero puts the line at the edge.");

        LinearLayout wallpaper=section(root,"wallpaper","Wallpaper & glass","Background, blur and card material",false);
        action(wallpaper,com.ubai.veilpanel.image.PowerSaverBackdrop.available(this)?"Replace panel wallpaper":"Choose panel wallpaper",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),REQUEST_POWER_BACKDROP));
        help(wallpaper,"Choose the original image used on Home. A private copy keeps the same background over every app, covers popup banners and works in power saver.");
        addGlassSlider(wallpaper,"Wallpaper blur",0,64,"blur",24);
        addPanelSwitch(wallpaper,"Follow wallpaper colours","dynamic",true);
        addGlassSlider(wallpaper,"Card opacity (%)",45,100,"glass_opacity",66);
        addPanelSwitch(wallpaper,"Frosted glass cards","reference_glass",false);
        addPanelSwitch(wallpaper,"Experimental glass material","experimental_glass",false);
        addPanelSwitch(wallpaper,"Borders around controls","control_borders",false);
        addGlassSlider(wallpaper,"Control border size",24,36,"control_surface_size",28);
        addPanelSwitch(wallpaper,"Show status bar","show_status_bar",true);
        addPanelSwitch(wallpaper,"Scroll beneath status icons","scroll_under_status_bar",true);
        help(wallpaper,"Let scrolling content pass behind the status icons. Turn off to keep a wallpaper strip above the content.");

        LinearLayout liquid=section(root,"liquid","Liquid Glass experiment","Kyant lens, wallpaper refraction and edge light",false);
        addPanelSwitch(liquid,"Use Kyant lens cards","kyant_liquid_glass",false);
        help(liquid,"An optional adaptation of AndroidLiquidGlass. Uses your saved panel wallpaper for refraction and blur, including in power saver. Text and icons stay sharp. Turn it off to return to your existing card style. Choose a panel wallpaper first.");
        addGlassSlider(liquid,"Refraction strength",0,32,"liquid_refraction",12);
        addGlassSlider(liquid,"Lens edge width",2,36,"liquid_edge",18);
        addGlassSlider(liquid,"Glass blur",0,64,"liquid_blur",20);
        addGlassSlider(liquid,"Glass tint (%)",0,75,"liquid_tint",24);
        addGlassSlider(liquid,"Edge highlight (%)",0,100,"liquid_highlight",45);
        addPanelSwitch(liquid,"Colour dispersion","liquid_dispersion",true);
        addPanelSwitch(liquid,"Lens depth","liquid_depth",true);
        action(liquid,"Preview notification panel",()->{if(!com.ubai.veilpanel.accessibility.PanOSPAccessibilityServiceCore.requestPanelOpen())android.widget.Toast.makeText(this,R.string.shortcut_enable_service,android.widget.Toast.LENGTH_LONG).show();});
        action(liquid,"Source & license",()->{try{String notice;try(java.io.InputStream input=getAssets().open("third_party/android-liquid-glass/NOTICE.txt")){notice=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}String license;try(java.io.InputStream input=getAssets().open("third_party/android-liquid-glass/LICENSE.txt")){license=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}new AlertDialog.Builder(this).setTitle("AndroidLiquidGlass · Kyant").setMessage(notice+"\n"+license).setPositiveButton(android.R.string.ok,null).setNeutralButton("Project",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/Kyant0/AndroidLiquidGlass")))).show();}catch(java.io.IOException unavailable){android.widget.Toast.makeText(this,"License file unavailable",android.widget.Toast.LENGTH_SHORT).show();}});

        LinearLayout notifications=section(root,"notifications","Notifications","Preview length and expanded messages",false);
        addGlassSlider(notifications,"Description brightness (%)",0,100,"description_brightness",0);
        help(notifications,"Zero keeps your current message colour. Increase toward white for brighter descriptions; titles and timestamps keep their own colours.");
        addOption(notifications,"preview_lines");addOption(notifications,"expanded_preview_lines");
        help(notifications,"Hold a card for its preview and actions. Silent notifications stay collapsed at the bottom. Clear all keeps notifications Android marks as ongoing.");

        LinearLayout header=section(root,"header","Clock & header","Visibility, alignment and clock style",false);
        addPanelSwitch(header,"Show clock","header_clock_visible",true);
        addPanelSwitch(header,"Leading zero in clock","header_clock_leading_zero",false);
        addPanelSwitch(header,"Show date","header_date_visible",true);
        addPanelSwitch(header,"Battery beside date","header_battery_visible",true);
        addPanelSwitch(header,"Show settings button","header_settings_visible",true);
        addPanelSwitch(header,"Show close button","header_close_visible",true);
        addHeaderChoice(header,"Clock alignment","header_clock_alignment",new String[]{"Start","Centre","End"},new String[]{"start","center","end"},"start");
        addHeaderChoice(header,"Date alignment","header_date_alignment",new String[]{"Start","Centre","End"},new String[]{"start","center","end"},"start");
        addHeaderChoice(header,"Clock style","header_clock_style",new String[]{"Plain","Sculpted light","Liquid glass"},new String[]{"plain","sculpted","liquid"},"plain");
        addOption(header,"clock_size");
        addGlassSlider(header,"Clock font weight",100,900,"header_clock_weight",500);
        addGlassSlider(header,"Clock digit spacing",0,100,"header_clock_spacing",25);
        help(header,"50 is normal digit spacing. Lower values bring the digits closer together.");
        addGlassSlider(header,"Space above clock",-80,120,"header_clock_top_space",4);
        addGlassSlider(header,"Space below clock",-80,120,"header_clock_bottom_space",com.ubai.veilpanel.config.PanelAppearance.get(this,"header_gap"));
        help(header,"Negative spacing pulls the clock closer to the date or brings notifications closer beneath it.");
        addGlassSlider(header,"Date font size",12,28,"header_date_size",17);
        addGlassSlider(header,"Date font weight",100,900,"header_date_weight",400);
        addGlassSlider(header,"Clock glass frost (%)",0,100,"clock_glass_frost",35);
        addGlassSlider(header,"Clock glass edge light (%)",0,100,"clock_glass_highlight",70);
        addGlassSlider(header,"Clock glass refraction",0,20,"clock_glass_refraction",8);
        addGlassSlider(header,"Clock glass bevel width",1,8,"clock_glass_bevel",3);
        help(header,"Liquid glass samples the saved panel wallpaper inside the digits. It works independently of glass cards and uses their Glass blur setting. Larger clock sizes show the edge light more clearly. Without a saved wallpaper or shader support, shaded lettering is used.");
        help(header,"For a large shaded clock, choose Sculpted light, centre the clock and increase its size. Hidden controls remain available from the app; the dismiss handle still closes the panel.");

        LinearLayout type=section(root,"typography","Typography & icons","Fonts, text sizes and notification icons",false);
        action(type,"Font family",()->{String[] labels={"System","Josefin Sans","Serif","Saved custom font"},values={"system","josefin","serif","custom"};new AlertDialog.Builder(this).setTitle("Font family").setItems(labels,(dialog,which)->{getSharedPreferences("veil_panel",0).edit().putString("font_family",values[which]).apply();recreate();}).show();});
        action(type,"Choose custom font (TTF / OTF)",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),REQUEST_CUSTOM_FONT));
        help(type,"Choose a TTF or OTF file up to 20 MB. A private copy is saved for offline use. Font family lets you switch back at any time.");
        action(type,"Remove saved custom font",()->new AlertDialog.Builder(this).setTitle("Remove custom font?").setMessage("The panel will return to the system font. Your original file stays unchanged.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{com.ubai.veilpanel.ui.CustomFont.clear(this);recreate();}).show());
        for(String key:new String[]{"headline_size","body_size","time_size","font_weight","icon_size","icon_corner"})addOption(type,key);
        addGlassSlider(type,"Control icon size",16,26,"control_icon_size",18);

        LinearLayout layout=section(root,"layout","Spacing & layout","Card dimensions, margins and alignment",false);
        for(String key:new String[]{"card_width","card_height","card_padding","card_gap","card_radius","side_margin","icon_gap","header_gap"})addOption(layout,key);

        LinearLayout motion=section(root,"motion","Motion","Curtain gestures and list springs",false);
        addPanelSwitch(motion,"Curtain opening and drag dismissal","curtain_motion",true);
        addPanelSwitch(motion,"Recover native shade after lost gestures","native_edge_recovery",true);
        help(motion,"Enabled by default for phones where Android takes over an extreme-edge pull. Disable if your OS does not need this recovery. Ordinary pull-down handling stays active.");
        addPanelSwitch(motion,"Cover status area during edge recovery","recovery_status_cover",true);
        help(motion,"Temporarily covers the native status strip after Android takes over an extreme-edge pull. Real status icons return after recovery settles. Experimental; disable to restore the previous behaviour.");
        addPanelSwitch(motion,"Elastic bounce at list edges","experimental_motion",false);
        help(motion,"Off keeps the list stationary when it reaches either end. Enable for an optional elastic pull and spring return.");
        addGlassSlider(motion,"List spring damping (%)",70,110,"motion_damping",90);

        LinearLayout advanced=section(root,"advanced","Advanced & about","Language, defaults and app information",false);
        action(advanced,"Backup & transfer settings",()->startActivityForResult(new Intent(this,SettingsTransferActivity.class),REQUEST_TRANSFER_SETTINGS));
        addLanguageControl(advanced);
        addPanelSwitch(advanced,"Preview saved wallpaper in legacy backdrop","fallback_always",false);
        help(advanced,"The legacy backdrop is used if the wallpaper or display-over-apps access is missing. It can pause the app underneath.");
        action(advanced,"Reset layout sizes",()->new AlertDialog.Builder(this).setTitle("Reset layout sizes?").setMessage("Reset card, text and icon sizes. Keep your wallpaper, colours and gestures.").setNegativeButton(android.R.string.cancel,null).setPositiveButton("Reset",(d,w)->{com.ubai.veilpanel.config.PanelAppearance.reset(this);recreate();}).show());
        action(advanced,"Remove saved wallpaper",()->new AlertDialog.Builder(this).setTitle("Remove panel wallpaper?").setMessage("Delete the private saved copy. The panel will use its legacy backdrop until you choose an image again.").setNegativeButton(android.R.string.cancel,null).setPositiveButton("Remove",(d,w)->IMAGE_WORK.execute(()->{com.ubai.veilpanel.image.PowerSaverBackdrop.clear(getApplicationContext());runOnUiThread(()->{if(!isDestroyed()&&!isFinishing())recreate();});})).show());
        help(advanced,"Veil Panel · 1.0.0\nBased on PanOSP · GPL-3.0-or-later\nNo Internet permission. Wallpaper and notification data stay on your device.");
    }

    private void addHeaderChoice(LinearLayout parent,String title,String key,String[] labels,String[] values,String initial){
        String current=getSharedPreferences("veil_panel",0).getString(key,initial);int selected=0;
        for(int i=0;i<values.length;i++)if(values[i].equals(current))selected=i;
        Button row=action(parent,title+": "+labels[selected],()->{});row.setTag("choice:"+key);
        row.setOnClickListener(v->{
            String value=getSharedPreferences("veil_panel",0).getString(key,initial);int checked=0;
            for(int i=0;i<values.length;i++)if(values[i].equals(value))checked=i;
            new AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(labels,checked,(dialog,which)->{
                getSharedPreferences("veil_panel",0).edit().putString(key,values[which]).apply();
                row.setText(title+": "+labels[which]);dialog.dismiss();
            }).setNegativeButton(android.R.string.cancel,null).show();
        });
    }

    private void addPanelSwitch(LinearLayout root,String title,String key,boolean initial){
        android.widget.Switch toggle=new android.widget.Switch(this);toggle.setTag("setting:"+key);toggle.setText(title);toggle.setTextSize(15);toggle.setTextColor(com.ubai.veilpanel.ui.PanOSPTheme.primaryText(this));toggle.setMinimumHeight(dp(56));toggle.setPadding(0,dp(8),0,dp(8));toggle.setSwitchPadding(dp(16));toggle.setChecked(getSharedPreferences("veil_panel",0).getBoolean(key,initial));toggle.setOnCheckedChangeListener((v,value)->{getSharedPreferences("veil_panel",0).edit().putBoolean(key,value).apply();if("dynamic".equals(key))recreate();});root.addView(toggle,new LinearLayout.LayoutParams(-1,-2));
    }
    private void addGlassSlider(LinearLayout root,String title,int min,int max,String key,int initial) {
        LinearLayout setting=new LinearLayout(this);setting.setTag("setting:"+key);setting.setOrientation(LinearLayout.VERTICAL);setting.setPadding(0,dp(12),0,dp(8));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView label=new TextView(this);label.setText(title);label.setTextSize(14);label.setTextColor(com.ubai.veilpanel.ui.PanOSPTheme.primaryText(this));row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        TextView value=new TextView(this);value.setTextSize(14);value.setTextColor(com.ubai.veilpanel.ui.PanOSPTheme.accent(this));value.setGravity(Gravity.CENTER);value.setMinWidth(dp(48));value.setMinHeight(dp(44));value.setClickable(true);value.setFocusable(true);row.addView(value,new LinearLayout.LayoutParams(-2,-2));setting.addView(row);
        android.widget.SeekBar seek=new android.widget.SeekBar(this);seek.setTag("value:"+key);seek.setMin(min);seek.setMax(max);seek.setProgress(getSharedPreferences("veil_panel",0).getInt(key,initial));seek.setMinimumHeight(dp(40));value.setText(String.valueOf(seek.getProgress()));value.setContentDescription(title+", "+seek.getProgress()+". Tap to enter a value.");setting.addView(seek,new LinearLayout.LayoutParams(-1,-2));root.addView(setting);
        value.setOnClickListener(v->{android.widget.EditText input=new android.widget.EditText(this);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|(min<0?android.text.InputType.TYPE_NUMBER_FLAG_SIGNED:0));input.setText(String.valueOf(seek.getProgress()));input.selectAll();new AlertDialog.Builder(this).setTitle(title+" ("+min+"–"+max+")").setView(input).setNegativeButton(android.R.string.cancel,null).setPositiveButton("Apply",(dialog,which)->{try{seek.setProgress(Math.max(min,Math.min(max,Integer.parseInt(input.getText().toString().trim()))));}catch(NumberFormatException ignored){}}).show();});
        seek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(android.widget.SeekBar bar,int next,boolean user){value.setText(String.valueOf(next));value.setContentDescription(title+", "+next+". Tap to enter a value.");getSharedPreferences("veil_panel",0).edit().putInt(key,next).apply();}
            public void onStartTrackingTouch(android.widget.SeekBar bar){}public void onStopTrackingTouch(android.widget.SeekBar bar){}
        });
    }

    private void addLanguageControl(LinearLayout root) {
        TextView heading = new TextView(this);
        heading.setText(R.string.app_language);
        heading.setTextColor(com.ubai.veilpanel.ui.PanOSPTheme.secondaryText(this));
        heading.setTextSize(16);
        heading.setPadding(0, dp(24), 0, dp(4));
        root.addView(heading);

        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(getString(R.string.app_language_value, getCurrentLanguageLabel()));
        button.setTextColor(com.ubai.veilpanel.ui.PanOSPTheme.primaryText(this));
        button.setMinHeight(dp(52));
        GradientDrawable languageBackground = new GradientDrawable();
        languageBackground.setColor(Color.argb(20, 255, 255, 255));
        languageBackground.setCornerRadius(dp(14));
        languageBackground.setStroke(dp(1), Color.argb(40, 255, 255, 255));
        button.setBackground(languageBackground);
        button.setStateListAnimator(null);
        button.setOnClickListener(v -> showLanguageDialog());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(4);
        root.addView(button, params);
    }

    public String getCurrentLanguageLabel() {
        LocaleManager manager = (LocaleManager) getSystemService(LocaleManager.class);
        if (manager == null) return getString(R.string.language_system);

        LocaleList locales = manager.getApplicationLocales();
        if (locales.isEmpty()) return getString(R.string.language_system);

        switch (locales.get(0).getLanguage()) {
            case "en": return "English";
            case "fr": return "Français";
            case "es": return "Español";
            case "pt": return "Português";
            case "it": return "Italiano";
            case "de": return "Deutsch";
            default: return getString(R.string.language_system);
        }
    }

    public void showLanguageDialog() {
        final String[] tags = {"", "en", "fr", "es", "pt", "it", "de"};
        final String[] labels = {
                getString(R.string.language_system),
                "English", "Français", "Español", "Português", "Italiano", "Deutsch"
        };

        LocaleManager manager = (LocaleManager) getSystemService(LocaleManager.class);
        if (manager == null) return;

        LocaleList current = manager.getApplicationLocales();
        String currentTag = current.isEmpty() ? "" : current.get(0).getLanguage();
        int checked = 0;
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].equals(currentTag)) {
                checked = i;
                break;
            }
        }

        PanOSPDialogStyler.show(new AlertDialog.Builder(this)
                .setTitle(R.string.app_language)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    LocaleList selected = tags[which].isEmpty()
                            ? LocaleList.getEmptyLocaleList()
                            : LocaleList.forLanguageTags(tags[which]);
                    manager.setApplicationLocales(selected);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null));
    }

    private Button addSetupButton(
            LinearLayout root,
            int labelRes,
            android.view.View.OnClickListener listener) {

        Button button = new Button(this);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        button.setTextColor(com.ubai.veilpanel.ui.PanOSPTheme.primaryText(this));
        button.setMinHeight(dp(52));
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(Color.argb(20, 255, 255, 255));
        buttonBackground.setCornerRadius(dp(14));
        buttonBackground.setStroke(dp(1), Color.argb(40, 255, 255, 255));
        button.setBackground(buttonBackground);
        button.setStateListAnimator(null);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        params.topMargin = dp(8);

        root.addView(button, params);
        button.setTag(labelRes);
        return button;
    }

    private void refreshStatus() {
        if (accessibilityButton != null) {
            setStatus(
                    accessibilityButton,
                    isAccessibilityServiceEnabled()
            );
        }

        if (notificationButton != null) {
            setStatus(
                    notificationButton,
                    isNotificationListenerEnabled()
            );
        }

        if (writeSettingsButton != null) {
            setStatus(
                    writeSettingsButton,
                    Settings.System.canWrite(this)
            );
        }

        if (cameraButton != null) {
            setStatus(
                    cameraButton,
                    checkSelfPermission(Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
            );
        }

        if (bluetoothButton != null) {
            setStatus(
                    bluetoothButton,
                    checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED
            );
        }
    }

    private void setStatus(Button button, boolean enabled) {
        if (button == null) {
            return;
        }

        int labelRes = (int) button.getTag();
        button.setText(
                getString(
                        R.string.setup_item_status,
                        getString(labelRes),
                        getString(enabled
                                ? R.string.status_enabled
                                : R.string.status_required)
                )
        );
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager manager =
                (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);

        if (manager == null) {
            return false;
        }

        List<AccessibilityServiceInfo> services =
                manager.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfo.FEEDBACK_ALL_MASK
                );

        String expectedPackage = getPackageName();
        String expectedClass = PanOSPAccessibilityService.class.getName();

        for (AccessibilityServiceInfo info : services) {
            if (info.getResolveInfo() == null
                    || info.getResolveInfo().serviceInfo == null) {
                continue;
            }

            String packageName =
                    info.getResolveInfo().serviceInfo.packageName;
            String className =
                    info.getResolveInfo().serviceInfo.name;

            if (expectedPackage.equals(packageName)
                    && expectedClass.equals(className)) {
                return true;
            }
        }

        return false;
    }

    private boolean isNotificationListenerEnabled() {
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (manager == null) {
            return false;
        }

        ComponentName component =
                new ComponentName(this, PanOSPNotificationListener.class);

        return manager.isNotificationListenerAccessGranted(component);
    }

    private void openAccessibilitySettings() {
        startActivitySafely(
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        );
    }

    private void openNotificationListenerSettings() {
        ComponentName component =
                new ComponentName(this, PanOSPNotificationListener.class);

        Intent intent =
                new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);

        intent.putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                component.flattenToString()
        );

        if (!startActivitySafely(intent)) {
            startActivitySafely(
                    new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            );
        }
    }

    private void openWriteSettings() {
        Intent intent =
                new Intent(
                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName())
                );

        startActivitySafely(intent);
    }

    public void exportPanelConfiguration() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, "PanOSP-config.zip");
        startActivityForResult(intent, REQUEST_EXPORT_CONFIGURATION);
    }

    public void importPanelConfiguration() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "application/zip", "application/x-zip-compressed", "application/octet-stream"});
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_IMPORT_CONFIGURATION);
    }

    private void exportConfigurationTo(Uri uri) {
        try {
            String snapshot = PanOSPConfigBackup.exportToJson(new PanOSPPreferences(this));
            BackgroundImageStore store = new BackgroundImageStore(this);
            IMAGE_WORK.execute(() -> {
                int message;
                try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                    if (output == null) throw new java.io.IOException("Cannot open destination");
                    PanOSPConfigBackup.writeBackup(output, snapshot, store);
                    message = R.string.panel_config_export_success;
                } catch (Exception | OutOfMemoryError error) {
                    message = R.string.panel_config_export_failed;
                }
                final int result = message;
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) showConfigBackupMessage(result);
                });
            });
        } catch (Exception error) { showConfigBackupMessage(R.string.panel_config_export_failed); }
    }

    private void importConfigurationFrom(Uri uri) {
        String abandoned = pendingBackgroundImageUri;
        pendingBackgroundImageUri = null;
        releaseUnusedPermission(abandoned);
        final long operation = IMAGE_OPERATION.incrementAndGet();
        final String previous = new PanOSPPreferences(this).getPanelBackgroundImageUri();
        BackgroundImageStore store = new BackgroundImageStore(this);
        IMAGE_WORK.execute(() -> {
            PanOSPConfigBackup.Restore candidate = null;
            try {
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input == null) throw new java.io.IOException("Cannot open backup");
                    candidate = PanOSPConfigBackup.readBackup(input, store, previous);
                }
                final PanOSPConfigBackup.Restore restore = candidate;
                runOnUiThread(() -> {
                    if (!isCurrentOperation(operation)) {
                        IMAGE_WORK.execute(() -> store.deleteManaged(restore.preparedUri));
                        return;
                    }
                    PanOSPPreferences preferences = new PanOSPPreferences(this);
                    String old = preferences.getPanelBackgroundImageUri();
                    if (preferences.publishConfiguration(restore.values)) {
                        cleanOldBackground(old, restore.preparedUri);
                        releaseUnusedPermission(restore.externalUri);
                        refreshConfiguration();
                        showConfigBackupMessage(R.string.panel_config_import_success);
                    } else {
                        IMAGE_WORK.execute(() -> store.deleteManaged(restore.preparedUri));
                        showConfigBackupMessage(R.string.panel_config_import_failed);
                    }
                });
            } catch (Exception | OutOfMemoryError error) {
                if (candidate != null) store.deleteManaged(candidate.preparedUri);
                runOnUiThread(() -> {
                    if (!isCurrentOperation(operation)) return;
                    if (error instanceof PanOSPConfigBackup.MissingImageException) showBackgroundReselectionDialog();
                    else showConfigBackupMessage(R.string.panel_config_import_failed);
                });
            }
        });
    }

    private void showBackgroundReselectionDialog() {
        PanOSPDialogStyler.show(new AlertDialog.Builder(this)
                .setMessage(
                        R.string.panel_config_background_unavailable
                )
                .setPositiveButton(
                        R.string.panel_config_background_reselect,
                        (dialog, which) -> selectPanelBackgroundImage()
                )
                .setNegativeButton(R.string.panel_config_background_later, null));
    }

    private void showConfigBackupMessage(int messageRes) {
        PanOSPDialogStyler.show(new AlertDialog.Builder(this)
                .setMessage(messageRes)
                .setPositiveButton(android.R.string.ok, null));
    }

    public void selectPanelBackgroundImage() {
        IMAGE_OPERATION.incrementAndGet();
        String abandoned = pendingBackgroundImageUri;
        pendingBackgroundImageUri = null;
        releaseUnusedPermission(abandoned);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                REQUEST_BACKGROUND_IMAGE
        );
    }

    public void clearPanelBackgroundImage() {
        IMAGE_OPERATION.incrementAndGet();
        String abandoned = pendingBackgroundImageUri;
        pendingBackgroundImageUri = null;
        releaseUnusedPermission(abandoned);
        PanOSPPreferences preferences = new PanOSPPreferences(this);
        String old = preferences.getPanelBackgroundImageUri();
        java.util.Map<String, Object> values = new java.util.HashMap<>();
        values.put(PanOSPConfigBackup.IMAGE_URI, null);
        if (preferences.publishConfiguration(values)) {
            cleanOldBackground(old, null);
            refreshConfiguration();
        } else showConfigBackupMessage(R.string.panel_config_import_failed);
    }

    public void resetPanelConfiguration() {
        final long operation = IMAGE_OPERATION.incrementAndGet();
        pendingBackgroundImageUri = null;
        PanOSPPreferences preferences = new PanOSPPreferences(this);
        BackgroundImageStore store = new BackgroundImageStore(this);
        IMAGE_WORK.execute(() -> {
            try {
                java.util.Map<String, Object> values = preferences.prepareDefaultConfiguration();
                String prepared = (String) values.get(PanOSPConfigBackup.IMAGE_URI);
                runOnUiThread(() -> {
                    if (!isCurrentOperation(operation)) {
                        IMAGE_WORK.execute(() -> store.deleteManaged(prepared));
                        return;
                    }
                    String old = preferences.getPanelBackgroundImageUri();
                    if (preferences.publishConfiguration(values)) {
                        // Reset only the panel configuration; retain language and permission grants.
                        IMAGE_WORK.execute(() -> store.deleteManaged(old));
                        refreshConfiguration();
                    showConfigBackupMessage(R.string.panel_config_reset_success);
                    } else {
                        IMAGE_WORK.execute(() -> store.deleteManaged(prepared));
                        showConfigBackupMessage(R.string.panel_config_import_failed);
                    }
                });
            } catch (Exception | OutOfMemoryError error) {
                runOnUiThread(() -> {
                    if (isCurrentOperation(operation)) showConfigBackupMessage(R.string.panel_config_import_failed);
                });
            }
        });
    }

    private void releaseBackgroundImagePermission(String uriString) {
        try {
            getContentResolver().releasePersistableUriPermission(
                    Uri.parse(uriString),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {
            // Nothing to release, or the provider did not persist the grant.
        }
    }

    private boolean startActivitySafely(Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }
}
