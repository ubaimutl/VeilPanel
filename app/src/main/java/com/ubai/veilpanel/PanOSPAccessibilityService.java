package com.ubai.veilpanel;

import com.ubai.veilpanel.accessibility.PanOSPAccessibilityServiceCore;

/**
 * Stable Android component entry point.
 *
 * Keep this class name stable so Android/HyperOS does not treat a refactor as a
 * brand-new accessibility service and unnecessarily require the user to grant
 * access again.
 */
public final class PanOSPAccessibilityService extends PanOSPAccessibilityServiceCore {
}
