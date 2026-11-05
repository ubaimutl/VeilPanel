package com.ubai.veilpanel.system;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class NotificationActionExecutorTest {
    private final Context context = RuntimeEnvironment.getApplication();
    private PendingIntent intent(int id) {
        return PendingIntent.getBroadcast(context, id, new Intent("example.action." + id), PendingIntent.FLAG_IMMUTABLE);
    }
    private Notification.Action action(PendingIntent intent) {
        return new Notification.Action.Builder(null, "Pause", intent).build();
    }

    private Notification.Action mutableReply() {
        PendingIntent intent=PendingIntent.getBroadcast(context,99,new Intent("example.REPLY").setPackage(context.getPackageName()),PendingIntent.FLAG_MUTABLE);
        return new Notification.Action.Builder(null,"Reply",intent).addRemoteInput(new RemoteInput.Builder("message").build()).build();
    }
    @Test public void replyCarriesTypedTextToTheOriginalAction() {
        shadowOf(context.getSystemService(KeyguardManager.class)).setIsDeviceLocked(false);
        assertEquals(NotificationActionExecutor.Result.SENT,new NotificationActionExecutor(context).reply(mutableReply(),"hello"));
        Intent sent=shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents().get(0);
        assertEquals("hello",RemoteInput.getResultsFromIntent(sent).getCharSequence("message"));
    }
    @Test public void blankAndLockedRepliesAreNotSent() {
        Notification.Action reply=mutableReply();
        assertEquals(NotificationActionExecutor.Result.UNSUPPORTED,new NotificationActionExecutor(context).reply(reply,"  "));
        shadowOf(context.getSystemService(KeyguardManager.class)).setIsDeviceLocked(true);
        assertEquals(NotificationActionExecutor.Result.LOCKED,new NotificationActionExecutor(context).reply(reply,"hello"));
        assertTrue(shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents().isEmpty());
    }
    @Test public void immutablePendingIntentCannotCarryReplyText() {
        Notification.Action reply=new Notification.Action.Builder(null,"Reply",intent(66)).addRemoteInput(new RemoteInput.Builder("message").build()).build();
        assertFalse(NotificationActionExecutor.supportsReply(reply));
    }
    @Test public void simpleActionIsSentWithoutTouchingNotificationRepository() {
        assertEquals(NotificationActionExecutor.Result.SENT, new NotificationActionExecutor(context).execute(action(intent(1))));
        assertEquals("example.action.1", shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents().get(0).getAction());
    }
    @Test public void activityActionReportsThatTheOverlayShouldClose() {
        PendingIntent pending = PendingIntent.getActivity(context, 5, new Intent("example.activity"), PendingIntent.FLAG_IMMUTABLE);
        assertEquals(NotificationActionExecutor.Result.SENT_ACTIVITY,
                new NotificationActionExecutor(context).execute(action(pending)));
    }
    @Test public void canceledIntentDoesNotCrash() {
        PendingIntent pending = intent(2);
        pending.cancel();
        assertEquals(NotificationActionExecutor.Result.CANCELED, new NotificationActionExecutor(context).execute(action(pending)));
    }
    @Test public void replyActionsAreNotSentWithoutInput() {
        Notification.Action reply = new Notification.Action.Builder(null, "Reply", intent(3))
                .addRemoteInput(new RemoteInput.Builder("reply").build()).build();
        assertFalse(NotificationActionExecutor.isSupported(reply));
        assertEquals(NotificationActionExecutor.Result.UNSUPPORTED, new NotificationActionExecutor(context).execute(reply));
        assertTrue(shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents().isEmpty());
    }
    @Test public void authenticationRequiredActionIsBlockedWhileLocked() {
        shadowOf(context.getSystemService(KeyguardManager.class)).setIsDeviceLocked(true);
        Notification.Action authenticated = new Notification.Action.Builder(null, "Confirm", intent(4))
                .setAuthenticationRequired(true).build();
        assertEquals(NotificationActionExecutor.Result.LOCKED, new NotificationActionExecutor(context).execute(authenticated));
        assertTrue(shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents().isEmpty());
    }
}
