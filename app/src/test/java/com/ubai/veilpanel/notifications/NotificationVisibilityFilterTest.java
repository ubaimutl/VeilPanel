package com.ubai.veilpanel.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.widget.RemoteViews;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.Arrays;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class NotificationVisibilityFilterTest {
    private Notification empty() {
        Notification n = new Notification();
        n.flags = Notification.FLAG_FOREGROUND_SERVICE;
        n.extras = new Bundle();
        return n;
    }
    private StatusBarNotification wrap(String pkg, Notification n) {
        return new StatusBarNotification(pkg, pkg, 42, null, 10001, 0, 0,
                n, android.os.Process.myUserHandle(), 1L);
    }
    private boolean visible(Notification n) {
        return NotificationVisibilityFilter.shouldDisplay(wrap("example.service", n));
    }
    @Test public void emptyOemServicesAreHiddenWithoutPackageOrChannelMatch() {
        for (String pkg : Arrays.asList("com.miui.misound", "com.miui.securitycenter", "example.service")) {
            String channel = pkg.equals("com.miui.misound") ? "misound_protect"
                    : pkg.equals("com.miui.securitycenter")
                    ? "securitycenter_resident_notification_hide" : "arbitrary";
            Notification n = new Notification.Builder(RuntimeEnvironment.getApplication(), channel).build();
            n.contentView = null;
            n.bigContentView = null;
            n.headsUpContentView = null;
            n.flags = Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
            n.extras = new Bundle();
            n.extras.putBoolean("android.reduced.images", true);
            n.extras.putBoolean(Notification.EXTRA_SHOW_WHEN, true);
            n.extras.putParcelable("android.appInfo", RuntimeEnvironment.getApplication().getApplicationInfo());
            assertTrue(NotificationVisibilityFilter.isEmptyForegroundService(n));
            assertFalse(NotificationVisibilityFilter.shouldDisplay(wrap(pkg, n)));
        }
    }
    @Test public void usefulNotificationsStayVisible() {
        String[] packages = {"com.ttxapps.autosync", "android", "com.android.mtp"};
        int[] flags = {Notification.FLAG_FOREGROUND_SERVICE,
                Notification.FLAG_ONGOING_EVENT, Notification.FLAG_NO_CLEAR};
        for (int i = 0; i < packages.length; i++) {
            Notification n = empty();
            n.flags = flags[i];
            n.extras.putCharSequence(Notification.EXTRA_TITLE, "Useful title");
            if (i != 2) n.extras.putCharSequence(Notification.EXTRA_TEXT, "Useful text");
            assertTrue(NotificationVisibilityFilter.shouldDisplay(wrap(packages[i], n)));
        }
    }
    @Test public void emptyNonForegroundStaysVisible() {
        Notification n = empty();
        n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        assertTrue(visible(n));
    }
    @Test public void everyTextFieldPreventsHiding() {
        for (String key : Arrays.asList(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT,
                Notification.EXTRA_SUB_TEXT, Notification.EXTRA_BIG_TEXT)) {
            Notification n = empty(); n.extras.putCharSequence(key, "content");
            assertTrue(visible(n));
        }
        Notification n = empty(); n.tickerText = "ticker"; assertTrue(visible(n));
    }
    @Test public void actionsAndEachIntentPreventHiding() {
        PendingIntent intent = PendingIntent.getActivity(RuntimeEnvironment.getApplication(),
                0, new Intent("example.TEST"), PendingIntent.FLAG_IMMUTABLE);
        for (int i = 0; i < 4; i++) {
            Notification n = empty();
            if (i == 0) n.contentIntent = intent;
            if (i == 1) n.deleteIntent = intent;
            if (i == 2) n.fullScreenIntent = intent;
            if (i == 3) n.actions = new Notification.Action[]{new Notification.Action(0, "Open", intent)};
            assertTrue(visible(n));
        }
    }
    @Test public void customViewsPreventHiding() {
        for (int i = 0; i < 3; i++) {
            Notification n = empty();
            RemoteViews view = new RemoteViews(RuntimeEnvironment.getApplication().getPackageName(), 1);
            if (i == 0) n.contentView = view;
            if (i == 1) n.bigContentView = view;
            if (i == 2) n.headsUpContentView = view;
            assertTrue(visible(n));
        }
    }
    @Test public void semanticAndAmbiguousContentStayVisible() {
        for (String category : Arrays.asList(Notification.CATEGORY_TRANSPORT,
                Notification.CATEGORY_CALL, Notification.CATEGORY_NAVIGATION,
                Notification.CATEGORY_ALARM, Notification.CATEGORY_MESSAGE, "unknown")) {
            Notification n = empty(); n.category = category; assertTrue(visible(n));
        }
        for (String key : Arrays.asList(Notification.EXTRA_MEDIA_SESSION,
                Notification.EXTRA_MESSAGES, Notification.EXTRA_TEMPLATE, "future.content")) {
            Notification n = empty(); n.extras.putString(key, "payload"); assertTrue(visible(n));
        }
        Notification n = empty(); n.extras = null; assertTrue(visible(n));
        n = empty(); n.extras.putInt(Notification.EXTRA_TITLE, 7); assertTrue(visible(n));
    }
    @Test public void progressAndChronometerStayVisible() {
        for (String key : Arrays.asList(Notification.EXTRA_PROGRESS, Notification.EXTRA_PROGRESS_MAX)) {
            Notification n = empty(); n.extras.putInt(key, 1); assertTrue(visible(n));
        }
        for (String key : Arrays.asList(Notification.EXTRA_PROGRESS_INDETERMINATE,
                Notification.EXTRA_SHOW_CHRONOMETER)) {
            Notification n = empty(); n.extras.putBoolean(key, true); assertTrue(visible(n));
        }
    }
    @Test public void summariesRemainSeparateAndChildrenControlDisplay() {
        Notification summary = empty();
        summary.flags |= Notification.FLAG_GROUP_SUMMARY;
        assertTrue(visible(summary));
        Notification child = new Notification.Builder(RuntimeEnvironment.getApplication(), "test")
                .setGroup("group").setContentTitle("child").build();
        Notification groupedSummary = new Notification.Builder(RuntimeEnvironment.getApplication(), "test")
                .setGroup("group").setGroupSummary(true).build();
        NotificationEntry s = NotificationEntry.from(wrap("example.service", groupedSummary));
        NotificationEntry c = NotificationEntry.from(wrap("example.service", child));
        assertEquals(Arrays.asList(s), NotificationVisibilityFilter.filterForDisplay(Arrays.asList(s)));
        assertEquals(Arrays.asList(c), NotificationVisibilityFilter.filterForDisplay(Arrays.asList(s, c)));
    }
}
