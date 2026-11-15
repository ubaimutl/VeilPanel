package com.ubai.veilpanel.ui;

import android.content.Context;
import com.ubai.veilpanel.config.PanelRouting;
import com.ubai.veilpanel.system.SystemWindowMetrics;

/** Shared geometry for the closed pull catcher and its settings preview. */
public final class PanelPullArea {
    private PanelPullArea() {}
    public static int defaultHeightDp(Context context) {
        return Math.round(SystemWindowMetrics.getStatusBarHeight(context)
                / context.getResources().getDisplayMetrics().density) + 8;
    }
    public static int height(Context context) {
        android.content.SharedPreferences prefs=context.getSharedPreferences("veil_panel",0);
        if (!prefs.contains("pull_area_height"))
            return SystemWindowMetrics.getStatusBarHeight(context)+SystemWindowMetrics.dp(context,8);
        return SystemWindowMetrics.dp(context,Math.max(8,Math.min(120,prefs.getInt("pull_area_height",defaultHeightDp(context)))));
    }
    public static int width(Context context,int screenWidth) {
        return Math.round(screenWidth*PanelRouting.split(context)/100f);
    }
}
