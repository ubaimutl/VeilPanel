package com.ubai.veilpanel.config;
import android.content.Context;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class PanelThemePresetsTest {
 private Context context;
 @Before public void setup(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("veil_panel",0).edit().clear().commit();context.getSharedPreferences("panel_theme_history",0).edit().clear().commit();}
 @Test public void allThemesAreVisualOnlyAndUndoRestoresTheExactPreviousLook(){
  for(String id:PanelThemePresets.IDS){
   var prefs=context.getSharedPreferences("veil_panel",0);
   prefs.edit().clear().putInt("split",73).putBoolean("fullscreen_two_swipe",false).putBoolean("native_edge_recovery",false).putBoolean("recovery_status_cover",false).putBoolean("show_status_bar",false).putInt("dismiss_handle_bottom",9).putString("font_family","custom").putInt("header_clock_weight",900).putInt("blur",47).commit();
   var before=prefs.getAll();PanelThemePresets.apply(context,id);
   assertEquals(73,prefs.getInt("split",0));assertFalse(prefs.getBoolean("fullscreen_two_swipe",true));assertFalse(prefs.getBoolean("native_edge_recovery",true));assertFalse(prefs.getBoolean("recovery_status_cover",true));assertFalse(prefs.getBoolean("show_status_bar",true));assertEquals(9,prefs.getInt("dismiss_handle_bottom",0));
   assertTrue(PanelThemePresets.canUndo(context));assertTrue(PanelThemePresets.undo(context));assertEquals(before,prefs.getAll());assertFalse(PanelThemePresets.canUndo(context));
  }
 }
 @Test public void demoAppliesGlassAppearanceAndPreservesInteractionSettings(){
  var values=PanelThemePresets.values("demo");assertEquals(86,values.get("clock_size"));assertEquals("liquid",values.get("header_clock_style"));assertEquals(900,values.get("header_clock_weight"));assertEquals(-24,values.get("header_clock_top_space"));assertEquals(7,values.get("card_gap"));assertEquals(29,values.get("control_surface_size"));assertEquals(67,values.get("description_brightness"));assertEquals(true,values.get("kyant_liquid_glass"));
  var prefs=context.getSharedPreferences("veil_panel",0);prefs.edit().putInt("split",68).putBoolean("scroll_under_status_bar",false).putBoolean("native_edge_recovery",false).commit();var before=prefs.getAll();PanelThemePresets.apply(context,"demo");assertEquals(68,prefs.getInt("split",0));assertFalse(prefs.getBoolean("scroll_under_status_bar",true));assertFalse(prefs.getBoolean("native_edge_recovery",true));assertTrue(PanelThemePresets.undo(context));assertEquals(before,prefs.getAll());
 }
 @Test public void allPresetSizesAreWithinSupportedRanges(){
  for(String id:PanelThemePresets.IDS)for(PanelAppearance.Option option:PanelAppearance.OPTIONS){
   int value=(Integer)PanelThemePresets.values(id).get(option.key);assertTrue(id+" / "+option.key,value>=option.min&&value<=option.max);
  }
 }
 @Test public void undoRestoresTheMostRecentlyReplacedTheme(){
  PanelThemePresets.apply(context,"minimal");var before=context.getSharedPreferences("veil_panel",0).getAll();
  PanelThemePresets.apply(context,"glass");assertTrue(PanelThemePresets.undo(context));assertEquals(before,context.getSharedPreferences("veil_panel",0).getAll());
 }
}
