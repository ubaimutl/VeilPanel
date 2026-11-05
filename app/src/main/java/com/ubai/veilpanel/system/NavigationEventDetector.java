package com.ubai.veilpanel.system;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.Locale;

/**
 * Recognizes navigation-bar button clicks emitted by SystemUI.
 *
 * View IDs are preferred. Localized content descriptions are only a fallback
 * for OEM variants that do not expose the standard AOSP IDs.
 */
public final class NavigationEventDetector {

    public boolean isNavigationButtonClick(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_CLICKED) {
            return false;
        }

        AccessibilityNodeInfo source = event.getSource();

        if (source != null) {
            String viewId = source.getViewIdResourceName();

            if (viewId != null
                    && (viewId.endsWith(":id/back")
                    || viewId.endsWith(":id/home")
                    || viewId.endsWith(":id/recent_apps")
                    || viewId.endsWith(":id/menu")
                    || viewId.endsWith(":id/overview"))) {
                return true;
            }

            if (looksLikeNavigationDescription(
                    source.getContentDescription())) {
                return true;
            }
        }

        if (looksLikeNavigationDescription(
                event.getContentDescription())) {
            return true;
        }

        List<CharSequence> texts = event.getText();

        if (texts != null) {
            for (CharSequence text : texts) {
                if (looksLikeNavigationDescription(text)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean looksLikeNavigationDescription(CharSequence value) {
        if (value == null) {
            return false;
        }

        String text = value.toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        return text.equals("back")
                || text.equals("retour")
                || text.equals("atrás")
                || text.equals("home")
                || text.equals("accueil")
                || text.equals("inicio")
                || text.contains("recent")
                || text.contains("récent")
                || text.contains("recient")
                || text.contains("overview")
                || text.contains("aperçu");
    }
}
