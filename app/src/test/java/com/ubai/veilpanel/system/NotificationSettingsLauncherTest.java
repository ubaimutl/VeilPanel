package com.ubai.veilpanel.system;

import android.content.ActivityNotFoundException;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class NotificationSettingsLauncherTest {
    private static final class CaptureContext extends ContextWrapper {
        final List<Intent> intents = new ArrayList<>();
        final List<String> reject = new ArrayList<>();
        boolean closed;
        CaptureContext() { super(RuntimeEnvironment.getApplication()); }
        @Override public void startActivity(Intent intent) {
            assertTrue("overlay must close before launch", closed);
            intents.add(intent);
            if (reject.contains(intent.getAction())) throw new ActivityNotFoundException();
        }
    }
    @Test public void channelLaunchIncludesApplicationAndChannel() {
        CaptureContext context = new CaptureContext();
        SettingsLauncher launcher = new SettingsLauncher(context, () -> context.closed = true);
        assertTrue(launcher.openNotificationSettings("example.clock", "timer", 0, Process.myUserHandle()));
        Intent intent = context.intents.get(0);
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS, intent.getAction());
        assertEquals("example.clock", intent.getStringExtra(Settings.EXTRA_APP_PACKAGE));
        assertEquals("timer", intent.getStringExtra(Settings.EXTRA_CHANNEL_ID));
        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
    }
    @Test public void unavailableChannelAndApplicationScreensFallBackToApplicationDetails() {
        CaptureContext context = new CaptureContext();
        context.reject.add(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
        context.reject.add(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        SettingsLauncher launcher = new SettingsLauncher(context, () -> context.closed = true);
        assertTrue(launcher.openNotificationSettings("example.clock", "timer", 0, Process.myUserHandle()));
        assertEquals(3, context.intents.size());
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, context.intents.get(1).getAction());
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, context.intents.get(2).getAction());
        assertEquals("package:example.clock", context.intents.get(2).getData().toString());
    }
    @Test public void foreignProfileNeverOpensSamePackageInCurrentProfile() {
        CaptureContext context = new CaptureContext();
        SettingsLauncher launcher = new SettingsLauncher(context, () -> context.closed = true);
        assertFalse(launcher.openNotificationSettings("example.clock", "timer", 10, UserHandle.getUserHandleForUid(1010001)));
        assertTrue(context.intents.isEmpty());
        assertFalse(context.closed);
    }
}
