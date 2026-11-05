package com.ubai.veilpanel.notifications;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Process-local stream for short-lived system feedback that PanOSP must show
 * while its shade is open.
 *
 * Events are NOT persisted. If the shade is closed, the event is simply
 * ignored. This prevents an old screenshot confirmation from appearing later.
 */
public final class SystemFeedbackRepository {

    public interface Listener {
        void onSystemFeedback(String message);
    }

    private static final SystemFeedbackRepository INSTANCE =
            new SystemFeedbackRepository();

    private final Set<Listener> listeners =
            new CopyOnWriteArraySet<>();

    private SystemFeedbackRepository() {
    }

    public static SystemFeedbackRepository getInstance() {
        return INSTANCE;
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

    public void emit(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        for (Listener listener : listeners) {
            listener.onSystemFeedback(message);
        }
    }
}
