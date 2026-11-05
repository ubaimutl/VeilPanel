package com.ubai.veilpanel.system;

import android.content.Context;
import android.graphics.Insets;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

/**
 * System-bar geometry used by the overlay windows.
 */
public final class SystemWindowMetrics {

    private SystemWindowMetrics() {
    }

    public static int getStatusBarHeight(Context context) {
        int id = context.getResources().getIdentifier(
                "status_bar_height",
                "dimen",
                "android"
        );

        if (id > 0) {
            return context.getResources().getDimensionPixelSize(id);
        }

        return dp(context, 24);
    }

    public static int getUsableScreenHeight(
            Context context,
            WindowManager windowManager,
            int statusBarHeight) {

        WindowMetrics metrics = windowManager.getCurrentWindowMetrics();

        Insets navigationInsets =
                metrics.getWindowInsets().getInsetsIgnoringVisibility(
                        WindowInsets.Type.navigationBars()
                );

        int fullHeight = metrics.getBounds().height();
        int usableHeight = fullHeight - navigationInsets.bottom;

        if (usableHeight > statusBarHeight) {
            return usableHeight;
        }

        return context.getResources()
                .getDisplayMetrics()
                .heightPixels;
    }

    public static int dp(Context context, int value) {
        return Math.round(
                value * context.getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
