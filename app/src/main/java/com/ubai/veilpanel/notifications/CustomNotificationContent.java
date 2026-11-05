package com.ubai.veilpanel.notifications;

import android.util.Log;
import android.widget.RemoteViews;

/** Notification-lifetime custom content; owns no Context or inflated View. */
public final class CustomNotificationContent {
    private final RemoteViews contentView;
    private final RemoteViews bigContentView;

    private CustomNotificationContent(RemoteViews compact, RemoteViews expanded) {
        contentView = compact;
        bigContentView = expanded;
    }

    public static CustomNotificationContent from(RemoteViews source) {
        return from(source, null);
    }

    public static CustomNotificationContent from(RemoteViews compact, RemoteViews expanded) {
        RemoteViews compactCopy = safeCopy(compact);
        RemoteViews expandedCopy = safeCopy(expanded);
        return compactCopy == null && expandedCopy == null ? null
                : new CustomNotificationContent(compactCopy, expandedCopy);
    }

    private static RemoteViews safeCopy(RemoteViews source) {
        if (source == null) return null;
        try {
            return new RemoteViews(source);
        } catch (RuntimeException exception) {
            Log.w("PanOSP.Notification", "custom content copy failed", exception);
            return null;
        }
    }

    public boolean hasContentView() { return contentView != null; }
    public boolean hasBigContentView() { return bigContentView != null; }

    /** Fresh copies prevent UI code from mutating the notification snapshot. */
    public RemoteViews copyContentView() {
        return contentView == null ? null : new RemoteViews(contentView);
    }

    public RemoteViews copyBigContentView() {
        return bigContentView == null ? null : new RemoteViews(bigContentView);
    }
}
