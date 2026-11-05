package com.ubai.veilpanel.notifications;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Receives the real system notification stream for the PanOSP shade.
 */
public final class PanOSPNotificationListener
        extends NotificationListenerService {

    private static final String TAG = "PanOSP.Notification";

    private final NotificationRepository repository =
            NotificationRepository.getInstance();

    private final SystemFeedbackRepository feedbackRepository =
            SystemFeedbackRepository.getInstance();

    private final NotificationRepository.ActionHandler actionHandler =
            new NotificationRepository.ActionHandler() {
                @Override
                public void dismissNotification(String key) {
                    dismissNotificationFromUi(key);
                }

                @Override
                public void refreshNotifications() {
                    refreshActiveNotifications("shade shown");
                }
            };

    private boolean listenerConnected;

    public static void reconnectIfNeeded(android.content.Context context) {
        if (NotificationRepository.getInstance().isConnected()) return;
        android.content.ComponentName component=new android.content.ComponentName(context,PanOSPNotificationListener.class);
        android.app.NotificationManager manager=context.getSystemService(android.app.NotificationManager.class);
        try {
            if (manager != null && manager.isNotificationListenerAccessGranted(component)) requestRebind(component);
        } catch(RuntimeException unavailable) { android.util.Log.w(TAG,"Notification listener reconnection unavailable"); }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();

        listenerConnected = true;
        repository.setActionHandler(actionHandler);

        Log.i(TAG, "listener connected");

        refreshActiveNotifications("listener connected");
    }

    private void refreshActiveNotifications(String reason) {
        if (!listenerConnected) {
            Log.w(TAG, "refresh rejected, listener unavailable");
            return;
        }

        Log.i(TAG, "refresh active notifications reason=" + reason);

        try {
            StatusBarNotification[] activeNotifications =
                    getActiveNotifications();

            if (activeNotifications == null) {
                Log.w(TAG, "active notifications unavailable, repository unchanged");
                return;
            }

            StatusBarNotification[] visibleNotifications =
                    filterVisible(activeNotifications);

            Log.i(
                    TAG,
                    "active notifications="
                            + activeNotifications.length
                            + " visible="
                            + visibleNotifications.length
            );

            repository.replaceAll(visibleNotifications, this, getCurrentRanking());

        } catch (Exception exception) {
            Log.e(
                    TAG,
                    "failed to read active notifications, repository retained",
                    exception
            );
        }
    }

    @Override
    public void onListenerDisconnected() {
        listenerConnected = false;
        repository.clearActionHandler(actionHandler);

        Log.w(TAG, "listener disconnected");

        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) {
            return;
        }

        /*
         * HyperOS' screenshot UI cannot currently draw above PanOSP's
         * accessibility overlay. The screenshot FGS notification is nevertheless
         * a reliable event telling us that the capture has just completed.
         * Convert that hidden technical notification into a short PanOSP UI
         * confirmation while keeping it out of the real notification list.
         */
        if (NotificationVisibilityFilter
                .isXiaomiScreenshotServiceNotification(sbn)) {

            Log.i(TAG, "screenshot feedback");

            feedbackRepository.emit(
                    "Capture d’écran enregistrée"
            );

            repository.remove(sbn.getKey());
            return;
        }

        if (!NotificationVisibilityFilter.shouldDisplay(sbn)) {
            Log.i(TAG, "filtered");

            repository.remove(sbn.getKey());
            return;
        }

        Log.i(TAG, "posted clearable=" + sbn.isClearable());

        repository.put(sbn, this, getCurrentRanking());
    }

    @Override public void onNotificationRankingUpdate(RankingMap rankingMap) { repository.updateRanking(rankingMap); }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) {
            return;
        }

        Log.i(TAG, "removed");

        repository.remove(sbn.getKey());
    }

    @Override
    public void onDestroy() {
        listenerConnected = false;
        repository.clearActionHandler(actionHandler);

        Log.i(TAG, "listener destroyed");

        super.onDestroy();
    }

    // =========================================================================
    // VISIBILITY
    // =========================================================================

    private StatusBarNotification[] filterVisible(
            StatusBarNotification[] notifications) {

        if (notifications == null || notifications.length == 0) {
            return new StatusBarNotification[0];
        }

        List<StatusBarNotification> visible =
                new ArrayList<>();

        for (StatusBarNotification sbn : notifications) {
            if (NotificationVisibilityFilter.shouldDisplay(sbn)) {
                visible.add(sbn);

            } else if (sbn != null) {
                Log.i(TAG, "filtered active");
            }
        }

        return visible.toArray(
                new StatusBarNotification[0]
        );
    }

    // =========================================================================
    // USER ACTIONS
    // =========================================================================

    private void dismissNotificationFromUi(String key) {
        if (!listenerConnected || key == null) {
            Log.w(TAG, "dismiss rejected, listener unavailable");
            return;
        }

        try {
            Log.i(TAG, "dismiss requested");
            cancelNotification(key);

        } catch (Exception exception) {
            Log.e(TAG, "dismiss failed", exception);
        }
    }
}
