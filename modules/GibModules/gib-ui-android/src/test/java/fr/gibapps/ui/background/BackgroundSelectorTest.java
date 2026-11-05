// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.ui.background;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.widget.CheckedTextView;
import android.widget.SeekBar;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import fr.gibapps.colorpicker.SvColorPicker;
import fr.gibapps.ui.R;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BackgroundSelectorTest {
    private ActivityController<Activity> controller;
    private Activity activity;
    private BackgroundSelector selector;
    private LinearLayout host;
    private final List<BackgroundConfig> changes = new ArrayList<>();
    private final BackgroundConfig initial = new BackgroundConfig(0, 0xff123456, 0xffabcdef, 0);

    @Before public void prepare() {
        controller = Robolectric.buildActivity(Activity.class).setup();
        activity = controller.get();
        ContextWrapper noPreferences = new ContextWrapper(activity) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                throw new AssertionError("Selector must not open preferences: " + name);
            }
        };
        selector = new BackgroundSelector(noPreferences, initial, 0xffbb8517);
        selector.setOnBackgroundChangedListener(changes::add);
        host = new LinearLayout(activity);
        host.addView(selector);
        activity.setContentView(host);
        controller.visible();
    }
    @After public void destroy() {
        selector.dismissDialogs();
        controller.pause().stop().destroy();
    }
    private Dialog openDirections() {
        selector.getChildAt(3).performClick();
        return ShadowDialog.getLatestDialog();
    }
    private void chooseType(int choice) {
        selector.getChildAt(0).performClick();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(2, dialog.getListView().getAdapter().getCount());
        assertEquals(activity.getString(R.string.gib_ui_background_solid), dialog.getListView().getAdapter().getItem(0));
        assertEquals(activity.getString(R.string.gib_ui_background_gradient), dialog.getListView().getAdapter().getItem(1));
        dialog.getListView().performItemClick(null, choice, choice);
    }
    private View option(Dialog d, int label) {
        return find(d.getWindow().getDecorView(), activity.getString(label), true);
    }
    private void action(Dialog d, int label) {
        find(d.getWindow().getDecorView(), activity.getString(label), false).performClick();
    }
    @Test public void solidAndGradientShowOnlyTheRequiredRowsAndTwoTypeChoices() {
        assertEquals(4, selector.getChildCount());
        assertEquals(View.GONE, selector.getChildAt(2).getVisibility());
        assertEquals(View.GONE, selector.getChildAt(3).getVisibility());
        chooseType(1);
        assertEquals(1, selector.getConfig().type);
        assertEquals(View.VISIBLE, selector.getChildAt(2).getVisibility());
        assertEquals(View.VISIBLE, selector.getChildAt(3).getVisibility());
        chooseType(0);
        assertEquals(View.GONE, selector.getChildAt(2).getVisibility());
        selector.setConfig(new BackgroundConfig(0, initial.color1, initial.color2, 5));
        chooseType(1);
        assertEquals(2, selector.getConfig().type);
        assertEquals(5, selector.getConfig().direction);
    }
    @Test public void sixThumbnailsHavePanospOrderAndImmediatePreview() {
        chooseType(1);
        Dialog d = openDirections();
        int[] labels = {R.string.gib_ui_background_top_bottom, R.string.gib_ui_background_bottom_top,
                R.string.gib_ui_background_left_right, R.string.gib_ui_background_right_left,
                R.string.gib_ui_background_center_out, R.string.gib_ui_background_out_center};
        int[] directions = {0,2,1,3,4,5};
        ViewGroup choices = (ViewGroup) option(d, labels[0]).getParent();
        assertEquals(6, choices.getChildCount());
        for (int i = 0; i < labels.length; i++) {
            View frame = option(d, labels[i]);
            assertSame(frame, choices.getChildAt(i));
            frame.performClick();
            assertEquals(directions[i], selector.getConfig().direction);
            assertEquals(directions[i] >= 4 ? 2 : 1, selector.getConfig().type);
            assertEquals(selector.getConfig(), changes.get(changes.size()-1));
            assertTrue(frame.isSelected());
        }
    }
    @Test public void cancelButtonRestoresTheEntireInitialConfigurationAndNotifiesHost() {
        chooseType(1);
        BackgroundConfig original = selector.getConfig();
        Dialog d = openDirections();
        option(d, R.string.gib_ui_background_out_center).performClick();
        action(d, R.string.gib_ui_background_cancel);
        assertEquals(original, selector.getConfig());
        assertEquals(original, changes.get(changes.size()-1));
        assertFalse(d.isShowing());
    }
    @Test public void backCancelRestoresButOkKeepsTheChosenConfiguration() {
        chooseType(1);
        BackgroundConfig original = selector.getConfig();
        Dialog d = openDirections();
        option(d, R.string.gib_ui_background_center_out).performClick();
        d.cancel();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(original, selector.getConfig());
        d = openDirections();
        option(d, R.string.gib_ui_background_out_center).performClick();
        action(d, R.string.gib_ui_background_ok);
        assertEquals(2, selector.getConfig().type);
        assertEquals(5, selector.getConfig().direction);
        selector.dismissDialogs();
        assertEquals(5, selector.getConfig().direction);
    }
    @Test public void hostUpdatesAreSilentAndEchoDoesNotCloseTheLiveDialog() {
        selector.setConfig(new BackgroundConfig(1, initial.color1, initial.color2, 0));
        assertTrue(changes.isEmpty());
        Dialog d = openDirections();
        option(d, R.string.gib_ui_background_center_out).performClick();
        selector.setConfig(changes.get(changes.size()-1));
        assertTrue(d.isShowing());
        int count = changes.size();
        selector.setConfig(initial);
        assertFalse(d.isShowing());
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(count, changes.size());
        assertEquals(initial, selector.getConfig());
    }
    @Test public void detachRollsBackPreviewOnceAndClosesWindow() {
        chooseType(1);
        BackgroundConfig original = selector.getConfig();
        Dialog d = openDirections();
        option(d, R.string.gib_ui_background_out_center).performClick();
        int count = changes.size();
        host.removeView(selector);
        assertFalse(d.isShowing());
        assertEquals(original, selector.getConfig());
        selector.dismissDialogs();
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(count+1, changes.size());
    }
    @Test public void sharedColorPickerOnlyPublishesOnOkAndNeverOpensPreferences() {
        chooseType(1);
        selector.getChildAt(2).performClick();
        Dialog d = ShadowDialog.getLatestDialog();
        SvColorPicker picker = (SvColorPicker) option(d, R.string.gib_ui_background_color_2);
        int before = changes.size();
        picker.layout(0,0,240,240);
        MotionEvent down = MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,70,80,0);
        MotionEvent up = MotionEvent.obtain(0,1,MotionEvent.ACTION_UP,70,80,0);
        picker.onTouchEvent(down); picker.onTouchEvent(up);
        down.recycle(); up.recycle();
        assertEquals(before, changes.size());
        action(d, R.string.gib_ui_background_ok);
        assertEquals(picker.getColor(), selector.getConfig().color2);
        assertEquals(initial.color1, selector.getConfig().color1);
        assertEquals(before+1, changes.size());
        selector.getChildAt(1).performClick();
        d = ShadowDialog.getLatestDialog();
        action(d, R.string.gib_ui_background_cancel);
        assertEquals(before+1, changes.size());
    }
    private TextView summary(int row) {
        ViewGroup labels = (ViewGroup) ((ViewGroup) selector.getChildAt(row)).getChildAt(0);
        return (TextView) labels.getChildAt(1);
    }
    private TextView button(Dialog dialog, int label) {
        return (TextView) find(dialog.getWindow().getDecorView(), activity.getString(label), false);
    }
    @Test public void initialAccentAndLaterDialogsUseCurrentArgbWithoutNotifying() {
        assertEquals(0xffbb8517, selector.getAccentColor());
        selector.getChildAt(1).performClick();
        Dialog first = ShadowDialog.getLatestDialog();
        assertEquals(0xffbb8517, button(first, R.string.gib_ui_background_ok).getCurrentTextColor());
        selector.dismissDialogs();
        selector.setAccentColor(0x80123456);
        assertEquals(0x80123456, selector.getAccentColor());
        selector.getChildAt(1).performClick();
        assertEquals(0x80123456, button(ShadowDialog.getLatestDialog(),
                R.string.gib_ui_background_ok).getCurrentTextColor());
        assertEquals(initial, selector.getConfig());
        assertTrue(changes.isEmpty());
    }
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void openDirectionFramesAndActionsUpdateWithoutResettingPreview() {
        selector.setConfig(new BackgroundConfig(1, initial.color1, initial.color2, 0));
        Dialog dialog = openDirections();
        View selected = option(dialog, R.string.gib_ui_background_top_bottom);
        assertEquals(0xffbb8517, borderPixel(selected));
        View inactive = option(dialog, R.string.gib_ui_background_bottom_top);
        int inactiveBefore = borderPixel(inactive);
        View newSelected = option(dialog, R.string.gib_ui_background_out_center);
        newSelected.performClick();
        BackgroundConfig preview = selector.getConfig();
        int count = changes.size();
        selector.setAccentColor(0xff22cc77);
        assertSame(dialog, ShadowDialog.getLatestDialog());
        assertTrue(dialog.isShowing());
        assertTrue(newSelected.isSelected());
        assertEquals(0xff22cc77, borderPixel(newSelected));
        assertEquals(inactiveBefore, borderPixel(inactive));
        assertEquals(0xff22cc77, button(dialog, R.string.gib_ui_background_cancel).getCurrentTextColor());
        assertEquals(0xff22cc77, button(dialog, R.string.gib_ui_background_ok).getCurrentTextColor());
        assertEquals(preview, selector.getConfig());
        assertEquals(count, changes.size());
        action(dialog, R.string.gib_ui_background_cancel);
        assertEquals(1, selector.getConfig().type);
        assertEquals(0, selector.getConfig().direction);
        assertEquals(0xff22cc77, selector.getAccentColor());
    }
    private int borderPixel(View frame) {
        Bitmap b = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888);
        frame.getBackground().setBounds(0, 0, 80, 120);
        frame.getBackground().draw(new Canvas(b));
        int color = b.getPixel(40, 1);
        b.recycle();
        return color;
    }
    @Test public void openColorDialogRetintsSliderAndActionsInPlace() {
        selector.getChildAt(1).performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();
        SeekBar hue = (SeekBar) option(dialog, R.string.gib_ui_background_hue);
        int progress = hue.getProgress();
        selector.setAccentColor(0xffabcdef);
        assertTrue(dialog.isShowing());
        assertSame(hue, option(dialog, R.string.gib_ui_background_hue));
        assertEquals(progress, hue.getProgress());
        assertEquals(0xffabcdef, hue.getThumbTintList().getDefaultColor());
        assertEquals(0xffabcdef, hue.getProgressTintList().getDefaultColor());
        assertEquals(0xffabcdef, button(dialog, R.string.gib_ui_background_ok).getCurrentTextColor());
        assertEquals(0xffabcdef, button(dialog, R.string.gib_ui_background_cancel).getCurrentTextColor());
        assertEquals(initial, selector.getConfig());
        assertTrue(changes.isEmpty());
    }
    @Test public void openTypeDialogRetintsExistingRadioRowsAndCancel() {
        selector.getChildAt(0).performClick();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        View list = dialog.getListView();
        list.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY));
        list.layout(0, 0, 600, 300);
        selector.setAccentColor(0xff3344ee);
        CheckedTextView row = (CheckedTextView) dialog.getListView().getChildAt(0);
        assertNotNull(row);
        assertEquals(0xff3344ee, row.getCheckMarkTintList().getColorForState(
                new int[]{android.R.attr.state_checked}, 0));
        assertEquals(0xa0ffffff, row.getCheckMarkTintList().getDefaultColor());
        assertEquals(0xff3344ee, dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getCurrentTextColor());
        assertTrue(dialog.isShowing());
        assertTrue(changes.isEmpty());
    }
    @Test public void colorSummariesAreAbsentByDefaultInBothModes() {
        assertEquals(View.GONE, summary(1).getVisibility());
        assertEquals(View.GONE, summary(2).getVisibility());
        selector.setConfig(new BackgroundConfig(1, initial.color1, initial.color2, 0));
        assertEquals(View.GONE, summary(1).getVisibility());
        assertEquals(View.GONE, summary(2).getVisibility());
        assertTrue(changes.isEmpty());
    }
    @Test public void suppliedSummariesUseSecondaryStyleWithoutChangingConfigOrRows() {
        View firstRow = selector.getChildAt(1);
        selector.setColor1Summary("Host summary one");
        selector.setColor2Summary("Host summary two");
        assertEquals("Host summary one", summary(1).getText().toString());
        assertEquals("Host summary two", summary(2).getText().toString());
        assertEquals(View.VISIBLE, summary(1).getVisibility());
        assertEquals(View.VISIBLE, summary(2).getVisibility());
        assertEquals(activity.getColor(R.color.gib_ui_background_text_secondary), summary(1).getCurrentTextColor());
        assertEquals(summary(1).getCurrentTextColor(), summary(2).getCurrentTextColor());
        assertSame(firstRow, selector.getChildAt(1));
        assertEquals(initial, selector.getConfig());
        assertTrue(changes.isEmpty());
    }
    @Test public void nullAndEmptyRemoveEitherSummary() {
        selector.setColor1Summary("One"); selector.setColor2Summary("Two");
        selector.setColor1Summary(null); selector.setColor2Summary("");
        assertEquals(View.GONE, summary(1).getVisibility());
        assertEquals(View.GONE, summary(2).getVisibility());
        selector.setColor1Summary("One"); selector.setColor2Summary("Two");
        selector.setColor1Summary(""); selector.setColor2Summary(null);
        assertEquals(View.GONE, summary(1).getVisibility());
        assertEquals(View.GONE, summary(2).getVisibility());
        assertTrue(changes.isEmpty());
    }
    @Test public void colorTwoSummaryFollowsRowVisibilityAndSurvivesModeChanges() {
        selector.setColor1Summary("One"); selector.setColor2Summary("Two");
        assertTrue(summary(1).isShown());
        assertFalse(summary(2).isShown());
        for (int type : new int[]{1, 2}) {
            selector.setConfig(new BackgroundConfig(type, initial.color1, initial.color2, type == 1 ? 0 : 4));
            assertTrue(summary(2).isShown());
            assertEquals("Two", summary(2).getText().toString());
        }
        selector.setConfig(initial);
        assertFalse(summary(2).isShown());
        assertTrue(summary(1).isShown());
        assertTrue(changes.isEmpty());
    }

    private View find(View v, String text, boolean description) {
        CharSequence candidate = description ? v.getContentDescription()
                : v instanceof TextView ? ((TextView) v).getText() : null;
        if (candidate != null && text.contentEquals(candidate)) return v;
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            for (int i = 0; i < group.getChildCount(); i++) {
                View result = find(group.getChildAt(i), text, description);
                if (result != null) return result;
            }
        }
        return null;
    }
}
