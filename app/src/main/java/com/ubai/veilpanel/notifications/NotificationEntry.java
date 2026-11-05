package com.ubai.veilpanel.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.os.Process;
import android.util.Log;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.service.notification.StatusBarNotification;

/**
 * UI-safe snapshot of the notification fields PanOSP currently displays.
 */
public final class NotificationEntry {

    private final String key;
    private final String packageName;
    private final CharSequence title;
    private final CharSequence text;
    private final CharSequence applicationLabel;
    private final CharSequence titleBig;
    private final CharSequence bigText;
    private final CharSequence subText;
    private final CharSequence infoText;
    private final CharSequence tickerText;
    private final CustomNotificationContent customContent;
    private final String channelId;
    private final List<Notification.Action> actions;
    private final long postTime;
    private final PendingIntent contentIntent;
    private final android.graphics.drawable.Icon smallIcon;
    private final boolean clearable;
    private boolean silent;
    public boolean isSilent() { return silent; }
    void updateSilent(boolean value) { silent=value; }
    private final boolean autoCancel;
    private final String groupKey;
    private final boolean grouped;
    private final boolean groupSummary;
    private final int userId;
    private final android.os.UserHandle user;

    private NotificationEntry(
            String key,
            String packageName,
            CharSequence title,
            CharSequence text,
            CharSequence applicationLabel,
            CharSequence titleBig,
            CharSequence bigText,
            CharSequence subText,
            CharSequence infoText,
            CharSequence tickerText,
            CustomNotificationContent customContent,
            String channelId,
            List<Notification.Action> actions,
            long postTime,
            PendingIntent contentIntent,
            boolean clearable,
            boolean autoCancel,
            String groupKey,
            boolean grouped,
            boolean groupSummary,
            int userId, android.os.UserHandle user, android.graphics.drawable.Icon smallIcon) {
        this.smallIcon = smallIcon;
        this.key = key;
        this.packageName = packageName;
        this.title = title;
        this.text = text;
        this.applicationLabel = applicationLabel;
        this.titleBig = titleBig;
        this.bigText = bigText;
        this.subText = subText;
        this.infoText = infoText;
        this.tickerText = tickerText;
        this.customContent = customContent;
        this.channelId = channelId;
        this.actions = actions;
        this.postTime = postTime;
        this.contentIntent = contentIntent;
        this.clearable = clearable;
        this.autoCancel = autoCancel;
        this.groupKey = groupKey;
        this.grouped = grouped;
        this.groupSummary = groupSummary;
        this.userId = userId;
        this.user = user;
    }

    public static NotificationEntry from(
            StatusBarNotification sbn) {

        return from(sbn, null);
    }

    public static NotificationEntry from(StatusBarNotification sbn, Context context) {
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;

        CharSequence title =
                readText(extras, Notification.EXTRA_TITLE);

        CharSequence text =
                readText(extras, Notification.EXTRA_TEXT);

        boolean autoCancel =
                (notification.flags & Notification.FLAG_AUTO_CANCEL) != 0;

        return new NotificationEntry(
                sbn.getKey(),
                sbn.getPackageName(),
                title,
                text,
                resolveApplicationLabel(sbn, context),
                readText(extras, Notification.EXTRA_TITLE_BIG),
                readText(extras, Notification.EXTRA_BIG_TEXT),
                readText(extras, Notification.EXTRA_SUB_TEXT),
                readText(extras, Notification.EXTRA_INFO_TEXT),
                notification.tickerText,
                CustomNotificationContent.from(notification.contentView, notification.bigContentView),
                notification.getChannelId(),
                copyActions(notification.actions),
                sbn.getPostTime(),
                notification.contentIntent,
                sbn.isClearable(),
                autoCancel,
                sbn.getGroupKey(),
                sbn.isGroup(),
                (notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0,
                sbn.getUserId(), sbn.getUser(), notification.getSmallIcon()
        );
    }

    private static List<Notification.Action> copyActions(Notification.Action[] source) {
        List<Notification.Action> result = new ArrayList<>();
        if (source != null) {
            for (Notification.Action action : source) {
                if (action == null) continue;
                try {
                    result.add(action.clone());
                } catch (RuntimeException exception) {
                    Log.w("PanOSP.Notification", "unreadable notification action", exception);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    public String getChannelId() { return channelId; }

    /** Return copies so callers cannot mutate the snapshot's action metadata. */
    public List<Notification.Action> getActions() {
        return copyActions(actions.toArray(new Notification.Action[0]));
    }

    private static CharSequence readText(Bundle extras, String key) {
        try {
            return extras != null ? extras.getCharSequence(key) : null;
        } catch (RuntimeException exception) {
            Log.w("PanOSP.Notification", "unreadable text extra=" + key, exception);
            return null;
        }
    }

    private static CharSequence resolveApplicationLabel(StatusBarNotification sbn, Context context) {
        if (context == null) return null;
        try {
            ApplicationInfo appInfo;
            if (sbn.getUserId() < 0 || sbn.getUser().equals(Process.myUserHandle())) {
                appInfo = context.getPackageManager().getApplicationInfo(sbn.getPackageName(), 0);
            } else {
                LauncherApps launcherApps = context.getSystemService(LauncherApps.class);
                if (launcherApps == null) return null;
                appInfo = launcherApps.getApplicationInfo(sbn.getPackageName(), 0, sbn.getUser());
            }
            return context.getPackageManager().getApplicationLabel(appInfo);
        } catch (android.content.pm.PackageManager.NameNotFoundException | RuntimeException exception) {
            Log.w("PanOSP.Notification", "application label unavailable");
            return null;
        }
    }

    public android.graphics.drawable.Drawable icon(Context context) {
        try { return context.getPackageManager().getApplicationIcon(packageName); }
        catch(android.content.pm.PackageManager.NameNotFoundException | RuntimeException unavailable) {
            try { return smallIcon != null ? smallIcon.loadDrawable(context) : null; }
            catch(RuntimeException missing) { return null; }
        }
    }
    public CharSequence getApplicationLabel() { return applicationLabel; }
    public CharSequence getTitleBig() { return titleBig; }
    public CharSequence getBigText() { return bigText; }
    public CharSequence getSubText() { return subText; }
    public CharSequence getInfoText() { return infoText; }
    public CharSequence getTickerText() { return tickerText; }
    public CustomNotificationContent getCustomContent() { return customContent; }

    public String getKey() {
        return key;
    }

    public String getPackageName() {
        return packageName;
    }

    public CharSequence getTitle() {
        return title;
    }

    public CharSequence getText() {
        return text;
    }

    public long getPostTime() {
        return postTime;
    }

    public PendingIntent getContentIntent() {
        return contentIntent;
    }

    public boolean isClearable() {
        return clearable;
    }

    public boolean isAutoCancel() {
        return autoCancel;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public boolean isGrouped() {
        return grouped;
    }

    public boolean isGroupSummary() {
        return groupSummary;
    }

    public android.os.UserHandle getUser() { return user; }

    public int getUserId() {
        return userId;
    }
}
