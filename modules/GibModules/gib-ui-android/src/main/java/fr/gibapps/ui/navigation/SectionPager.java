// SPDX-License-Identifier: Apache-2.0
package fr.gibapps.ui.navigation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

/** Experimental section navigation. Host owns titles, page content, persistence and insets.
 * All methods must be called on the UI thread. No application state is read or written.
 */
public final class SectionPager extends LinearLayout {
    public interface OnSectionSelectedListener { void onSectionSelected(int index); }
    /** Return true for a touched control whose entire gesture belongs to the child. */
    public interface GestureProtection { boolean protects(View view); }

    private SectionAppearance appearance = SectionAppearance.DEFAULT;
    private final SectionState state = new SectionState();
    private final List<View> pages = new ArrayList<>();
    private final List<TextView> tabs = new ArrayList<>();
    private final FrameLayout header;
    private final HorizontalScrollView menu;
    private final LinearLayout tabRow;
    private final NativePager body;
    private final PagerAdapter pageAdapter;
    private View overflow;
    private boolean swipeEnabled;
    private boolean primaryNavigation;
    private GestureProtection protection = view -> false;
    private OnSectionSelectedListener listener;

    public SectionPager(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(appearance.backgroundColor);

        header = new FrameLayout(context);
        menu = new HorizontalScrollView(context);
        menu.setHorizontalScrollBarEnabled(false);
        tabRow = new LinearLayout(context);
        tabRow.setOrientation(HORIZONTAL);
        menu.addView(tabRow, new ViewGroup.LayoutParams(-2, -1));
        header.addView(menu, new FrameLayout.LayoutParams(-1, -1));
        addView(header, new LinearLayout.LayoutParams(-1, dp(52)));

        pageAdapter = new PagerAdapter() {
            @Override public int getCount() { return pages.size(); }

            @Override public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override public Object instantiateItem(ViewGroup container, int position) {
                View page = pages.get(position);
                if (page.getParent() == null) {
                    container.addView(page, new ViewGroup.LayoutParams(-1, -1));
                } else if (page.getParent() != container) {
                    throw new IllegalStateException("Page attached outside SectionPager");
                }
                return page;
            }

            @Override public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        };

        body = new NativePager(context);
        body.setAdapter(pageAdapter);
        body.setOffscreenPageLimit(1);
        body.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override public void onPageSelected(int position) {
                commitSelection(position);
            }
        });
        addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        menu.setOnScrollChangeListener((v, x, y, oldX, oldY) -> updateOverflow());
        menu.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            updateOverflow();
            revealSelected(false);
        });
        tabRow.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> updateOverflow());
    }

    /** Null restores the original appearance; selection and gestures are untouched. */
    public void setAppearance(SectionAppearance appearance) {
        this.appearance = appearance == null ? SectionAppearance.DEFAULT : appearance;
        setBackgroundColor(this.appearance.backgroundColor);
        updateTabs();
    }

    public int getCurrentSection() { return state.getSelected(); }
    public int getSectionCount() { return state.getCount(); }

    public void setSwipeEnabled(boolean enabled) {
        swipeEnabled = enabled;
        if (!enabled) body.restoreCurrentSection();
    }

    public boolean isSwipeEnabled() { return swipeEnabled; }

    /**
     * Main navigation uses the GibClock/PanOSP grammar:
     * icon above label, compact two-line label and taller header.
     * Must be configured before sections are added.
     */
    public void setPrimaryNavigation(boolean primary) {
        if (state.getCount() != 0) {
            throw new IllegalStateException(
                    "Navigation level must be selected before adding sections");
        }
        primaryNavigation = primary;
        ViewGroup.LayoutParams params = header.getLayoutParams();
        params.height = dp(primary ? 76 : 52);
        header.setLayoutParams(params);
    }

    public boolean isPrimaryNavigation() { return primaryNavigation; }

    /**
     * Shows or hides the navigation chrome owned by SectionPager.
     * The ViewPager body, swipe behavior, section state and callbacks are unchanged.
     * Hosts with their own navigation UI can therefore use SectionPager as a paging engine only.
     */
    public void setNavigationVisible(boolean visible) {
        header.setVisibility(visible ? VISIBLE : GONE);
    }

    public boolean isNavigationVisible() {
        return header.getVisibility() != GONE;
    }

    public void setOnSectionSelectedListener(OnSectionSelectedListener listener) { this.listener = listener; }
    public void setGestureProtection(GestureProtection protection) {
        this.protection = protection == null ? view -> false : protection;
    }

    /** Replaceable host-supplied affordance, not a section. Click reveals hidden tabs. */
    public void setRightOverflowIndicator(View indicator) {
        if (indicator != null && indicator.getParent() != null) {
            throw new IllegalArgumentException("Indicator already attached");
        }
        if (overflow != null) header.removeView(overflow);
        overflow = indicator;
        if (indicator != null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -1, Gravity.RIGHT);
            header.addView(indicator, params);
            indicator.setVisibility(INVISIBLE);
            indicator.setOnClickListener(v -> revealNextHidden());
        }
        post(this::updateOverflow);
    }

    /** Supply an unattached secondary-navigation page. */
    public void addSection(CharSequence title, View page) {
        addSection(title, 0, page);
    }

    /**
     * Supply an unattached page with an optional navigation icon.
     * The icon is used when primary navigation is enabled.
     */
    public void addSection(CharSequence title, int iconResId, View page) {
        if (page == null || page.getParent() != null) {
            throw new IllegalArgumentException("Page already attached or null");
        }

        final int index = state.getCount();

        TextView tab = new TextView(getContext());
        tab.setText(title);
        tab.setGravity(Gravity.CENTER);
        tab.setFocusable(true);
        tab.setOnClickListener(v -> selectSection(index, true));

        if (primaryNavigation) {
            tab.setTextSize(11);
            tab.setMaxLines(2);
            tab.setMinLines(1);
            tab.setPadding(dp(12), dp(2), dp(12), dp(3));
            tab.setMinWidth(dp(72));
            tab.setCompoundDrawablePadding(dp(5));

            if (iconResId != 0) {
                Drawable icon = getContext().getDrawable(iconResId);
                if (icon != null) {
                    icon = icon.mutate();
                    tab.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            null, icon, null, null);
                }
            }
        } else {
            tab.setTextSize(15);
            tab.setSingleLine(true);
            tab.setPadding(dp(16), 0, dp(16), 0);
            tab.setMinWidth(dp(64));
        }

        tabRow.addView(tab, new LinearLayout.LayoutParams(-2, -1));
        tabs.add(tab);

        pages.add(page);
        state.addSection();
        pageAdapter.notifyDataSetChanged();
        // GibClock keeps all five main/settings pages warm. Do the same here: page creation and
        // layout stay outside the gesture path and ViewPager owns only the scrolling physics.
        body.setOffscreenPageLimit(Math.max(1, pages.size() - 1));
        if (pages.size() == 1) body.setCurrentItem(0, false);

        updateTabs();
        post(this::updateOverflow);
    }

    public void selectSection(int index, boolean animate) {
        if (index < 0 || index >= state.getCount() || index == state.getSelected()) return;
        body.setCurrentItem(index, animate);
    }

    /** Select the next/previous section when one exists. Edges are bounded like GibClock/ViewPager. */
    public void showAdjacentSection(int direction) {
        if (direction == 0 || state.getSelected() < 0) return;
        int target = state.getSelected() + Integer.signum(direction);
        if (target >= 0 && target < state.getCount()) selectSection(target, true);
    }

    private void commitSelection(int index) {
        if (!state.select(index)) return;
        updateTabs();
        post(() -> revealSelected(true));
        if (listener != null) listener.onSectionSelected(index);
    }

    private void updateTabs() {
        for (int i = 0; i < tabs.size(); i++) {
            TextView tab = tabs.get(i);
            boolean active = i == state.getSelected();
            tab.setSelected(active);
            int color = active
                    ? appearance.activeTextColor
                    : appearance.inactiveTextColor;

            tab.setTextColor(color);
            tab.setTypeface(
                    null,
                    active
                            ? android.graphics.Typeface.BOLD
                            : android.graphics.Typeface.NORMAL
            );

            if (primaryNavigation) {
                Drawable icon = tab.getCompoundDrawablesRelative()[1];
                if (icon != null) icon.setTint(color);
            }

            tab.setBackground(new TabUnderlineDrawable(active));
        }
    }

    private final class TabUnderlineDrawable extends Drawable {
        private final boolean active;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        TabUnderlineDrawable(boolean active) { this.active = active; }

        @Override public void draw(Canvas canvas) {
            if (!active) return;
            Rect bounds = getBounds();
            paint.setColor(appearance.activeTabColor);
            int inset = dp(12);
            int height = dp(3);
            canvas.drawRect(
                    bounds.left + inset,
                    bounds.bottom - height,
                    Math.max(bounds.left + inset, bounds.right - inset),
                    bounds.bottom,
                    paint);
        }

        @Override public void setAlpha(int alpha) { }

        @Override public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    private void updateOverflow() {
        if (overflow != null) overflow.setVisibility(SectionState.hasHiddenRight(
                tabRow.getWidth(), menu.getWidth(), menu.getScrollX()) ? VISIBLE : INVISIBLE);
    }

    private void revealSelected(boolean smooth) {
        if (state.getSelected() >= 0) reveal(tabs.get(state.getSelected()), smooth);
    }

    private void reveal(View tab, boolean smooth) {
        int x = menu.getScrollX();
        int covered = overflow != null && overflow.getVisibility() == VISIBLE ? overflow.getWidth() : 0;
        if (tab.getLeft() < x) x = tab.getLeft();
        else if (tab.getRight() > x + menu.getWidth() - covered) {
            x = tab.getRight() - menu.getWidth() + covered;
        }
        if (tab.getWidth() > menu.getWidth() - covered) x = tab.getLeft();
        x = Math.max(0, Math.min(x, tabRow.getWidth() - menu.getWidth()));
        if (smooth) menu.smoothScrollTo(x, 0); else menu.scrollTo(x, 0);
        updateOverflow();
    }

    private void revealNextHidden() {
        int edge = menu.getScrollX() + menu.getWidth() - (overflow == null ? 0 : overflow.getWidth());
        for (View tab : tabs) {
            if (tab.getRight() > edge) {
                menu.smoothScrollTo(Math.min(Math.max(tab.getLeft(), menu.getScrollX() + dp(48)),
                        Math.max(0, tabRow.getWidth() - menu.getWidth())), 0);
                return;
            }
        }
    }

    private boolean protectsDown(View view, float rawX, float rawY) {
        if (view.getVisibility() != VISIBLE) return false;
        Rect rect = new Rect();
        if (!view.getLocalVisibleRect(rect)) return false;
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        rect.offset(location[0], location[1]);
        if (view instanceof SeekBar && rawY >= rect.top - dp(12) && rawY <= rect.bottom + dp(12)) return true;
        if (rect.contains((int) rawX, (int) rawY) && protection.protects(view)) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (protectsDown(group.getChildAt(i), rawX, rawY)) return true;
            }
        }
        return false;
    }

    /**
     * Thin GibUI wrapper around the same AndroidX ViewPager engine used by GibClock.
     * GibUI keeps host gesture exclusions; AndroidX owns drag, fling, settling and frame timing.
     */
    private final class NativePager extends ViewPager {
        private boolean gestureBlocked;

        NativePager(Context context) { super(context); }

        @Override public boolean onInterceptTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    gestureBlocked = !swipeEnabled
                            || protectsDown(this, event.getRawX(), event.getRawY());
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    gestureBlocked = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (gestureBlocked) {
                        gestureBlocked = false;
                        return false;
                    }
                    break;
                default:
                    break;
            }
            if (gestureBlocked || !swipeEnabled) return false;
            try {
                return super.onInterceptTouchEvent(event);
            } catch (IllegalArgumentException ignored) {
                // Defensive guard for malformed pointer streams from OEM gesture dispatch.
                return false;
            }
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (gestureBlocked || !swipeEnabled) {
                if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    gestureBlocked = false;
                }
                return false;
            }
            try {
                boolean handled = super.onTouchEvent(event);
                if (event.getActionMasked() == MotionEvent.ACTION_UP
                        || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    gestureBlocked = false;
                }
                return handled;
            } catch (IllegalArgumentException ignored) {
                gestureBlocked = false;
                return false;
            }
        }

        void restoreCurrentSection() {
            gestureBlocked = false;
            int selected = state.getSelected();
            if (selected >= 0 && selected < pages.size()) setCurrentItem(selected, false);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
