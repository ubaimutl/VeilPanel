// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.ui.background;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import fr.gibapps.colorpicker.SvColorPicker;
import fr.gibapps.ui.R;
import java.util.Objects;

/** EXPERIMENTAL. PanOSP background rows and dialogs, with no persistence or app dependency.
 * Main thread only. Supply an Activity/themed Activity context for dialogs.
 * The listener receives live direction previews and rollback on cancel. Colors
 * and Solid/Gradient choices are notified on confirmation. The host owns storage.
 */
public final class BackgroundSelector extends LinearLayout {
    public interface OnBackgroundChangedListener {
        void onBackgroundChanged(BackgroundConfig config);
    }

    private static final int[] DIRECTIONS = {R.string.gib_ui_background_top_bottom,
            R.string.gib_ui_background_left_right, R.string.gib_ui_background_bottom_top,
            R.string.gib_ui_background_right_left, R.string.gib_ui_background_center_out,
            R.string.gib_ui_background_out_center};
    private BackgroundConfig config;
    private int accentColor;
    private OnBackgroundChangedListener listener;
    private final TextView typeValue, directionValue;
    private final TextView color1Summary, color2Summary;
    private final View color1Swatch, color2Swatch;
    private final View color2Row, directionRow;
    private Dialog activeDialog;
    private FrameLayout[] directionFrames;
    private Runnable rollback;

    public BackgroundSelector(Context context, BackgroundConfig config, int accentColor) {
        super(context);
        this.config = Objects.requireNonNull(config, "config");
        this.accentColor = accentColor;
        setOrientation(VERTICAL);
        typeValue = addRow(R.string.gib_ui_background_type, null, v -> showTypeDialog());
        color1Swatch = new View(context);
        color1Summary = addRow(R.string.gib_ui_background_color_1, color1Swatch, v -> showColorDialog(false));
        color2Swatch = new View(context);
        color2Summary = addRow(R.string.gib_ui_background_color_2, color2Swatch, v -> showColorDialog(true));
        setColor1Summary(null);
        setColor2Summary(null);
        color2Row = getChildAt(2);
        directionValue = addRow(R.string.gib_ui_background_gradient_type, null, v -> showDirectionDialog());
        directionRow = getChildAt(3);
        refresh();
    }

    public BackgroundConfig getConfig() { return config; }

    /** ARGB, unchanged from the constructor convention; no preference/config callback. */
    public int getAccentColor() { return accentColor; }

    /** Updates visible dialog controls in place and all subsequently opened dialogs. */
    public void setAccentColor(int color) {
        accentColor = color;
        if (directionFrames != null) {
            for (FrameLayout frame : directionFrames) directionFrame(frame, frame.isSelected());
        }
        if (activeDialog != null && activeDialog.getWindow() != null) {
            tintAccentControls(activeDialog.getWindow().getDecorView());
        }
    }

    /** Optional host text, never persisted; null/empty hides the secondary line. */
    public void setColor1Summary(CharSequence summary) { setSummary(color1Summary, summary); }

    /** The entire color-2 row remains hidden in solid mode, retaining its summary. */
    public void setColor2Summary(CharSequence summary) { setSummary(color2Summary, summary); }

    private void setSummary(TextView view, CharSequence summary) {
        view.setText(summary);
        view.setVisibility(summary == null || summary.length() == 0 ? GONE : VISIBLE);
    }

