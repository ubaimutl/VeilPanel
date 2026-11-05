package com.ubai.veilpanel.system;

import android.accessibilityservice.AccessibilityService;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

/**
 * Isolates interaction with the resident SystemUI notification shade.
 *
 * PanOSP does not replace SystemUI at the framework level. The accessibility
 * overlay receives the top-edge gesture and Android's public global action is
 * used to dismiss the OEM shade if SystemUI attempts to expand underneath it.
 */
public final class SystemShadeController {

    public static final String SYSTEM_UI_PACKAGE = "com.android.systemui";

    private static final long MIN_DISMISS_INTERVAL_MS = 40L;

    private final AccessibilityService service;

    private long lastDismissAt;
    private String systemShadeDescription;

    public SystemShadeController(AccessibilityService service) {
        this.service = service;
        loadSystemShadeDescription();
    }

    public boolean isSystemUiEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        return packageName != null
                && SYSTEM_UI_PACKAGE.contentEquals(packageName);
    }

    /** Registered by SystemUI while its shade can be dismissed. No view-tree access. */
    public boolean hasDismissAction() {
        List<android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction> actions=service.getSystemActions();
        if(actions!=null)for(var action:actions)if(action.getId()==AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)return true;
        return false;
    }

    public boolean shouldDismissSystemShade(AccessibilityEvent event) {
        if (matchesShadeDescription(event)) {
            return true;
        }

        int type = event.getEventType();

        /*
         * Some OEM SystemUI builds do not expose the AOSP notification-shade
         * accessibility description consistently. Window/content changes are
         * therefore kept as a compatibility fallback while PanOSP is open.
         */
        return type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
    }

    // Diagnostics are opt-in through the Android log tag; no notification contents.
    public void dismissSystemShade() {
        dismissSystemShade("unspecified");
    }

    public void dismissSystemShade(String debugOrigin) {
        long now = SystemClock.uptimeMillis();

        if (Log.isLoggable("VEIL_SHADE_DEBUG", Log.DEBUG)) Log.d("VEIL_SHADE_DEBUG", "uptime=" + now + " dismiss request origin=" + debugOrigin
                + " controller=" + System.identityHashCode(this)
                + " lastDismissAt=" + lastDismissAt + " interval=" + (now - lastDismissAt)
                + " decision=" + (now - lastDismissAt < MIN_DISMISS_INTERVAL_MS ? "ignored-40ms" : "execute"));
        if (now - lastDismissAt < MIN_DISMISS_INTERVAL_MS) {
            return;
        }

        boolean result = service.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE
        );

        if (result) {
            lastDismissAt = now;
        }
        if (Log.isLoggable("VEIL_SHADE_DEBUG", Log.DEBUG)) Log.d("VEIL_SHADE_DEBUG", "uptime=" + SystemClock.uptimeMillis()
                + " dismiss result=" + result + " requestUptime=" + now + " origin=" + debugOrigin);
    }

    private boolean matchesShadeDescription(AccessibilityEvent event) {
        if (systemShadeDescription == null) {
            return false;
        }

        List<CharSequence> texts = event.getText();

        if (texts != null) {
            for (CharSequence text : texts) {
                if (text != null
                        && systemShadeDescription.contentEquals(text)) {
                    return true;
                }
            }
        }

        CharSequence description = event.getContentDescription();

        return description != null
                && systemShadeDescription.contentEquals(description);
    }

    private void loadSystemShadeDescription() {
        try {
            android.content.res.Resources resources =
                    service.getPackageManager()
                            .getResourcesForApplication(SYSTEM_UI_PACKAGE);

            int id = resources.getIdentifier(
                    "accessibility_desc_notification_shade",
                    "string",
                    SYSTEM_UI_PACKAGE
            );

            if (id != 0) {
                systemShadeDescription = resources.getString(id);
            }

        } catch (Exception ignored) {
            systemShadeDescription = null;
        }
    }
}
