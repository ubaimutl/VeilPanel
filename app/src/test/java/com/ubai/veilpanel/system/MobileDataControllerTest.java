package com.ubai.veilpanel.system;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.shadows.ShadowSubscriptionManager;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class MobileDataControllerTest {
    private Context context;
    private TelephonyManager manager;
    private ShadowPackageManager packages;

    @Before public void prepare() {
        context = RuntimeEnvironment.getApplication();
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(android.Manifest.permission.ACCESS_NETWORK_STATE);
        manager = context.getSystemService(TelephonyManager.class);
        packages = shadowOf(context.getPackageManager());
        packages.setSystemFeature(PackageManager.FEATURE_TELEPHONY_DATA, false);
        packages.setSystemFeature(PackageManager.FEATURE_TELEPHONY, true);
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(3);
        shadowOf(manager).setTelephonyManagerForSubscriptionId(3, manager);
    }

    @Test public void legacyTelephonyDeclarationStillMakesDataAvailable() {
        assertTrue(new MobileDataController(context).isAvailable());
        packages.setSystemFeature(PackageManager.FEATURE_TELEPHONY, false);
        assertFalse(new MobileDataController(context).isAvailable());
        packages.setSystemFeature(PackageManager.FEATURE_TELEPHONY_DATA, true);
        assertTrue(new MobileDataController(context).isAvailable());
    }

    @Test public void selectedSubscriptionUserStateWinsIncludingValidOff() {
        Settings.Global.putInt(context.getContentResolver(), "mobile_data", 1);
        MobileDataController controller = new MobileDataController(context);
        shadowOf(manager).setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER, false);
        assertFalse(controller.isEnabled());
        shadowOf(manager).setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER, true);
        assertTrue(controller.isEnabled());
    }

    @Test public void missingDefaultSubscriptionUsesGlobalFallbackForBothStates() {
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(-1);
        MobileDataController controller = new MobileDataController(context);
        Settings.Global.putInt(context.getContentResolver(), "mobile_data", 1);
        assertTrue(controller.isEnabled());
        Settings.Global.putInt(context.getContentResolver(), "mobile_data", 0);
        assertFalse(controller.isEnabled());
    }

    @Test public void defaultSubscriptionIsReadAgainAfterSimSwitch() {
        MobileDataController controller = new MobileDataController(context);
        shadowOf(manager).setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER, true);
        assertTrue(controller.isEnabled());
        TelephonyManager otherSim = org.robolectric.util.ReflectionHelpers.callConstructor(
                TelephonyManager.class,
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(Context.class, context));
        shadowOf(manager).setTelephonyManagerForSubscriptionId(4, otherSim);
        shadowOf(otherSim).setDataEnabledForReason(
                TelephonyManager.DATA_ENABLED_REASON_USER, false);
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(4);
        Settings.Global.putInt(context.getContentResolver(), "mobile_data", 1);
        assertFalse(controller.isEnabled());
        ShadowSubscriptionManager.setDefaultDataSubscriptionId(3);
        assertTrue(controller.isEnabled());
    }

    @Test public void relevantSettingsNotifyAndCloseUnregistersObserver() {
        MobileDataController controller = new MobileDataController(context);
        AtomicInteger changes = new AtomicInteger();
        controller.setStateChangeListener(changes::incrementAndGet);
        Settings.Global.putInt(context.getContentResolver(), "mobile_data", 1);
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(changes.get() > 0);
        int before = changes.get();
        Settings.Global.putInt(context.getContentResolver(), "unrelated_setting", 1);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(before, changes.get());
        Settings.Global.putInt(context.getContentResolver(), "multi_sim_data_call", 4);
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(changes.get() > before);
        controller.close();
        before = changes.get();
        Settings.Global.putInt(context.getContentResolver(), "mobile_data", 0);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(before, changes.get());
    }
}