    private void tintAccentControls(View view) {
        if (view instanceof Button) {
            ((Button) view).setTextColor(accentColor);
        } else if (view instanceof CheckedTextView) {
            ((CheckedTextView) view).setCheckMarkTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{accentColor, 0xa0ffffff}));
        } else if (view instanceof SeekBar) {
            ((SeekBar) view).setProgressTintList(ColorStateList.valueOf(accentColor));
            ((SeekBar) view).setThumbTintList(ColorStateList.valueOf(accentColor));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) tintAccentControls(group.getChildAt(i));
        }
    }

    /** Silent host update. A different config supersedes any dialog without rollback.
     * Echoing the last callback's config leaves the current dialog open.
     */
    public void setConfig(BackgroundConfig next) {
        Objects.requireNonNull(next, "config");
        if (next.equals(config)) return;
        finishDialog(activeDialog, null);
        config = next;
        refresh();
    }

    public void setOnBackgroundChangedListener(OnBackgroundChangedListener listener) {
        this.listener = listener;
    }

    /** Cancel transient previews synchronously; also called when detached. */
    public void dismissDialogs() { finishDialog(activeDialog, rollback); }

    @Override protected void onDetachedFromWindow() {
        dismissDialogs();
        super.onDetachedFromWindow();
    }

    private void publish(BackgroundConfig next) {
        config = next;
        refresh();
        if (listener != null) listener.onBackgroundChanged(next);
    }

    private TextView addRow(int title, View swatch, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, 0, 0);
        row.setMinimumHeight(dp(64));
        row.setOnClickListener(click);
        row.setFocusable(true);
        LinearLayout labels = new LinearLayout(getContext());
        labels.setOrientation(VERTICAL);
        labels.setPadding(0, dp(16), 0, dp(16));
        label(labels, title, 16);
        TextView value = label(labels, 0, 14);
        value.setTextColor(getContext().getColor(R.color.gib_ui_background_text_secondary));
        value.setMaxLines(10);
        row.addView(labels, new LayoutParams(0, -2, 1));
        if (swatch != null) {
            FrameLayout frame = new FrameLayout(getContext());
            frame.addView(swatch, new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
            LayoutParams params = new LayoutParams(dp(48), dp(48));
            params.leftMargin = dp(16);
            row.addView(frame, params);
        }
        addView(row, new LayoutParams(-1, -2));
        return value;
    }

    private void refresh() {
        typeValue.setText(config.type == BackgroundConfig.SOLID
                ? R.string.gib_ui_background_solid : R.string.gib_ui_background_gradient);
        directionValue.setText(DIRECTIONS[config.direction]);
        swatch(color1Swatch, config.color1);
        swatch(color2Swatch, config.color2);
        int visibility = config.type == BackgroundConfig.SOLID ? GONE : VISIBLE;
        color2Row.setVisibility(visibility);
        directionRow.setVisibility(visibility);
    }

    private void swatch(View view, int color) {
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(color);
        disc.setStroke(dp(1), Color.rgb(136, 136, 136));
        view.setBackground(disc);
    }

    private void showTypeDialog() {
        dismissDialogs();
        String[] labels = {getContext().getString(R.string.gib_ui_background_solid),
                getContext().getString(R.string.gib_ui_background_gradient)};
        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.GibUi_BackgroundDialog)
                .setTitle(R.string.gib_ui_background_type)
                .setSingleChoiceItems(labels, config.type == 0 ? 0 : 1, (d, which) -> {
                    BackgroundConfig next = new BackgroundConfig(which == 0 ? 0 : (config.direction >= 4 ? 2 : 1),
                            config.color1, config.color2, config.direction);
                    finishDialog(activeDialog, () -> publish(next));
                }).setNegativeButton(R.string.gib_ui_background_cancel,
                        (d, which) -> finishDialog(activeDialog, null)).create();
        manage(dialog, null);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(dialogSurface());
            window.getDecorView().setClipToOutline(true);
            removeBackgroundDim(window);
        }
        styleDialogAction(dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
        // Native checked rows must use the supplied accent, not the application's theme accent.
        dialog.getListView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            for (int i = 0; i < dialog.getListView().getChildCount(); i++) {
                View child = dialog.getListView().getChildAt(i);
                if (child instanceof CheckedTextView) {
                    ((CheckedTextView) child).setCheckMarkTintList(new ColorStateList(
                            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                            new int[]{accentColor, 0xa0ffffff}));
                }
            }
        });
    }

    private void showDirectionDialog() {
        dismissDialogs();
        BackgroundConfig original = config;
        Dialog dialog = new Dialog(getContext(), R.style.GibUi_BackgroundDialog);
        LinearLayout root = dialogRoot(R.string.gib_ui_background_gradient_type, false);
        LinearLayout choices = new LinearLayout(getContext());
        choices.setGravity(Gravity.CENTER);
        int[] order = {0, 2, 1, 3, 4, 5};
        FrameLayout[] frames = new FrameLayout[6];
        for (int i = 0; i < order.length; i++) {
            int option = order[i];
            FrameLayout frame = new FrameLayout(getContext());
            frame.setFocusable(true);
            frame.setContentDescription(getContext().getString(DIRECTIONS[option]));
            directionFrame(frame, option == config.direction);
            View preview = new View(getContext());
            preview.setBackground(new GibBackgroundDrawable(option >= 4 ? 2 : 1,
                    0xffe0e0e0, 0xff303030, option, dp(5)));
            FrameLayout.LayoutParams inner = new FrameLayout.LayoutParams(dp(28), dp(48), Gravity.CENTER);
            inner.setMargins(dp(3), dp(3), dp(3), dp(3));
            frame.addView(preview, inner);
            LayoutParams outer = new LayoutParams(dp(36), dp(56));
            if (i > 0) outer.leftMargin = dp(6);
            choices.addView(frame, outer);
            frames[i] = frame;
            frame.setOnClickListener(v -> {
                for (int j = 0; j < frames.length; j++) directionFrame(frames[j], order[j] == option);
                publish(new BackgroundConfig(option >= 4 ? 2 : 1, config.color1, config.color2, option));
            });
        }
        root.addView(choices, new LayoutParams(-1, -2));
        Runnable restore = () -> publish(original);
        addActions(root, dialog, restore, () -> publish(config), true);
        manage(dialog, restore);
        directionFrames = frames;
        dialog.setCanceledOnTouchOutside(false);
        show(dialog, root, true);
    }

    private void directionFrame(FrameLayout frame, boolean selected) {
        GradientDrawable border = new GradientDrawable();
        border.setCornerRadius(dp(8));
        border.setColor(Color.TRANSPARENT);
        border.setStroke(dp(selected ? 2 : 1), selected ? accentColor : Color.rgb(105, 105, 105));
        frame.setBackground(border);
        frame.setSelected(selected);
    }

    private void showColorDialog(boolean second) {
        dismissDialogs();
        int initial = second ? config.color2 : config.color1;
        int title = second ? R.string.gib_ui_background_color_2 : R.string.gib_ui_background_color_1;
        Dialog dialog = new Dialog(getContext(), R.style.GibUi_BackgroundDialog);
        LinearLayout root = dialogRoot(title, true);
        SvColorPicker picker = new SvColorPicker(getContext());
        picker.setContentDescription(getContext().getString(title));
        picker.setColor(initial);
        LayoutParams pickerParams = new LayoutParams(-1, dp(240));
        pickerParams.topMargin = dp(14);
        root.addView(picker, pickerParams);
        SeekBar hue = new SeekBar(getContext());
        hue.setContentDescription(getContext().getString(R.string.gib_ui_background_hue));
        hue.setMax(360);
        hue.setSplitTrack(false);
        hue.setProgressTintList(ColorStateList.valueOf(accentColor));
        hue.setThumbTintList(ColorStateList.valueOf(accentColor));
        float[] hsv = new float[3];
        Color.colorToHSV(initial, hsv);
        hue.setProgress(Math.round(hsv[0]));
        root.addView(hue, new LayoutParams(-1, dp(48)));
        int[] selected = {initial};
        picker.setOnColorChangeListener((color, fromUser) -> {
            if (fromUser) selected[0] = color | 0xff000000;
        });
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                picker.setHue(progress);
                selected[0] = picker.getColor() | 0xff000000;
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        addActions(root, dialog, null, () -> publish(new BackgroundConfig(config.type,
                second ? config.color1 : selected[0], second ? selected[0] : config.color2,
                config.direction)), false);
        manage(dialog, null);
        show(dialog, root, false);
    }

    private LinearLayout dialogRoot(int title, boolean color) {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(VERTICAL);
        root.setPadding(dp(color ? 20 : 16), dp(color ? 16 : 14), dp(color ? 20 : 16), dp(color ? 12 : 10));
        root.setBackground(dialogSurface());
        root.setClipToOutline(true);
        TextView titleView = label(root, title, color ? 20 : 18);
        if (!color) titleView.setPadding(0, 0, 0, dp(12));
        return root;
    }

    private GradientDrawable dialogSurface() {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(14));
        background.setColor(getContext().getColor(R.color.gib_ui_background_dialog));
        return background;
    }

    private TextView label(LinearLayout parent, int title, int size) {
        TextView view = new TextView(getContext());
        if (title != 0) view.setText(title);
        view.setTextSize(size);
        view.setTextColor(getContext().getColor(R.color.gib_ui_background_text_primary));
        parent.addView(view);
        return view;
    }

    private void addActions(LinearLayout root, Dialog dialog, Runnable cancel, Runnable apply, boolean direction) {
        LinearLayout actions = new LinearLayout(getContext());
        actions.setGravity(Gravity.END);
        if (direction) actions.setPadding(0, dp(10), 0, 0);
        Button cancelButton = new Button(dialog.getContext());
        cancelButton.setText(R.string.gib_ui_background_cancel);
        styleDialogAction(cancelButton);
        Button ok = new Button(dialog.getContext());
        ok.setText(R.string.gib_ui_background_ok);
        styleDialogAction(ok);
        actions.addView(cancelButton);
        actions.addView(ok);
        root.addView(actions);
        cancelButton.setOnClickListener(v -> finishDialog(dialog, cancel));
        ok.setOnClickListener(v -> finishDialog(dialog, apply));
    }

    private void manage(Dialog dialog, Runnable onCancel) {
        activeDialog = dialog;
        rollback = onCancel;
        dialog.setOnCancelListener(d -> finishDialog(dialog, onCancel));
    }

    private void finishDialog(Dialog dialog, Runnable action) {
        if (dialog == null || dialog != activeDialog) return;
        activeDialog = null;
        directionFrames = null;
        rollback = null;
        dialog.setOnCancelListener(null);
        dialog.dismiss();
        if (action != null) action.run();
    }

    private void show(Dialog dialog, LinearLayout root, boolean bottom) {
        dialog.setContentView(root);
        dialog.show();
        Window window = dialog.getWindow();
        if (window == null) return;

        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        removeBackgroundDim(window);

        if (bottom) {
            window.setLayout(-1, -2);

            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.y = dp(20);
            window.setAttributes(params);
        } else {
            int width = (int) (
                    getResources().getDisplayMetrics().widthPixels * 0.88f
            );

            window.setLayout(
                    width,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity =
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            params.y = dp(16);
            window.setAttributes(params);
        }
    }

    private void removeBackgroundDim(Window window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.dimAmount = 0f;
        window.setAttributes(attrs);
    }

    private void styleDialogAction(Button button) {
        if (button == null) return;

        button.setTextColor(accentColor);

        TypedValue ripple = new TypedValue();
        if (button.getContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, ripple, true)
                && ripple.resourceId != 0) {
            button.setBackgroundResource(ripple.resourceId);
        } else {
            button.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
