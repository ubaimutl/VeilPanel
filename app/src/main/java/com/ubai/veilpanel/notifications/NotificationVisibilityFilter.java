package com.ubai.veilpanel.notifications;

import android.app.Notification;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides which notifications should be exposed in the PanOSP shade.
 *
 * OEM-specific quirks live here so filtering remains explicit, reviewable and
 * separate from the notification UI.
 */
public final class NotificationVisibilityFilter {

    private static final String XIAOMI_SECURITY_CENTER =
            "com.miui.securitycenter";

    private static final String XIAOMI_RESIDENT_HIDDEN_CHANNEL =
            "securitycenter_resident_notification_hide";

    private static final String XIAOMI_SCREENSHOT =
            "com.miui.screenshot";

    private static final String XIAOMI_SCREENSHOT_FGS_CHANNEL =
            "SCN";


    private NotificationVisibilityFilter() {
    }

    /**
     * Hides summaries represented by visible children, without removing them
     * from the repository. Entries must already pass the individual filters.
     */
    public static List<NotificationEntry> filterForDisplay(
            List<NotificationEntry> entries) {
        Map<Integer, Set<String>> groupsWithChildren = new HashMap<>();

        for (NotificationEntry entry : entries) {
            if (entry.isGrouped()
                    && !entry.isGroupSummary()
                    && entry.getGroupKey() != null) {
                groupsWithChildren.computeIfAbsent(
                        entry.getUserId(), userId -> new HashSet<>()
                ).add(entry.getGroupKey());
            }
        }

        List<NotificationEntry> visible = new ArrayList<>(entries.size());
        for (NotificationEntry entry : entries) {
            Set<String> groups = groupsWithChildren.get(entry.getUserId());
            if (entry.isGrouped()
                    && entry.isGroupSummary()
                    && entry.getGroupKey() != null
                    && groups != null
                    && groups.contains(entry.getGroupKey())) {
                continue;
            }
            visible.add(entry);
        }
        return visible;
    }

    public static boolean shouldDisplay(StatusBarNotification sbn) {
        if (sbn == null) {
            return false;
        }

        Notification notification = sbn.getNotification();

        if (notification == null) {
            return false;
        }

        // =====================================================================
        // XIAOMI / HYPEROS QUIRKS
        // =====================================================================

        /*
         * Security Center resident foreground-service notification.
         * HyperOS SystemUI intentionally hides it from the user-facing shade.
         */
        if (XIAOMI_SECURITY_CENTER.equals(sbn.getPackageName())
                && XIAOMI_RESIDENT_HIDDEN_CHANNEL.equals(
                notification.getChannelId())) {
            return false;
        }

        /*
         * Android/HyperOS can create an automatic group-summary notification
         * for the resident Security Center notification above. The generated
         * summary has id 0 and FLAG_GROUP_SUMMARY, and should be hidden as
         * well. Restricting the check to this exact shape keeps genuine Xiaomi
         * Security Center alerts visible.
         */
        if (XIAOMI_SECURITY_CENTER.equals(sbn.getPackageName())
                && sbn.getId() == 0
                && (notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) {
            return false;
        }

        /*
         * Temporary screenshot foreground-service notification.
         * HyperOS masks it in its own shade; PanOSP does the same.
         */
        if (isXiaomiScreenshotServiceNotification(sbn)) {
            return false;
        }

        return !isEmptyForegroundService(notification);
    }

    /** Conservative display-only rule; unreadable or unfamiliar content stays visible. */
    static boolean isEmptyForegroundService(Notification notification) {
        try {
            if (notification == null
                    || (notification.flags & Notification.FLAG_FOREGROUND_SERVICE) == 0
                    || (notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0
                    || notification.extras == null
                    || notification.category != null
                    || notification.publicVersion != null
                    || notification.getShortcutId() != null
                    || notification.getBubbleMetadata() != null
                    || !isEmptyText(notification.tickerText)
                    || (notification.actions != null && notification.actions.length > 0)
                    || notification.contentIntent != null
                    || notification.deleteIntent != null
                    || notification.fullScreenIntent != null
                    || notification.contentView != null
                    || notification.bigContentView != null
                    || notification.headsUpContentView != null) {
                return false;
            }
            Bundle extras = notification.extras;
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                if (value == null) {
                    continue;
                }
                switch (key) {
                    case Notification.EXTRA_TITLE:
                    case Notification.EXTRA_TEXT:
                    case Notification.EXTRA_SUB_TEXT:
                    case Notification.EXTRA_TITLE_BIG:
                    case Notification.EXTRA_BIG_TEXT:
                    case Notification.EXTRA_SUMMARY_TEXT:
                    case Notification.EXTRA_INFO_TEXT:
                        if (!(value instanceof CharSequence)
                                || !isEmptyText((CharSequence) value)) {
                            return false;
                        }
                        break;
                    case Notification.EXTRA_PROGRESS:
                    case Notification.EXTRA_PROGRESS_MAX:
                        if (!(value instanceof Integer) || ((Integer) value) != 0) {
                            return false;
                        }
                        break;
                    case Notification.EXTRA_PROGRESS_INDETERMINATE:
                    case Notification.EXTRA_SHOW_CHRONOMETER:
                        if (!(value instanceof Boolean) || (Boolean) value) {
                            return false;
                        }
                        break;
                    // Framework presentation metadata is not user content.
                    case Notification.EXTRA_SHOW_WHEN:
                    case "android.reduced.images":
                        if (!(value instanceof Boolean)) {
                            return false;
                        }
                        break;
                    case "android.appInfo":
                        if (!(value instanceof android.content.pm.ApplicationInfo)) {
                            return false;
                        }
                        break;
                    default:
                        // Includes styles, media sessions, messages, people, images,
                        // custom/OEM payloads and future Android content fields.
                        return false;
                }
            }
            return true;
        } catch (RuntimeException unreadableExtras) {
            return false;
        }
    }

    private static boolean isEmptyText(CharSequence text) {
        if (text == null) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Identifies the confirmed HyperOS screenshot foreground-service
     * notification. It is hidden as a real notification but also acts as a
     * reliable event signal for PanOSP's temporary screenshot confirmation.
     */
    public static boolean isXiaomiScreenshotServiceNotification(
            StatusBarNotification sbn) {

        if (sbn == null || sbn.getNotification() == null) {
            return false;
        }

        return XIAOMI_SCREENSHOT.equals(sbn.getPackageName())
                && XIAOMI_SCREENSHOT_FGS_CHANNEL.equals(
                sbn.getNotification().getChannelId()
        );
    }
}
