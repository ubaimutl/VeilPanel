package com.ubai.veilpanel.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import fr.gibapps.colorpicker.SvColorPicker;
import fr.gibapps.ui.background.BackgroundConfig;
import fr.gibapps.ui.background.BackgroundSelector;
import com.ubai.veilpanel.R;
import com.ubai.veilpanel.config.PanOSPConfigBackup;
import com.ubai.veilpanel.config.PanOSPPreferences;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class BackgroundSelectorIntegrationTest {
    private ActivityController<Activity> controller;
    private Activity activity;
    private PanOSPPreferences preferences;
    private PanelConfigurationView panel;
    private BackgroundSelector selector;

    @Before public void prepare() {
        controller = Robolectric.buildActivity(Activity.class).setup();
        activity = controller.get();
        preferences = new PanOSPPreferences(activity);
        preferences.setThemeBackgroundType(0);
        preferences.setThemeBackgroundColor(0x123456);
        preferences.setThemeBackgroundColor2(0xabcdef);
        preferences.setThemeBackgroundDirection(2);
        preferences.setThemeAccentColor(0xffbb8517);
        panel = new PanelConfigurationView(activity);
        activity.setContentView(panel);
        // ViewPager instantiates its pages only after layout. Open the Appearance page.
        fr.gibapps.ui.navigation.SectionPager pager = findPager(panel);
        pager.selectSection(2, false);
        panel.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));
        panel.layout(0,0,500,1000);
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        selector = findSelector(panel);
        assertNotNull(selector);
    }
    @After public void destroy() {
        if (selector != null) selector.dismissDialogs();
        controller.pause().stop().destroy();
    }
    private BackgroundConfig stored() {
        return new BackgroundConfig(preferences.getThemeBackgroundType(), preferences.getThemeBackgroundColor(),
                preferences.getThemeBackgroundColor2(), preferences.getThemeBackgroundDirection());
    }
    private void gradient() {
        selector.getChildAt(0).performClick();
        AlertDialog d = ShadowAlertDialog.getLatestAlertDialog();
        d.getListView().performItemClick(null, 1, 1);
    }
    @Test public void sharedSelectorStartsFromPanospValuesAndSolidHidesGradientRows() {
        assertEquals(stored(), selector.getConfig());
        assertEquals(0xff123456, selector.getConfig().color1);
        assertEquals(0xffabcdef, selector.getConfig().color2);
        assertEquals(0xffbb8517, selector.getAccentColor());
        assertEquals(View.GONE, selector.getChildAt(2).getVisibility());
        assertEquals(View.GONE, selector.getChildAt(3).getVisibility());
        gradient();
        assertEquals(1, preferences.getThemeBackgroundType());
        assertEquals(2, preferences.getThemeBackgroundDirection());
        assertEquals(View.VISIBLE, selector.getChildAt(2).getVisibility());
        assertEquals(View.VISIBLE, selector.getChildAt(3).getVisibility());
    }
    @Test @Config(qualifiers = "fr")
    public void panospTranslatedSummariesAreSuppliedToBothRows() {
        assertEquals(activity.getString(R.string.panel_config_background_color_1_summary), summary(1).getText().toString());
        assertEquals(activity.getString(R.string.panel_config_background_color_2_summary), summary(2).getText().toString());
        assertEquals(View.VISIBLE, summary(1).getVisibility());
        gradient();
        assertEquals(View.VISIBLE, selector.getChildAt(2).getVisibility());
        assertEquals(View.VISIBLE, summary(2).getVisibility());
    }
    @Test public void liveDirectionWritesPreferencesAndCancelRestoresAllFourValues() {
        gradient();
        BackgroundConfig original = stored();
        selector.getChildAt(3).performClick();
        Dialog d = ShadowDialog.getLatestDialog();
        find(d.getWindow().getDecorView(), activity.getString(fr.gibapps.ui.R.string.gib_ui_background_out_center), true).performClick();
        assertEquals(2, preferences.getThemeBackgroundType());
        assertEquals(5, preferences.getThemeBackgroundDirection());
        assertEquals(original.color1, preferences.getThemeBackgroundColor());
        assertEquals(original.color2, preferences.getThemeBackgroundColor2());
        assertEquals(stored(), selector.getConfig());
        assertTrue(d.isShowing());
        find(d.getWindow().getDecorView(), activity.getString(fr.gibapps.ui.R.string.gib_ui_background_cancel), false).performClick();
        assertEquals(original, stored());
        assertEquals(original, selector.getConfig());
    }
    @Test public void confirmedColorIsPersistedOpaqueWithoutReplacingSelector() {
        selector.getChildAt(1).performClick();
        Dialog d = ShadowDialog.getLatestDialog();
        SvColorPicker picker = (SvColorPicker) find(d.getWindow().getDecorView(),
                activity.getString(fr.gibapps.ui.R.string.gib_ui_background_color_1), true);
        picker.layout(0,0,240,240);
        MotionEvent down = MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,70,80,0);
        MotionEvent up = MotionEvent.obtain(0,1,MotionEvent.ACTION_UP,70,80,0);
        picker.onTouchEvent(down); picker.onTouchEvent(up); down.recycle(); up.recycle();
        find(d.getWindow().getDecorView(), activity.getString(fr.gibapps.ui.R.string.gib_ui_background_ok), false).performClick();
        assertEquals(picker.getColor(), preferences.getThemeBackgroundColor());
        assertEquals(255, preferences.getThemeBackgroundColor() >>> 24);
        assertEquals(stored(), selector.getConfig());
        assertSame(selector, findSelector(panel));
    }
    @Test public void refreshFromPreferencesUpdatesAccentAndConfigWithoutRebuilding() {
        preferences.setThemeBackgroundType(2);
        preferences.setThemeBackgroundDirection(5);
        preferences.setThemeAccentColor(0xff2299bb);
        panel.refreshFromPreferences();
        assertSame(selector, findSelector(panel));
        assertEquals(stored(), selector.getConfig());
        assertEquals(0xff2299bb, selector.getAccentColor());
        selector.getChildAt(3).performClick();
        TextView ok = (TextView) find(ShadowDialog.getLatestDialog().getWindow().getDecorView(),
                activity.getString(fr.gibapps.ui.R.string.gib_ui_background_ok), false);
        assertEquals(0xff2299bb, ok.getCurrentTextColor());
    }
    @Test public void importedConfigurationAndFactoryResetResynchronizeSameSelector() throws Exception {
        String json = "{\"format_version\":1,\"theme_background_type\":2,\"theme_background_color\":123456,"
                + "\"theme_background_color_2\":654321,\"theme_background_direction\":5,\"theme_accent_color\":777777}";
        assertTrue(preferences.publishConfiguration(PanOSPConfigBackup.parseConfiguration(json)));
        panel.refreshFromPreferences();
        assertEquals(2, selector.getConfig().type);
        assertEquals(5, selector.getConfig().direction);
        assertEquals(stored(), selector.getConfig());
        assertEquals(preferences.getThemeAccentColor(), selector.getAccentColor());
        assertTrue(preferences.resetPanelConfiguration());
        panel.refreshFromPreferences();
        assertEquals(stored(), selector.getConfig());
        assertEquals(preferences.getThemeAccentColor(), selector.getAccentColor());
        assertSame(selector, findSelector(panel));
    }
    private TextView summary(int row) {
        return (TextView) ((ViewGroup) ((ViewGroup) selector.getChildAt(row)).getChildAt(0)).getChildAt(1);
    }
    private fr.gibapps.ui.navigation.SectionPager findPager(View view) {
        if (view instanceof fr.gibapps.ui.navigation.SectionPager) return (fr.gibapps.ui.navigation.SectionPager)view;
        if (view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            fr.gibapps.ui.navigation.SectionPager p=findPager(((ViewGroup)view).getChildAt(i));if(p!=null)return p;
        }
        return null;
    }
    private BackgroundSelector findSelector(View view) {
        if (view instanceof BackgroundSelector) return (BackgroundSelector) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i=0; i<group.getChildCount(); i++) {
                BackgroundSelector result = findSelector(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }
    private View find(View view, String text, boolean description) {
        CharSequence value = description ? view.getContentDescription()
                : view instanceof TextView ? ((TextView) view).getText() : null;
        if (value != null && text.contentEquals(value)) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i=0; i<group.getChildCount(); i++) {
                View result = find(group.getChildAt(i), text, description);
                if (result != null) return result;
            }
        }
        return null;
    }
}
