package com.ubai.veilpanel.ui;
import android.content.Context;import android.graphics.Color;import android.view.*;import android.widget.TextView;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=33)
public class HeaderAppearanceTest {
 private Context context;
 @Before public void setup(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("veil_panel",0).edit().clear().commit();}
 @Test public void descriptionsBecomeWhiteWithoutChangingOtherTextColours(){
  int title=PanOSPTheme.notificationPrimaryText(context),secondary=PanOSPTheme.notificationSecondaryText(context);
  assertEquals(secondary,PanOSPTheme.notificationDescriptionText(context));
  context.getSharedPreferences("veil_panel",0).edit().putInt("description_brightness",100).commit();
  assertEquals(Color.WHITE,PanOSPTheme.notificationDescriptionText(context));assertEquals(title,PanOSPTheme.notificationPrimaryText(context));assertEquals(secondary,PanOSPTheme.notificationSecondaryText(context));
 }
 @Test public void headerCanDisappearOrUseCentredLargeClock(){
  context.getSharedPreferences("veil_panel",0).edit().putBoolean("header_date_visible",false).putBoolean("header_settings_visible",false).putBoolean("header_close_visible",false).putBoolean("header_clock_visible",false).commit();
  ShadeView hidden=new ShadeView(context,()->{});assertEquals(View.GONE,hidden.findViewWithTag("panel_header").getVisibility());assertEquals(View.GONE,hidden.findViewWithTag("panel_clock").getVisibility());
  context.getSharedPreferences("veil_panel",0).edit().putBoolean("header_clock_visible",true).putString("header_clock_alignment","center").putString("header_clock_style","sculpted").putInt("clock_size",120).commit();
  ShadeView visible=new ShadeView(context,()->{});TextView clock=visible.findViewWithTag("panel_clock");assertEquals(View.VISIBLE,clock.getVisibility());assertTrue(clock instanceof SculptedClockView);assertEquals(Gravity.CENTER_HORIZONTAL,clock.getGravity()&Gravity.HORIZONTAL_GRAVITY_MASK);assertEquals(120,com.ubai.veilpanel.config.PanelAppearance.get(context,"clock_size"));
 }
}
