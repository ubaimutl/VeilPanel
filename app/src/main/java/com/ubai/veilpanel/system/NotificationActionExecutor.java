package com.ubai.veilpanel.system;

import android.app.ActivityOptions;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/** Platform policy and execution for simple notification actions; no UI or repository dependency. */
public final class NotificationActionExecutor {
    public enum Result { SENT, SENT_ACTIVITY, UNSUPPORTED, LOCKED, CANCELED, FAILED }

    private final Context context;

    public NotificationActionExecutor(Context context) { this.context = context; }

    public static boolean isSupported(Notification.Action action) {
        if (action == null || action.actionIntent == null || !hasText(action.title)) return false;
        return (action.getRemoteInputs() == null || action.getRemoteInputs().length == 0)
                && (action.getDataOnlyRemoteInputs() == null || action.getDataOnlyRemoteInputs().length == 0);
    }

    public static boolean supportsReply(Notification.Action action) {
        if (action == null || action.actionIntent == null || !hasText(action.title)
                || action.actionIntent.isImmutable() || action.getRemoteInputs() == null) return false;
        for (RemoteInput input : action.getRemoteInputs()) if (input.getAllowFreeFormInput()) return true;
        return false;
    }

    public Result reply(Notification.Action action, CharSequence text) {
        if (!supportsReply(action) || !hasText(text)) return Result.UNSUPPORTED;
        KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
        if (keyguard == null || keyguard.isDeviceLocked()) return Result.LOCKED;
        Intent fill = new Intent();
        Bundle results = new Bundle();
        for (RemoteInput input : action.getRemoteInputs())
            if (input.getAllowFreeFormInput()) results.putCharSequence(input.getResultKey(), text);
        RemoteInput.addResultsToIntent(action.getRemoteInputs(), fill, results);
        RemoteInput.setResultsSource(fill, RemoteInput.SOURCE_FREE_FORM_INPUT);
        try {
            action.actionIntent.send(context, 0, fill);
            return Result.SENT;
        } catch (PendingIntent.CanceledException error) { return Result.CANCELED; }
        catch (RuntimeException error) { return Result.FAILED; }
    }

    private static boolean hasText(CharSequence text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c) && !Character.isSpaceChar(c)) return true;
        }
        return false;
    }

    public Result execute(Notification.Action action) {
        if (!isSupported(action)) return Result.UNSUPPORTED;
        try {
            if (action.isAuthenticationRequired()) {
                KeyguardManager keyguard = context.getSystemService(KeyguardManager.class);
                // Fail closed; this limited host does not implement a keyguard dismissal flow.
                if (keyguard == null || keyguard.isDeviceLocked()) return Result.LOCKED;
            }
            ActivityOptions options = ActivityOptions.makeBasic();
            if (Build.VERSION.SDK_INT >= 36) configureApi36(options);
            else if (Build.VERSION.SDK_INT >= 34) configureApi34(options);
            action.actionIntent.send(context, 0, null, null, null, null, options.toBundle());
            return action.actionIntent.isActivity() ? Result.SENT_ACTIVITY : Result.SENT;
        } catch (PendingIntent.CanceledException exception) {
            Log.w("PanOSP.Notification", "notification action canceled", exception);
            return Result.CANCELED;
        } catch (RuntimeException exception) {
            Log.w("PanOSP.Notification", "notification action failed", exception);
            return Result.FAILED;
        }
    }

    @android.annotation.SuppressLint("UseRequiresApi")
    @android.annotation.TargetApi(34)
    private static void configureApi34(ActivityOptions options) {
        options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
    }

    @android.annotation.SuppressLint("UseRequiresApi")
    @android.annotation.TargetApi(36)
    private static void configureApi36(ActivityOptions options) {
        options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE);
    }
}
