package com.ubai.veilpanel;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.ubai.veilpanel.config.PanelSettingsArchive;
import java.io.*;
import java.util.concurrent.Executors;

/** User-initiated portable backups through the Android document picker. */
public final class SettingsTransferActivity extends androidx.appcompat.app.AppCompatActivity {
    private static final int EXPORT=41,IMPORT=42;
    private boolean busy;
    private Button exportButton,importButton;
    @Override public void onCreate(Bundle state){
        setTheme(com.ubai.veilpanel.ui.SettingsStyle.isDark(this)?R.style.VeilSettingsDark:R.style.VeilSettingsLight);
        super.onCreate(state);
        getWindow().setStatusBarColor(com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this));getWindow().setNavigationBarColor(com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this));
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setBackgroundColor(com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this));int padding=(int)(24*getResources().getDisplayMetrics().density);body.setPadding(padding,padding,padding,padding);
        body.setOnApplyWindowInsetsListener((v,insets)->{if(v.getWindowInsetsController()!=null)v.getWindowInsetsController().setSystemBarsAppearance(com.ubai.veilpanel.ui.SettingsStyle.isDark(this)?0:WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(padding+bars.left,padding+bars.top,padding+bars.right,padding+bars.bottom);return insets;});
        TextView title=new TextView(this);title.setText("Backup & transfer");title.setTextSize(26);title.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.primaryText(this));body.addView(title);
        TextView detail=new TextView(this);detail.setText("Save your layout, colours, gestures, glass settings, selected wallpaper and custom font in one ZIP. Import this file in another installation of Veil Panel.\n\nAndroid permissions and launcher shortcuts must be set up again. Notifications are never included. Keep the backup private: it contains your selected images and preferences.");detail.setTextSize(16);detail.setTextColor(com.ubai.veilpanel.ui.SettingsStyle.secondaryText(this));detail.setPadding(0,padding,0,padding);body.addView(detail);
        exportButton=new Button(this);exportButton.setText("Export complete settings");exportButton.setOnClickListener(v->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip").putExtra(Intent.EXTRA_TITLE,"Veil-Panel-settings.zip"),EXPORT));body.addView(exportButton);
        importButton=new Button(this);importButton.setText("Import complete settings");importButton.setOnClickListener(v->new com.ubai.veilpanel.ui.SettingsDialogBuilder(this).setTitle("Restore settings?").setMessage("This replaces this installation’s saved panel settings, wallpaper and custom font with the backup. Your other installation stays unchanged. Close the panel before importing.").setNegativeButton(android.R.string.cancel,null).setPositiveButton("Choose backup",(d,w)->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/zip"),IMPORT)).show());body.addView(importButton);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(com.ubai.veilpanel.ui.SettingsStyle.panelBackground(this));scroll.setVerticalScrollBarEnabled(false);scroll.addView(body);com.ubai.veilpanel.ui.SettingsTypography.apply(scroll);setContentView(scroll);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null||(request!=EXPORT&&request!=IMPORT)||busy)return;
        android.net.Uri uri=data.getData();busy=true;exportButton.setEnabled(false);importButton.setEnabled(false);
        Context application=getApplicationContext();var executor=Executors.newSingleThreadExecutor();
        executor.execute(()->{
            String message;
            try {
                if(request==EXPORT){try(OutputStream output=application.getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new IOException("Unavailable document");PanelSettingsArchive.write(application,output);}message="Backup saved. Import this ZIP in the other installation.";}
                else{application.sendBroadcast(new Intent(application.getPackageName()+".CLOSE_PANEL").setPackage(application.getPackageName()));try(InputStream input=application.getContentResolver().openInputStream(uri)){if(input==null)throw new IOException("Unavailable document");PanelSettingsArchive.restore(application,input);}message="Settings restored. Reopen Veil Panel settings to see the imported values.";}
            }catch(Exception|OutOfMemoryError error){message=request==EXPORT?"Could not save the backup. Choose a writable location with enough space.":"Could not restore this backup. Choose a complete Veil Panel settings ZIP. Your saved settings were retained.";}
            final String text=message;runOnUiThread(()->{if(isFinishing()||isDestroyed())return;busy=false;exportButton.setEnabled(true);importButton.setEnabled(true);new com.ubai.veilpanel.ui.SettingsDialogBuilder(this).setMessage(text).setPositiveButton(android.R.string.ok,null).show();});executor.shutdown();
        });
    }
}
