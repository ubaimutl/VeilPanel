package com.ubai.veilpanel.notifications;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class CustomNotificationContentTest {
    @Test public void snapshotSurvivesChangesToSourceAndReturnedCopy() {
        RemoteViews source = new RemoteViews("android", android.R.layout.simple_list_item_1);
        source.setTextViewText(android.R.id.text1, "original");
        CustomNotificationContent snapshot = CustomNotificationContent.from(source);
        assertNotNull(snapshot);
        source.setTextViewText(android.R.id.text1, "source changed");
        RemoteViews uiCopy = snapshot.copyContentView();
        uiCopy.setTextViewText(android.R.id.text1, "UI changed");
        FrameLayout parent = new FrameLayout(RuntimeEnvironment.getApplication());
        TextView rendered = snapshot.copyContentView()
                .apply(RuntimeEnvironment.getApplication(), parent).findViewById(android.R.id.text1);
        assertEquals("original", rendered.getText().toString());
    }

    @Test public void customOnlyNotificationKeepsItsContentAndIdentity() {
        Notification notification = new Notification();
        notification.extras = new Bundle();
        notification.contentView = new RemoteViews("android", android.R.layout.simple_list_item_1);
        notification.contentView.setTextViewText(android.R.id.text1, "custom timer");
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        StatusBarNotification sbn = new StatusBarNotification("example.clock", "example.clock",
                42, null, 10001, 0, 0, notification, android.os.Process.myUserHandle(), 1L);
        NotificationEntry entry = NotificationEntry.from(sbn, RuntimeEnvironment.getApplication());
        assertEquals(sbn.getKey(), entry.getKey());
        assertNull(entry.getTitle());
        assertNull(entry.getText());
        assertNotNull(entry.getCustomContent());
        assertEquals("example.clock", entry.getPackageName());
        assertNull(entry.getApplicationLabel());
    }

    @Test public void expandedOnlyContentIsPreservedAndDefensivelyCopied() {
        RemoteViews source = new RemoteViews("android", android.R.layout.simple_list_item_1);
        source.setTextViewText(android.R.id.text1, "expanded");
        CustomNotificationContent content = CustomNotificationContent.from(null, source);
        assertNotNull(content);
        assertFalse(content.hasContentView());
        assertTrue(content.hasBigContentView());
        assertNull(content.copyContentView());
        source.setTextViewText(android.R.id.text1, "mutated");
        TextView text = content.copyBigContentView().apply(RuntimeEnvironment.getApplication(),
                new FrameLayout(RuntimeEnvironment.getApplication())).findViewById(android.R.id.text1);
        assertEquals("expanded", text.getText().toString());
    }

    @Test public void absentCustomViewDoesNotCreatePlaceholderContent() {
        assertNull(CustomNotificationContent.from(null));
    }
}
