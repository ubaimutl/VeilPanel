package com.ubai.veilpanel;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.LocaleManager;
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
public class MainActivity extends androidx.appcompat.app.AppCompatActivity {

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
    private com.ubai.veilpanel.ui.SettingsNavigator settingsNavigator;
    private String selectedSettingsPage="home";
    private String pendingBackgroundImageUri;
    private static final java.util.concurrent.ExecutorService IMAGE_WORK =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final java.util.concurrent.atomic.AtomicLong IMAGE_OPERATION =
            new java.util.concurrent.atomic.AtomicLong();

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString("pending_background", pendingBackgroundImageUri);
        state.putString("settings_page",selectedSettingsPage);
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
        setTheme(com.ubai.veilpanel.ui.SettingsStyle.isDark(this)?R.style.VeilSettingsDark:R.style.VeilSettingsLight);
        super.onCreate(savedInstanceState);
        getWindow().setNavigationBarContrastEnforced(false);
        getWindow().setStatusBarColor(com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this));
        getWindow().setNavigationBarColor(com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this));
        if(savedInstanceState!=null)selectedSettingsPage=savedInstanceState.getString("settings_page","home");

        if (getIntent().getBooleanExtra("com.ubai.veilpanel.INTERNAL_BOOT_REFRESH", false)) {
            finish();
            return;
        }

        if (savedInstanceState != null) pendingBackgroundImageUri = savedInstanceState.getString("pending_background");
        setContentView(buildContent());
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->{if(settingsNavigator==null||!settingsNavigator.back())finish();});
        if(savedInstanceState==null&&getIntent().getBooleanExtra("open_transfer_settings",false))startActivityForResult(new Intent(this,SettingsTransferActivity.class),REQUEST_TRANSFER_SETTINGS);
        if(savedInstanceState==null&&getIntent().getBooleanExtra("open_liquid_settings",false))settingsNavigator.select("liquid");
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
        windowRoot.setOrientation(LinearLayout.VERTICAL);windowRoot.setFocusableInTouchMode(true);
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
        navigationBarScrim.setBackgroundColor(com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setTag("settings_scroll");scrollView.setFillViewport(true);scrollView.setVerticalScrollBarEnabled(false);scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        final int horizontalPadding = dp(16);
        final int topPadding = dp(8);
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
            if (controller != null) controller.setSystemBarsAppearance(com.ubai.veilpanel.ui.SettingsStyle.isDark(this)?0:android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
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


        LinearLayout toolbar=new LinearLayout(this);toolbar.setTag("settings_toolbar");toolbar.setBackgroundColor(com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this));toolbar.setOrientation(LinearLayout.VERTICAL);toolbar.setPadding(dp(16),dp(8),dp(16),dp(8));
        LinearLayout headingRow=new LinearLayout(this);headingRow.setGravity(Gravity.CENTER_VERTICAL);
        Button back=new Button(this);back.setTag("settings_back");back.setText("←");back.setTextSize(28);back.setAllCaps(false);back.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.primaryText(this));back.setContentDescription("Back to settings");back.setPadding(0,0,0,0);back.setIncludeFontPadding(false);back.setGravity(Gravity.CENTER);back.setStateListAnimator(null);back.setElevation(0);back.setMinWidth(0);back.setMinimumWidth(0);back.setBackground(com.ubai.veilpanel.ui.SettingsStyle.touch(this,com.ubai.veilpanel.ui.PanOSPTheme.blend(com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this),com.ubai.veilpanel.ui.SettingsStyle.primaryText(this),.06f),28));headingRow.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));back.setOnClickListener(v->{if(settingsNavigator!=null&&!settingsNavigator.back())finish();});
        View spacer=new View(this);headingRow.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        Button mode=new Button(this);mode.setTag("settings_appearance");mode.setAllCaps(false);mode.setTextSize(16);mode.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.primaryText(this));mode.setPadding(dp(16),dp(8),dp(16),dp(8));mode.setMinHeight(dp(48));mode.setMinimumHeight(dp(48));mode.setMinWidth(0);mode.setMinimumWidth(0);mode.setIncludeFontPadding(false);mode.setSingleLine(true);mode.setGravity(Gravity.CENTER);mode.setStateListAnimator(null);mode.setElevation(0);mode.setText("Appearance");mode.setContentDescription("Settings appearance");mode.setBackground(com.ubai.veilpanel.ui.SettingsStyle.touch(this,Color.TRANSPARENT,24));mode.setOnClickListener(v->showSettingsAppearance());headingRow.addView(mode,new LinearLayout.LayoutParams(-2,-2));toolbar.addView(headingRow);
        android.widget.EditText search=new android.widget.EditText(this);search.setTag("settings_search");search.setSingleLine(true);search.setTextSize(16);search.setHint("Search settings");search.setContentDescription("Search settings");search.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.primaryText(this));search.setHintTextColor(com.ubai.veilpanel.ui.SettingsStyle.secondaryText(this));search.setPadding(dp(16),0,dp(16),0);search.setBackground(com.ubai.veilpanel.ui.SettingsStyle.rounded(this,com.ubai.veilpanel.ui.PanOSPTheme.blend(com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this),com.ubai.veilpanel.ui.SettingsStyle.primaryText(this),.08f),16));search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);toolbar.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));windowRoot.addView(toolbar,1);

        addSetupContent(root);

        windowRoot.setBackgroundColor(com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this));
        scrollView.addView(root);
        settingsNavigator=new com.ubai.veilpanel.ui.SettingsNavigator(this,toolbar,root,scrollView,search,key->selectedSettingsPage=key);
        settingsNavigator.select(selectedSettingsPage);


        com.ubai.veilpanel.ui.SettingsTypography.apply(windowRoot);
        windowRoot.post(windowRoot::requestApplyInsets);

        return windowRoot;
    }

    @Override public void onBackPressed(){if(settingsNavigator!=null&&settingsNavigator.back())return;super.onBackPressed();}

    private void showSettingsAppearance(){
        String[] values={"system","light","dark"};String current=getSharedPreferences("panel_settings_ui",0).getString("appearance","system");int checked=java.util.Arrays.asList(values).indexOf(current);
        new com.ubai.veilpanel.ui.SettingsDialogBuilder(this).setTitle("Settings appearance").setSingleChoiceItems(new String[]{"Follow system","Light","Dark"},Math.max(0,checked),(dialog,which)->{
            getSharedPreferences("panel_settings_ui",0).edit().putString("appearance",values[which]).apply();dialog.dismiss();recreate();
        }).setNegativeButton(android.R.string.cancel,null).show();
    }

    public void refreshGlobalAppearance() {
        if(appearanceRoot!=null)appearanceRoot.setBackgroundColor(com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this));
    }

    private LinearLayout section(LinearLayout root,String key,String title,String summary,boolean initial) {
        com.ubai.veilpanel.ui.SettingsSection section=new com.ubai.veilpanel.ui.SettingsSection(this,key,title,summary,initial);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(12);root.addView(section,params);return section.content();
    }
    private void help(LinearLayout root,String text){
        TextView note=new TextView(this);note.setText(text);note.setTextSize(14);note.setLineSpacing(dp(3),1);note.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.secondaryText(this));note.setPadding(dp(4),dp(8),dp(4),dp(16));
        View last=root.getChildCount()==0?null:root.getChildAt(root.getChildCount()-1);String tag=last==null?"":String.valueOf(last.getTag());
        if(last instanceof LinearLayout&&tag.startsWith("row:")){
            LinearLayout row=(LinearLayout)last;View first=row.getChildAt(0);LinearLayout labels;
            if(first instanceof TextView){row.removeViewAt(0);labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);first.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));labels.addView(first);row.addView(labels,0,new LinearLayout.LayoutParams(0,-2,1));}else labels=(LinearLayout)first;
            note.setPadding(0,dp(4),0,0);labels.addView(note);row.setContentDescription(((TextView)labels.getChildAt(0)).getText()+". "+text);return;
        }
        if(last instanceof LinearLayout&&tag.startsWith("setting:")){
            LinearLayout row=(LinearLayout)last;note.setPadding(0,dp(4),dp(8),dp(8));row.addView(note,Math.max(1,row.getChildCount()-1));return;
        }
        root.addView(note);
    }
    private Button action(LinearLayout root,String title,Runnable run){
        Button button=new Button(this);button.setText(title);styleAction(button);button.setOnClickListener(v->run.run());LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(8);root.addView(button,params);return button;
    }
    private void styleAction(Button button){
        button.setAllCaps(false);button.setTextSize(16);button.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.primaryText(this));button.setMinHeight(dp(64));button.setMinimumHeight(dp(64));button.setMinWidth(0);button.setMinimumWidth(0);button.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);button.setPadding(dp(20),dp(16),dp(20),dp(16));button.setStateListAnimator(null);
        button.setBackground(com.ubai.veilpanel.ui.SettingsStyle.touch(this,Color.TRANSPARENT,4));
    }
    private void addOption(LinearLayout root,String key){for(com.ubai.veilpanel.config.PanelAppearance.Option option:com.ubai.veilpanel.config.PanelAppearance.OPTIONS)if(option.key.equals(key)){addGlassSlider(root,option.label,option.min,option.max,option.key,option.initial);return;}}
    private void addThemePresets(LinearLayout root){
        LinearLayout box=new LinearLayout(this);box.setTag("theme_presets");box.setOrientation(LinearLayout.VERTICAL);box.setPadding(0,dp(8),0,dp(16));
        int accent=com.ubai.veilpanel.ui.SettingsStyle.accent(this),background=com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this);

        TextView heading=new TextView(this);heading.setText("Presets");heading.setTextSize(34);heading.setPadding(dp(4),dp(20),0,dp(28));heading.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.primaryText(this));heading.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL),600,false));box.addView(heading);
        help(box,"Swipe to explore the presets. Tap a card to apply its look; Undo restores your previous appearance.");
        android.widget.HorizontalScrollView carousel=new android.widget.HorizontalScrollView(this);carousel.setTag("theme_carousel");carousel.setHorizontalScrollBarEnabled(false);carousel.setClipToPadding(true);carousel.setHorizontalFadingEdgeEnabled(true);carousel.setFadingEdgeLength(dp(20));
        LinearLayout choices=new LinearLayout(this);choices.setPadding(0,dp(4),dp(12),dp(8));carousel.addView(choices);box.addView(carousel,new LinearLayout.LayoutParams(-1,-2));
        for(int i=0;i<com.ubai.veilpanel.config.PanelThemePresets.IDS.length;i++){
            final int index=i;LinearLayout choice=new LinearLayout(this);choice.setOrientation(LinearLayout.VERTICAL);choice.setPadding(dp(16),dp(10),dp(16),dp(16));choice.setTag("theme:"+com.ubai.veilpanel.config.PanelThemePresets.IDS[index]);choice.setClickable(true);choice.setFocusable(true);choice.setBackground(com.ubai.veilpanel.ui.SettingsStyle.touch(this,com.ubai.veilpanel.ui.PanOSPTheme.blend(background,accent,.08f),18));
            TextView name=new TextView(this);name.setText(com.ubai.veilpanel.config.PanelThemePresets.NAMES[index]);name.setTextSize(16);name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);name.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.primaryText(this));name.setPadding(dp(4),dp(12),dp(4),dp(4));choice.addView(name);
            TextView description=new TextView(this);description.setText(com.ubai.veilpanel.config.PanelThemePresets.DESCRIPTIONS[index]);description.setTextSize(14);description.setMinHeight(dp(44));description.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.secondaryText(this));description.setPadding(dp(4),0,dp(4),0);choice.addView(description);
            java.util.Map<String,Object> preset=com.ubai.veilpanel.config.PanelThemePresets.values(com.ubai.veilpanel.config.PanelThemePresets.IDS[index]);
            TextView details=new TextView(this);details.setText("Card opacity  "+preset.get("glass_opacity")+"%\nWallpaper blur  "+preset.get("blur")+"\nClock size  "+preset.get("clock_size"));details.setTextSize(14);details.setLineSpacing(dp(6),1);details.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.secondaryText(this));details.setPadding(dp(4),dp(16),dp(4),dp(12));choice.addView(details);
            TextView apply=new TextView(this);apply.setText("Apply theme  →");apply.setTextSize(14);apply.setTextColor(accent);apply.setPadding(dp(4),dp(8),dp(4),0);choice.addView(apply);
            choice.setContentDescription("Apply "+com.ubai.veilpanel.config.PanelThemePresets.NAMES[index]+" theme. "+com.ubai.veilpanel.config.PanelThemePresets.DESCRIPTIONS[index]+". "+details.getText());
            LinearLayout.LayoutParams placement=new LinearLayout.LayoutParams(dp(220),-1);placement.setMarginEnd(dp(12));choices.addView(choice,placement);
            choice.setOnClickListener(v->{try{
                com.ubai.veilpanel.config.PanelThemePresets.apply(this,com.ubai.veilpanel.config.PanelThemePresets.IDS[index]);
                android.widget.Toast.makeText(this,com.ubai.veilpanel.config.PanelThemePresets.NAMES[index]+" theme applied",android.widget.Toast.LENGTH_SHORT).show();recreate();
            }catch(IllegalStateException unavailable){android.widget.Toast.makeText(this,"Could not save your previous look. Theme kept unchanged.",android.widget.Toast.LENGTH_LONG).show();}});
        }
        help(box,"Changes apply the next time the panel opens. Return to settings to fine-tune each look. Tap a slider value to enter an exact number.\n\nGlass uses your saved panel wallpaper; without one, cards use a frosted fallback. Gestures, recovery options and access stay as configured.");
        if(com.ubai.veilpanel.config.PanelThemePresets.canUndo(this)){
            Button undo=action(box,"Undo last theme",()->{if(com.ubai.veilpanel.config.PanelThemePresets.undo(this))recreate();else android.widget.Toast.makeText(this,"Previous look could not be restored",android.widget.Toast.LENGTH_SHORT).show();});undo.setTag("theme:undo");
        }
        LinearLayout.LayoutParams placement=new LinearLayout.LayoutParams(-1,-2);placement.topMargin=dp(24);placement.bottomMargin=dp(4);root.addView(box,placement);
    }

    private void addSetupContent(LinearLayout root) {
        boolean ready=isAccessibilityServiceEnabled()&&isNotificationListenerEnabled()&&Settings.canDrawOverlays(this);
        LinearLayout access=section(root,"access","Setup & access",ready?"Required access is enabled":"Enable the panel and its wallpaper",!ready);
        accessibilityButton=addSetupButton(access,R.string.setup_accessibility,v->openAccessibilitySettings());styleAction(accessibilityButton);
        notificationButton=addSetupButton(access,R.string.setup_notification_access,v->openNotificationListenerSettings());styleAction(notificationButton);
        action(access,Settings.canDrawOverlays(this)?"Display over apps · Enabled":"Enable display over apps",()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        help(access,"Accessibility handles gestures. Notification access supplies the cards. Display-over-apps access and a saved wallpaper let apps keep playing underneath. The lock screen stays with Android.");
        action(access,"Pause panel",()->sendBroadcast(new Intent(getPackageName()+".PAUSE").setPackage(getPackageName())));

        action(access,"Open notification panel",()->{if(!com.ubai.veilpanel.accessibility.PanOSPAccessibilityServiceCore.requestPanelOpen())android.widget.Toast.makeText(this,R.string.shortcut_enable_service,android.widget.Toast.LENGTH_LONG).show();});
        addThemePresets(root);


        LinearLayout gestures=section(root,"gestures","Gestures & shortcuts","Pull-down split and launcher actions",false);
        help(gestures,"Pull from the left for notifications and from the right for your original quick settings.");
        addGlassSlider(gestures,"Left notification area (%)",20,80,"split",50);
        addPanelSwitch(gestures,"Two swipes in fullscreen apps","fullscreen_two_swipe",true);
        help(gestures,"When both status and navigation bars are hidden, the first pull reveals Android's bars and a fresh second pull opens Veil. Apps that hide only the status bar open Veil with one pull.");
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
        help(wallpaper,"Bright wallpapers receive extra shading so notification text stays readable. Glass tint and card opacity are minimum values; contrast protection can increase them.");
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
        action(liquid,"Source & license",()->{try{String notice;try(java.io.InputStream input=getAssets().open("third_party/android-liquid-glass/NOTICE.txt")){notice=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}String license;try(java.io.InputStream input=getAssets().open("third_party/android-liquid-glass/LICENSE.txt")){license=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}new com.ubai.veilpanel.ui.SettingsDialogBuilder(this).setTitle("AndroidLiquidGlass · Kyant").setMessage(notice+"\n"+license).setPositiveButton(android.R.string.ok,null).setNeutralButton("Project",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/Kyant0/AndroidLiquidGlass")))).show();}catch(java.io.IOException unavailable){android.widget.Toast.makeText(this,"License file unavailable",android.widget.Toast.LENGTH_SHORT).show();}});

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
        action(type,"Font family",()->{String[] labels={"System","Josefin Sans","Serif","Saved custom font"},values={"system","josefin","serif","custom"};new com.ubai.veilpanel.ui.SettingsDialogBuilder(this).setTitle("Font family").setItems(labels,(dialog,which)->{getSharedPreferences("veil_panel",0).edit().putString("font_family",values[which]).apply();recreate();}).show();});
        action(type,"Choose custom font (TTF / OTF)",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),REQUEST_CUSTOM_FONT));
        help(type,"Choose a TTF or OTF file up to 20 MB. A private copy is saved for offline use. Font family lets you switch back at any time.");
        action(type,"Remove saved custom font",()->new com.ubai.veilpanel.ui.SettingsDialogBuilder(this).setTitle("Remove custom font?").setMessage("The panel will return to the system font. Your original file stays unchanged.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{com.ubai.veilpanel.ui.CustomFont.clear(this);recreate();}).show());
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
        action(advanced,"Reset layout sizes",()->new com.ubai.veilpanel.ui.SettingsDialogBuilder(this).setTitle("Reset layout sizes?").setMessage("Reset card, text and icon sizes. Keep your wallpaper, colours and gestures.").setNegativeButton(android.R.string.cancel,null).setPositiveButton("Reset",(d,w)->{com.ubai.veilpanel.config.PanelAppearance.reset(this);recreate();}).show());
        action(advanced,"Remove saved wallpaper",()->new com.ubai.veilpanel.ui.SettingsDialogBuilder(this).setTitle("Remove panel wallpaper?").setMessage("Delete the private saved copy. The panel will use its legacy backdrop until you choose an image again.").setNegativeButton(android.R.string.cancel,null).setPositiveButton("Remove",(d,w)->IMAGE_WORK.execute(()->{com.ubai.veilpanel.image.PowerSaverBackdrop.clear(getApplicationContext());runOnUiThread(()->{if(!isDestroyed()&&!isFinishing())recreate();});})).show());
        help(advanced,"Veil Panel · 1.0.0\nBased on PanOSP · GPL-3.0-or-later\nNo Internet permission. Wallpaper and notification data stay on your device.");
    }

    private void addHeaderChoice(LinearLayout parent,String title,String key,String[] labels,String[] values,String initial){
        String current=getSharedPreferences("veil_panel",0).getString(key,initial);int selected=0;
        for(int i=0;i<values.length;i++)if(values[i].equals(current))selected=i;
        Button row=action(parent,title+": "+labels[selected],()->{});row.setTag("choice:"+key);
        row.setOnClickListener(v->{
            String value=getSharedPreferences("veil_panel",0).getString(key,initial);int checked=0;
            for(int i=0;i<values.length;i++)if(values[i].equals(value))checked=i;
            new com.ubai.veilpanel.ui.SettingsDialogBuilder(this).setTitle(title).setSingleChoiceItems(labels,checked,(dialog,which)->{
                getSharedPreferences("veil_panel",0).edit().putString(key,values[which]).apply();
                row.setText(title+": "+labels[which]);dialog.dismiss();
            }).setNegativeButton(android.R.string.cancel,null).show();
        });
    }

    private void addPanelSwitch(LinearLayout root,String title,String key,boolean initial){
        LinearLayout row=new LinearLayout(this);row.setTag("row:"+key);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(20),dp(16),dp(20),dp(16));row.setMinimumHeight(dp(72));row.setFocusable(true);
        TextView label=new TextView(this);label.setText(title);label.setTextSize(16);label.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.primaryText(this));row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        com.google.android.material.materialswitch.MaterialSwitch toggle=new com.google.android.material.materialswitch.MaterialSwitch(this);toggle.setTag("setting:"+key);toggle.setContentDescription(title);toggle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);toggle.setFocusable(false);toggle.setChecked(getSharedPreferences("veil_panel",0).getBoolean(key,initial));LinearLayout.LayoutParams switchParams=new LinearLayout.LayoutParams(-2,dp(48));switchParams.setMarginStart(dp(16));row.addView(toggle,switchParams);
        row.setContentDescription(title);row.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(android.widget.Switch.class.getName());info.setCheckable(true);info.setChecked(toggle.isChecked());}});
        row.setOnClickListener(v->toggle.setChecked(!toggle.isChecked()));toggle.setOnCheckedChangeListener((v,value)->{getSharedPreferences("veil_panel",0).edit().putBoolean(key,value).apply();if("dynamic".equals(key))recreate();});root.addView(row,new LinearLayout.LayoutParams(-1,-2));
    }
    private void addGlassSlider(LinearLayout root,String title,int min,int max,String key,int initial) {
        LinearLayout setting=new LinearLayout(this);setting.setTag("setting:"+key);setting.setOrientation(LinearLayout.VERTICAL);setting.setPadding(dp(20),dp(20),dp(12),dp(16));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView label=new TextView(this);label.setText(title);label.setTextSize(16);label.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.primaryText(this));row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        TextView value=new TextView(this);value.setTextSize(16);value.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);value.setPadding(dp(12),0,dp(8),0);value.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.primaryText(this));value.setGravity(Gravity.CENTER);value.setMinWidth(dp(48));value.setMinHeight(dp(48));value.setClickable(true);value.setFocusable(true);row.addView(value,new LinearLayout.LayoutParams(-2,-2));setting.addView(row);
        String explanation=com.ubai.veilpanel.ui.SettingDescriptions.forKey(key);if(explanation!=null){TextView detail=new TextView(this);detail.setText(explanation);detail.setTextSize(14);detail.setLineSpacing(dp(3),1);detail.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.secondaryText(this));detail.setPadding(0,0,dp(8),dp(8));setting.addView(detail);}
        LinearLayout control=new LinearLayout(this);control.setGravity(Gravity.CENTER_VERTICAL);
        com.google.android.material.slider.Slider slider=new com.google.android.material.slider.Slider(this);slider.setTag("value:"+key);slider.setValueFrom(min);slider.setValueTo(max);slider.setStepSize(1);slider.setValue(Math.max(min,Math.min(max,getSharedPreferences("veil_panel",0).getInt(key,initial))));slider.setContentDescription(title);slider.setTrackHeight(dp(16));slider.setThumbWidth(dp(4));slider.setThumbHeight(dp(44));slider.setThumbTrackGapSize(dp(6));slider.setTrackStopIndicatorSize(dp(4));slider.setTrackInsideCornerSize(dp(2));slider.setTickVisible(false);slider.setLabelBehavior(com.google.android.material.slider.LabelFormatter.LABEL_GONE);control.addView(slider,new LinearLayout.LayoutParams(0,dp(56),1));
        Button reset=new Button(this);reset.setTag("reset:"+key);reset.setText("↺");reset.setTextSize(26);reset.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.secondaryText(this));reset.setContentDescription("Reset "+title+" to "+initial);reset.setMinWidth(0);reset.setMinimumWidth(0);reset.setPadding(0,0,0,0);reset.setBackground(com.ubai.veilpanel.ui.SettingsStyle.touch(this,Color.TRANSPARENT,24));control.addView(reset,new LinearLayout.LayoutParams(dp(48),dp(48)));reset.setOnClickListener(v->slider.setValue(initial));setting.addView(control);root.addView(setting,new LinearLayout.LayoutParams(-1,-2));
        Runnable refresh=()->{int next=Math.round(slider.getValue());value.setText(next+(title.contains("%")?"%":""));value.setContentDescription(title+", "+next+". Tap to enter a value.");reset.setEnabled(next!=initial);reset.setAlpha(next==initial?.35f:1);};refresh.run();
        value.setOnClickListener(v->{android.widget.EditText input=new android.widget.EditText(this);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|(min<0?android.text.InputType.TYPE_NUMBER_FLAG_SIGNED:0));input.setText(String.valueOf(Math.round(slider.getValue())));input.selectAll();new com.ubai.veilpanel.ui.SettingsDialogBuilder(this).setTitle(title+" ("+min+"–"+max+")").setView(input).setNegativeButton(android.R.string.cancel,null).setPositiveButton("Apply",(dialog,which)->{try{slider.setValue(Math.max(min,Math.min(max,Integer.parseInt(input.getText().toString().trim()))));}catch(NumberFormatException ignored){}}).show();});
        slider.addOnChangeListener((bar,next,user)->{getSharedPreferences("veil_panel",0).edit().putInt(key,Math.round(next)).apply();refresh.run();});
    }

    private void addLanguageControl(LinearLayout root) {
        TextView heading = new TextView(this);
        heading.setText(R.string.app_language);
        heading.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.secondaryText(this));
        heading.setTextSize(16);
        heading.setPadding(0, dp(24), 0, dp(4));
        root.addView(heading);

        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(getString(R.string.app_language_value, getCurrentLanguageLabel()));
        button.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.primaryText(this));
        button.setMinHeight(dp(52));
        GradientDrawable languageBackground = new GradientDrawable();
        languageBackground.setColor(Color.argb(20, 255, 255, 255));
        languageBackground.setCornerRadius(dp(14));
        languageBackground.setStroke(dp(1), Color.argb(40, 255, 255, 255));
        styleAction(button);
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

        new com.ubai.veilpanel.ui.SettingsDialogBuilder(this)
                .setTitle(R.string.app_language)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    LocaleList selected = tags[which].isEmpty()
                            ? LocaleList.getEmptyLocaleList()
                            : LocaleList.forLanguageTags(tags[which]);
                    manager.setApplicationLocales(selected);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private Button addSetupButton(
            LinearLayout root,
            int labelRes,
            android.view.View.OnClickListener listener) {

        Button button = new Button(this);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        button.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.primaryText(this));
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
        new com.ubai.veilpanel.ui.SettingsDialogBuilder(this)
                .setMessage(
                        R.string.panel_config_background_unavailable
                )
                .setPositiveButton(
                        R.string.panel_config_background_reselect,
                        (dialog, which) -> selectPanelBackgroundImage()
                )
                .setNegativeButton(R.string.panel_config_background_later, null).show();
    }

    private void showConfigBackupMessage(int messageRes) {
        new com.ubai.veilpanel.ui.SettingsDialogBuilder(this)
                .setMessage(messageRes)
                .setPositiveButton(android.R.string.ok, null).show();
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
