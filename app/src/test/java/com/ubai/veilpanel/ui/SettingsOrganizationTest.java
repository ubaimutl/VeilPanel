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
   assertEquals(before,prefs.getAll());assertEquals(37,Math.round(((com.google.android.material.slider.Slider)root.findViewWithTag("value:blur")).getValue()));assertEquals(63,Math.round(((com.google.android.material.slider.Slider)root.findViewWithTag("value:split")).getValue()));
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
   androidx.appcompat.app.AlertDialog dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
   dialog.getListView().performItemClick(null,1,1);
   assertEquals(0,activity.recreates);assertTrue(header.isExpanded());assertEquals("Clock alignment: Centre",row.getText().toString());
   assertEquals("center",activity.getSharedPreferences("veil_panel",0).getString("header_clock_alignment",""));
   row.performClick();assertEquals(1,((androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog()).getListView().getCheckedItemPosition());
  }
 }
 @Test public void categoryNavigationAndSearchReachControlsWithoutChangingPanelPreferences(){
  Context context=RuntimeEnvironment.getApplication();var prefs=context.getSharedPreferences("veil_panel",0);prefs.edit().clear().putInt("blur",37).commit();var before=prefs.getAll();
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   View root=controller.get().getWindow().getDecorView();View setup=root.findViewWithTag("section:access"),clock=root.findViewWithTag("section:header"),themes=root.findViewWithTag("theme_presets");
   assertEquals(View.GONE,setup.getVisibility());assertEquals(View.VISIBLE,root.findViewWithTag("settings_categories").getVisibility());assertEquals(View.GONE,clock.getVisibility());
   root.findViewWithTag("category:header").performClick();assertEquals(View.VISIBLE,clock.getVisibility());assertEquals(View.GONE,setup.getVisibility());assertTrue(root.findViewWithTag("category:header").isSelected());
   root.findViewWithTag("settings_back").performClick();EditText search=root.findViewWithTag("settings_search");search.setText("Wallpaper blur");assertEquals(View.VISIBLE,root.findViewWithTag("section:wallpaper").getVisibility());assertEquals(View.GONE,clock.getVisibility());
   search.setText("no such setting xyz");assertEquals(View.VISIBLE,root.findViewWithTag("settings_empty").getVisibility());search.setText("");assertEquals(View.VISIBLE,root.findViewWithTag("settings_categories").getVisibility());
   root.findViewWithTag("category:themes").performClick();assertEquals(View.VISIBLE,themes.getVisibility());assertTrue(root.findViewWithTag("theme_carousel") instanceof HorizontalScrollView);
   assertEquals(before,prefs.getAll());
  }
 }
 @Test public void settingsAppearanceIsIndependentOfPanelPreferencesAndKeepsTheSelectedCategory(){
  Context context=RuntimeEnvironment.getApplication();var panel=context.getSharedPreferences("veil_panel",0);panel.edit().clear().putInt("blur",37).commit();var before=panel.getAll();
  context.getSharedPreferences("panel_settings_ui",0).edit().putString("appearance","light").commit();assertFalse(SettingsStyle.isDark(context));int light=SettingsStyle.panelBackground(context);
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   View root=controller.get().getWindow().getDecorView();root.findViewWithTag("category:appappearance").performClick();root.findViewWithTag("settings_appearance").performClick();
   androidx.appcompat.app.AlertDialog dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();assertEquals(1,dialog.getListView().getCheckedItemPosition());dialog.getListView().performItemClick(null,2,2);
   assertTrue(SettingsStyle.isDark(context));assertNotEquals(light,SettingsStyle.panelBackground(context));
   android.os.Bundle state=new android.os.Bundle();controller.saveInstanceState(state);assertEquals("appappearance",state.getString("settings_page"));assertEquals(before,panel.getAll());
  }finally{context.getSharedPreferences("panel_settings_ui",0).edit().remove("appearance").commit();}
 }
 @Test public void materialSlidersSupportNegativeValuesAndResetOnlyTheirOwnSetting(){
  Context context=RuntimeEnvironment.getApplication();var prefs=context.getSharedPreferences("veil_panel",0);prefs.edit().clear().putInt("blur",37).putInt("split",63).commit();
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   View root=controller.get().getWindow().getDecorView();com.google.android.material.slider.Slider spacing=root.findViewWithTag("value:header_clock_top_space");spacing.setValue(-31);assertEquals(-31,prefs.getInt("header_clock_top_space",0));root.findViewWithTag("reset:header_clock_top_space").performClick();assertEquals(4,prefs.getInt("header_clock_top_space",0));assertEquals(37,prefs.getInt("blur",0));assertEquals(63,prefs.getInt("split",0));
  }
 }
 @Test public void switchTitlesKeepFullWidthWhenSupportingTextIsGroupedInsideTheRow(){
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   View root=controller.get().getWindow().getDecorView();LinearLayout row=root.findViewWithTag("row:kyant_liquid_glass");LinearLayout labels=(LinearLayout)row.getChildAt(0);assertTrue(labels.getChildCount()>1);assertEquals(ViewGroup.LayoutParams.MATCH_PARENT,labels.getChildAt(0).getLayoutParams().width);assertEquals(0,((LinearLayout.LayoutParams)labels.getChildAt(0).getLayoutParams()).weight,0);
  }
 }
 @Test public void compactHomeHeaderHasNoBackOrAppearanceButtonAndKeepsContentBelowIt(){
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   View root=controller.get().getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2400,View.MeasureSpec.EXACTLY));root.layout(0,0,1080,2400);
   TextView title=root.findViewWithTag("settings_home_title");assertEquals("Veil Panel",title.getText().toString());assertNull(root.findViewWithTag("settings_home_subtitle"));assertEquals(View.GONE,root.findViewWithTag("settings_back").getVisibility());
   View toolbar=root.findViewWithTag("settings_toolbar"),scroll=root.findViewWithTag("settings_scroll");assertNull(toolbar.findViewWithTag("settings_appearance"));assertTrue(scroll.getTop()>=toolbar.getBottom());assertTrue(toolbar.getHeight()<=140*root.getResources().getDisplayMetrics().density);
   root.findViewWithTag("category:statusbar").performClick();assertEquals(View.VISIBLE,root.findViewWithTag("settings_back").getVisibility());root.findViewWithTag("settings_back").performClick();assertEquals(View.GONE,root.findViewWithTag("settings_back").getVisibility());
  }
 }
 @Test public void appearanceDefaultsToFollowSystemAndLivesInItsOwnPage(){
  RuntimeEnvironment.getApplication().getSharedPreferences("panel_settings_ui",0).edit().clear().commit();
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   View root=controller.get().getWindow().getDecorView();root.findViewWithTag("category:appappearance").performClick();Button mode=root.findViewWithTag("settings_appearance");assertEquals("Appearance: Follow system",mode.getText().toString());mode.performClick();
   androidx.appcompat.app.AlertDialog dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();assertEquals(0,dialog.getListView().getCheckedItemPosition());
  }
 }
 @Test public void statusBarControlsPreserveLegacyWallpaperChoiceAndSupportDarkIcons(){
  Context context=RuntimeEnvironment.getApplication();var prefs=context.getSharedPreferences("veil_panel",0);prefs.edit().clear().putBoolean("scroll_under_status_bar",false).putBoolean("show_status_bar",true).commit();var before=prefs.getAll();
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   View root=controller.get().getWindow().getDecorView();root.findViewWithTag("category:statusbar").performClick();assertEquals(before,prefs.getAll());
   android.widget.CompoundButton transparent=root.findViewWithTag("setting:scroll_under_status_bar");assertFalse(transparent.isChecked());assertFalse(PanelSystemBars.transparent(context));assertTrue(PanelSystemBars.visible(context));
   root.findViewWithTag("choice:status_bar_icons").performClick();androidx.appcompat.app.AlertDialog dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();dialog.getListView().performItemClick(null,1,1);
   assertEquals(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,PanelSystemBars.iconAppearance(context));assertFalse(PanelSystemBars.transparent(context));transparent.setChecked(true);assertTrue(PanelSystemBars.transparent(context));
  }
 }
 @Test public void replyOptionsAreReachableWithoutChangingThePanelCardStyle(){
  Context context=RuntimeEnvironment.getApplication();var prefs=context.getSharedPreferences("veil_panel",0);prefs.edit().clear().putInt("card_radius",13).commit();
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   View root=controller.get().getWindow().getDecorView();root.findViewWithTag("category:replies").performClick();assertEquals(View.VISIBLE,root.findViewWithTag("section:replies").getVisibility());
   root.findViewWithTag("choice:reply_send_style").performClick();androidx.appcompat.app.AlertDialog dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();dialog.getListView().performItemClick(null,1,1);
   assertEquals("filled",prefs.getString("reply_send_style",""));assertEquals(13,prefs.getInt("card_radius",0));assertNotNull(root.findViewWithTag("setting:reply_show_preview"));assertNotNull(root.findViewWithTag("value:reply_spacing"));
  }
 }
 @Test public void dedicatedSettingsFontDoesNotReplaceTheSelectedPanelFont(){
  Context context=RuntimeEnvironment.getApplication();var prefs=context.getSharedPreferences("veil_panel",0);prefs.edit().clear().putString("font_family","serif").commit();android.graphics.Typeface panelFont=PanelStyle.font(context);var before=prefs.getAll();
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();android.content.res.TypedArray attrs=activity.obtainStyledAttributes(new int[]{android.R.attr.fontFamily});assertEquals(com.ubai.veilpanel.R.font.roboto,attrs.getResourceId(0,0));attrs.recycle();assertEquals(panelFont,PanelStyle.font(activity));assertEquals(before,prefs.getAll());
  }
 }
 @Test public void systemBackHidesPreviewAndReturnsToSettingsIndex(){
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View root=activity.getWindow().getDecorView();root.findViewWithTag("category:gestures").performClick();root.findViewWithTag("pull_area_preview").performClick();activity.getOnBackPressedDispatcher().onBackPressed();
   PullAreaPreview preview=org.robolectric.util.ReflectionHelpers.getField(activity,"pullAreaPreview");assertFalse(preview.isShown());assertFalse(activity.isFinishing());assertEquals(View.VISIBLE,root.findViewWithTag("settings_categories").getVisibility());
  }
 }
 @Test public void pullAreaPreviewTracksBothSlidersAndNeverSurvivesNavigationOrPause(){
  Context context=RuntimeEnvironment.getApplication();var prefs=context.getSharedPreferences("veil_panel",0);prefs.edit().clear().commit();
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
   MainActivity activity=controller.get();View root=activity.getWindow().getDecorView();root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2400,View.MeasureSpec.EXACTLY));root.layout(0,0,1080,2400);
   root.findViewWithTag("category:gestures").performClick();root.findViewWithTag("pull_area_preview").performClick();
   PullAreaPreview preview=org.robolectric.util.ReflectionHelpers.getField(activity,"pullAreaPreview");assertTrue(preview.isShown());assertFalse(prefs.contains("pull_area_height"));
   ((com.google.android.material.slider.Slider)root.findViewWithTag("value:split")).setValue(70);
   ((com.google.android.material.slider.Slider)root.findViewWithTag("value:pull_area_height")).setValue(64);
   assertEquals(756,preview.getBounds().width());assertEquals(PanelPullArea.height(activity),preview.getBounds().height());assertTrue(preview.isShown());
   root.findViewWithTag("settings_back").performClick();assertFalse(preview.isShown());root.findViewWithTag("category:gestures").performClick();root.findViewWithTag("pull_area_preview").performClick();assertTrue(preview.isShown());controller.pause();assertFalse(preview.isShown());
  }
 }
 private boolean hasLegacyEditor(View root){
  if(root instanceof PanelConfigurationView)return true;
  if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)if(hasLegacyEditor(((ViewGroup)root).getChildAt(i)))return true;
  return false;
 }
}
