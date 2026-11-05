package com.ubai.veilpanel.system;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

/**
 * Mobile-data state reader.
 *
 * Public Android APIs allow state inspection with ACCESS_NETWORK_STATE but
 * changing the user mobile-data setting requires privileges unavailable to an
 * ordinary application. Writes therefore belong in the privileged bridge.
 */
public final class MobileDataController {

    private final Context context;
    private final TelephonyManager telephonyManager;
    private final ContentObserver settingsObserver;
    private Runnable stateChangeListener;

    public MobileDataController(Context context) {
        this.context = context.getApplicationContext();
        telephonyManager =
                (TelephonyManager) this.context.getSystemService(
                        Context.TELEPHONY_SERVICE
                );
        settingsObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override public void onChange(boolean selfChange, Uri uri) {
                String key = uri == null ? null : uri.getLastPathSegment();
                if (key == null || key.startsWith("mobile_data")
                        || "multi_sim_data_call".equals(key)) {
                    Runnable listener = stateChangeListener;
                    if (listener != null) listener.run();
                }
            }
        };
    }

    public void setStateChangeListener(Runnable listener) {
        context.getContentResolver().unregisterContentObserver(settingsObserver);
        stateChangeListener = listener;
        if (listener != null) {
            context.getContentResolver().registerContentObserver(
                    Settings.Global.CONTENT_URI, true, settingsObserver);
        }
    }

    public void close() {
        setStateChangeListener(null);
    }

    public boolean isAvailable() {
        return telephonyManager != null
                && (context.getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_TELEPHONY_DATA)
                    // Older feature declarations imply mobile-data support.
                    || context.getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_TELEPHONY));
    }

    // Retained upstream controller. The notification-only app never instantiates it.
    // Runtime permission gate below remains mandatory; this app does not declare that permission.
    @android.annotation.SuppressLint("MissingPermission")
    public boolean isEnabled() {
        try {
            if (isAvailable() && context.checkSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
                int subId = SubscriptionManager.getDefaultDataSubscriptionId();
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    TelephonyManager dataManager =
                            telephonyManager.createForSubscriptionId(subId);
                    if (dataManager != null) {
                        // Unlike isDataEnabled(), this public API does not turn an
                        // unavailable telephony service into a valid-looking OFF.
                        return dataManager.isDataEnabledForReason(
                                TelephonyManager.DATA_ENABLED_REASON_USER);
                    }
                }
            }
        } catch (SecurityException ignored) {
            // The public read is not permitted; use the settings fallback.
        } catch (RuntimeException ignored) {
            // Fall back only when the subscription-specific read is unavailable.
        }

        try {
            return Settings.Global.getInt(
                    context.getContentResolver(), "mobile_data", 0) != 0;
        } catch (SecurityException
                 | IllegalStateException
                 | UnsupportedOperationException ignored) {
            return false;
        }
    }
}
