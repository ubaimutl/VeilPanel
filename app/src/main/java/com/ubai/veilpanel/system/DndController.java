package com.ubai.veilpanel.system;

import android.app.NotificationManager;
import android.content.Context;

/**
 * Public Android integration for Do Not Disturb.
 *
 * Android 15+ no longer lets normal apps directly replace the global DND
 * filter. With Notification Policy access, setInterruptionFilter() instead
 * controls an app-owned implicit AutomaticZenRule. This is still the official
 * public API and contributes to the device's effective DND state.
 *
 * If another rule/user mode keeps DND active, asking PanOSP to turn its own
 * rule off may legitimately leave the global DND state active.
 */
public final class DndController {

    private final NotificationManager notificationManager;

    public DndController(Context context) {
        notificationManager =
                context.getSystemService(NotificationManager.class);
    }

    public boolean isAvailable() {
        return notificationManager != null;
    }

    public boolean hasPolicyAccess() {
        return notificationManager != null
                && notificationManager
                .isNotificationPolicyAccessGranted();
    }

    public boolean isEnabled() {
        if (notificationManager == null) {
            return false;
        }

        int filter =
                notificationManager
                        .getCurrentInterruptionFilter();

        return filter
                != NotificationManager.INTERRUPTION_FILTER_ALL
                && filter
                != NotificationManager.INTERRUPTION_FILTER_UNKNOWN;
    }

    public boolean toggle() {
        if (!hasPolicyAccess()) {
            return false;
        }

        int target =
                isEnabled()
                        ? NotificationManager.INTERRUPTION_FILTER_ALL
                        : NotificationManager.INTERRUPTION_FILTER_PRIORITY;

        try {
            notificationManager.setInterruptionFilter(target);
            return true;

        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
