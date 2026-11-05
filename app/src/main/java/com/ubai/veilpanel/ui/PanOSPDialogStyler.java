package com.ubai.veilpanel.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.ubai.veilpanel.config.PanOSPPreferences;

/** Native-dialog counterpart of GibClock's GibDialogStyler; no AppCompat dependency. */
public final class PanOSPDialogStyler {
    private PanOSPDialogStyler() { }

    public static AlertDialog show(AlertDialog.Builder builder) {
        AlertDialog dialog = builder.create();
        dialog.show();
        apply(dialog);
        return dialog;
    }

    public static void apply(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(roundedBackground(dialog.getContext()));
        window.getDecorView().setClipToOutline(true);

        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.dimAmount = 0f;
        window.setAttributes(attrs);

        int accent = new PanOSPPreferences(dialog.getContext()).getThemeAccentColor();
        if (dialog instanceof AlertDialog) {
            AlertDialog alert = (AlertDialog) dialog;
            styleDialogAction(alert.getButton(AlertDialog.BUTTON_POSITIVE), accent);
            styleDialogAction(alert.getButton(AlertDialog.BUTTON_NEGATIVE), accent);
            styleDialogAction(alert.getButton(AlertDialog.BUTTON_NEUTRAL), accent);
        }
        styleControls(window.getDecorView(), accent);
    }

    public static void styleDialogAction(Button button) {
        if (button == null) return;
        int accent = new PanOSPPreferences(button.getContext()).getThemeAccentColor();
        styleDialogAction(button, accent);
    }

    private static void styleDialogAction(Button button, int accent) {
        if (button == null) return;

        button.setTextColor(accent);

        TypedValue ripple = new TypedValue();
        if (button.getContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, ripple, true)
                && ripple.resourceId != 0) {
            button.setBackgroundResource(ripple.resourceId);
        } else {
            button.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    public static GradientDrawable roundedBackground(Context context) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(14f * context.getResources().getDisplayMetrics().density);
        background.setColor(Color.rgb(48, 48, 48));
        return background;
    }

    private static void styleControls(View view, int accent) {
        int normal = Color.argb(160, 255, 255, 255);
        ColorStateList checked = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{accent, normal});
        ColorStateList focused = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
                new int[]{accent, normal});
        if (view instanceof CompoundButton) {
            ((CompoundButton) view).setButtonTintList(checked);
        } else if (view instanceof CheckedTextView) {
            ((CheckedTextView) view).setCheckMarkTintList(checked);
            ((CheckedTextView) view).setCompoundDrawableTintList(checked);
        } else if (view instanceof SeekBar) {
            ((SeekBar) view).setProgressTintList(ColorStateList.valueOf(accent));
            ((SeekBar) view).setThumbTintList(ColorStateList.valueOf(accent));
        } else if (view instanceof ProgressBar) {
            ((ProgressBar) view).setProgressTintList(ColorStateList.valueOf(accent));
        } else if (view instanceof EditText) {
            ((EditText) view).setBackgroundTintList(focused);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                styleControls(group.getChildAt(i), accent);
            }
        }
    }
}
