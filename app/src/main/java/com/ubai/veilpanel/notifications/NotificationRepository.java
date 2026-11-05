package com.ubai.veilpanel.notifications;

import android.service.notification.StatusBarNotification;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Process-local notification repository shared by the notification listener
 * and the accessibility overlay.
 */
public final class NotificationRepository {

    private static final String TAG = "PanOSP.Notification";

    public interface Listener {
        void onNotificationsChanged();
    }

    /**
     * System-side notification actions are provided by the live
     * NotificationListenerService. The UI never talks to the service directly.
     */
    public interface ActionHandler {
        void dismissNotification(String key);
        void refreshNotifications();
    }

    private static final NotificationRepository INSTANCE =
            new NotificationRepository();

    private final Map<String, NotificationEntry> entries =
            new LinkedHashMap<>();

    private final Set<Listener> listeners =
            new CopyOnWriteArraySet<>();

    private volatile ActionHandler actionHandler;

    private NotificationRepository() {
    }

    public boolean isConnected() { return actionHandler != null; }

    public static NotificationRepository getInstance() {
        return INSTANCE;
    }

    public synchronized void replaceAll(
            StatusBarNotification[] notifications) {

        replaceAll(notifications, null);
    }

    public synchronized void replaceAll(StatusBarNotification[] notifications, Context context) {
        replaceAll(notifications,context,null);
    }
    public synchronized void replaceAll(StatusBarNotification[] notifications, Context context, android.service.notification.NotificationListenerService.RankingMap rankings) {
        entries.clear();

        if (notifications != null) {
            for (StatusBarNotification notification : notifications) {
                if (notification != null) {
                    NotificationEntry entry =
                            NotificationEntry.from(notification, context);

                    rank(entry,rankings);
                    entries.put(entry.getKey(), entry);
                }
            }
        }

        Log.i(TAG, "repository replaceAll count=" + entries.size());
        notifyListeners();
    }

    public synchronized void put(
            StatusBarNotification notification) {

        put(notification, null);
    }

    public synchronized void put(StatusBarNotification notification, Context context) { put(notification,context,null); }
    public synchronized void put(StatusBarNotification notification, Context context, android.service.notification.NotificationListenerService.RankingMap rankings) {
        if (notification == null) {
            return;
        }

        NotificationEntry entry =
                NotificationEntry.from(notification, context);

        rank(entry,rankings);
        entries.put(entry.getKey(), entry);

        Log.i(
                TAG,
                "repository put count=" + entries.size()
        );

        notifyListeners();
    }

    private static void rank(NotificationEntry entry,android.service.notification.NotificationListenerService.RankingMap rankings) {
        if(rankings==null)return;
        android.service.notification.NotificationListenerService.Ranking ranking=new android.service.notification.NotificationListenerService.Ranking();
        if(rankings.getRanking(entry.getKey(),ranking)) entry.updateSilent(isSilentRanking(ranking.getImportance(),ranking.isAmbient()));
    }
    public static boolean isSilentRanking(int importance,boolean ambient) {
        return ambient || (importance!=android.app.NotificationManager.IMPORTANCE_UNSPECIFIED && importance<android.app.NotificationManager.IMPORTANCE_DEFAULT);
    }
    public synchronized void updateRanking(android.service.notification.NotificationListenerService.RankingMap rankings) {
        boolean changed=false;
        for(NotificationEntry entry:entries.values()) { boolean old=entry.isSilent();rank(entry,rankings);changed|=old!=entry.isSilent(); }
        if(changed)notifyListeners();
    }
    public synchronized void remove(String key) {
        if (key == null) {
            return;
        }

        boolean removed = entries.remove(key) != null;

        Log.i(
                TAG,
                "repository remove removed=" + removed
                        + " count=" + entries.size()
        );

        notifyListeners();
    }

    public synchronized List<NotificationEntry> snapshot() {
        List<NotificationEntry> result =
                new ArrayList<>(entries.values());

        result.sort(
                Comparator.comparingLong(
                        NotificationEntry::getPostTime
                ).reversed()
        );

        return Collections.unmodifiableList(result);
    }

    public void addListener(Listener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void setActionHandler(ActionHandler actionHandler) {
        this.actionHandler = actionHandler;
        Log.i(TAG, "notification actions available");
    }

    public void clearActionHandler(ActionHandler actionHandler) {
        if (this.actionHandler == actionHandler) {
            this.actionHandler = null;
            Log.i(TAG, "notification actions unavailable");
        }
    }

    /** Requests a fresh snapshot from Android through the connected listener. */
    public void requestRefresh() {
        ActionHandler handler = actionHandler;

        if (handler == null) {
            Log.w(TAG, "refresh unavailable, listener not connected");
            return;
        }

        handler.refreshNotifications();
    }

    /**
     * Requests a real system dismissal. The repository itself is not modified:
     * Android remains the source of truth and onNotificationRemoved() performs
     * the actual local removal.
     */
    public boolean requestDismiss(String key) {
        if (key == null) {
            return false;
        }

        synchronized (this) {
            NotificationEntry entry = entries.get(key);

            if (entry == null) {
                Log.w(TAG, "dismiss ignored, unknown");
                return false;
            }

            if (!entry.isClearable()) {
                Log.i(TAG, "dismiss blocked, not clearable");
                return false;
            }
        }

        ActionHandler handler = actionHandler;

        if (handler == null) {
            Log.w(TAG, "dismiss unavailable, listener not connected");
            return false;
        }

        handler.dismissNotification(key);
        return true;
    }

    private void notifyListeners() {
        for (Listener listener : listeners) {
            listener.onNotificationsChanged();
        }
    }
}
