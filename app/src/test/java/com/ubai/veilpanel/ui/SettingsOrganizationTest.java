package com.ubai.veilpanel.ui;
import android.app.Activity;import android.content.*;import android.view.*;import android.widget.*;
import com.ubai.veilpanel.MainActivity;import com.ubai.veilpanel.PanelShortcutActivity;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class SettingsOrganizationTest {
 @Test public void openingAndCollapsingSettingsPreservesExistingPanelValues(){
  Context context=RuntimeEnvironment.getApplication();android.content.SharedPreferences prefs=context.getSharedPreferences("veil_panel",0);prefs.edit().clear().putInt("blur",37).putInt("split",63).putInt("dismiss_handle_bottom",7).putBoolean("curtain_motion",true).commit();java.util.Map<String,?> before=prefs.getAll();
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   View root=controller.get().getWindow().getDecorView();String[] sections={"access","gestures","handle","wallpaper","notifications","typography","layout","motion","advanced"};
   for(String key:sections){SettingsSection section=root.findViewWithTag("section:"+key);assertNotNull(key,section);section.setExpanded(true,true);assertEquals(View.VISIBLE,section.content().getVisibility());section.setExpanded(false,true);assertEquals(View.GONE,section.content().getVisibility());}
   assertEquals(before,prefs.getAll());assertEquals(37,((SeekBar)root.findViewWithTag("value:blur")).getProgress());assertEquals(63,((SeekBar)root.findViewWithTag("value:split")).getProgress());
  }
 }
 @Test public void sectionsRememberTheirOwnStateWithoutWritingPanelSettings(){Context context=RuntimeEnvironment.getApplication();context.getSharedPreferences("panel_settings_ui",0).edit().clear().commit();SettingsSection first=new SettingsSection(context,"sample","Title","Summary",false);first.setExpanded(true,true);SettingsSection reopened=new SettingsSection(context,"sample","Title","Summary",false);assertTrue(reopened.isExpanded());assertFalse(context.getSharedPreferences("veil_panel",0).contains("sample"));}
 @Test public void legacyShortcutReturnsAnExplicitOpenAction(){try(var controller=Robolectric.buildActivity(PanelShortcutActivity.class,new Intent(Intent.ACTION_CREATE_SHORTCUT)).setup()){
  PanelShortcutActivity activity=controller.get();var shadow=Shadows.shadowOf(activity);assertEquals(Activity.RESULT_OK,shadow.getResultCode());Intent result=shadow.getResultIntent();Intent open=result.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT,Intent.class);assertNotNull(open);assertEquals(PanelShortcutActivity.ACTION_OPEN,open.getAction());assertEquals(PanelShortcutActivity.class.getName(),open.getComponent().getClassName());assertEquals(activity.getPackageName(),open.getComponent().getPackageName());assertTrue(activity.isFinishing());
 }}
 @Test public void legacyEditorIntentOnlyOpensNotificationSettings(){
  Intent intent=new Intent(RuntimeEnvironment.getApplication(),MainActivity.class)
   .putExtra(MainActivity.EXTRA_OPEN_PANEL_CONFIGURATION,true)
   .putStringArrayListExtra("com.ubai.veilpanel.TILE_PREVIEW_SPECS",new java.util.ArrayList<>(java.util.List.of("wifi","bluetooth")));
  try(var controller=Robolectric.buildActivity(MainActivity.class,intent).setup()){
   View root=controller.get().getWindow().getDecorView();
   assertNotNull(root.findViewWithTag("section:access"));
   assertNotNull(root.findViewWithTag("section:gestures"));
   assertFalse(hasLegacyEditor(root));
  }
 }
 public static class RecordingActivity extends MainActivity {
  int recreates;
  @Override public void recreate(){recreates++;}
 }
 @Test public void changingAlignmentKeepsSettingsOpenAndUpdatesItsSelection(){
  Context context=RuntimeEnvironment.getApplication();context.getSharedPreferences("veil_panel",0).edit().clear().commit();
  try(var controller=Robolectric.buildActivity(RecordingActivity.class).setup()){
   RecordingActivity activity=controller.get();View root=activity.getWindow().getDecorView();
   SettingsSection header=root.findViewWithTag("section:header");header.setExpanded(true,true);
   Button row=root.findViewWithTag("choice:header_clock_alignment");row.performClick();
   android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
   dialog.getListView().performItemClick(null,1,1);
   assertEquals(0,activity.recreates);assertTrue(header.isExpanded());assertEquals("Clock alignment: Centre",row.getText().toString());
   assertEquals("center",activity.getSharedPreferences("veil_panel",0).getString("header_clock_alignment",""));
   row.performClick();assertEquals(1,org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getListView().getCheckedItemPosition());
  }
 }
 private boolean hasLegacyEditor(View root){
  if(root instanceof PanelConfigurationView)return true;
  if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)if(hasLegacyEditor(((ViewGroup)root).getChildAt(i)))return true;
  return false;
 }
}
