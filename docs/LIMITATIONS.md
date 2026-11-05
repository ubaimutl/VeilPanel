# Limitations and known issues

## Tested scope

Only REDMAGIC 10 Pro / Android 16 has been tested on a physical device. The native OS must support separate notification and quick-settings panels. Android 13 is the minimum supported API, not a promise that every Android 13+ device works. Combined native shades need a separate routing implementation.

## Native shade and gesture handling

No visible native-shade overlap is currently reported in the confirmed REDMAGIC 10 Pro / Android 16 / REDMAGIC OS 11.0.6_EU configuration. Other devices and firmware versions remain unverified. Android's SystemUI can observe or take over reserved edge gestures, so ordinary app APIs cannot guarantee exclusive ownership on every device.

Motion includes independent switches for native-edge recovery and its temporary status-area cover. Both are enabled by default and can be disabled on devices that do not need them. The temporary cover can briefly hide the real status icons during recovery.

Wallpaper & glass → **Scroll beneath status icons** is enabled by default. If moving content appears split at the status-bar boundary, turn it off to keep a wallpaper-backed strip beneath the real icons, with notification content stopping below it.

Fullscreen two-swipe protection must be tested per game/ROM. Transient system bars do not always report visible through app insets. Automated tests cannot prove OEM gesture behavior.

## Overlay boundaries

Veil is an application overlay, not a privileged replacement for SystemUI. It cannot guarantee exclusive ownership of the status-bar gesture, replace lock-screen/security surfaces, or reproduce every native notification animation. Secure or OEM-protected screens may restrict overlays. Other gesture/accessibility apps can affect routing.

## Wallpaper, blur and power saver

The normal backdrop and glass use a user-selected static image. They do not capture other apps or live wallpaper movement. Choose a new image after changing Home wallpaper. Saved-image software blur does not require system cross-window blur, which the OS may disable in power saver. Shader failure has a visible fallback; visual/performance behavior is hardware-dependent.

The older Activity backdrop fallback can pause underlying video. Use the saved wallpaper plus display-over-apps permission for the non-Activity backdrop.

## Notifications and fonts

Android decides what notification text, actions, images, grouping and ranking are available. App-specific custom layouts cannot always be reproduced. Notification access being enabled is different from the listener actually being connected; ROM background management can interrupt it. Replies use an Activity and may affect the underlying app’s lifecycle.

The portable settings ZIP can include saved wallpaper, an imported font and a managed panel background. It does not transfer notifications or Android permission grants. Treat exported archives as personal data and keep your original files. A source archive or APK does not contain your personal configuration. The chosen font may not cover every language or symbol.

## Native quick settings only

The custom control center is disabled and unavailable. Right-side pulls keep Android's original quick settings. There is no supported custom-control-center switch, shortcut or legacy editor mode.

Ordinary app APIs cannot provide every system toggle or guarantee exclusive native-gesture ownership. Root, Shizuku, privileged system installs and persistent shell backends are outside the project scope. Extra privileges are not presented as a promised future solution.

## Reporting

Include app version, phone model, Android/ROM version, whether the native panels are split, whether the app is fullscreen, and the exact gesture. Do not include IMEI, serial numbers, account names, unredacted notifications or raw logs. Use the [bug form](../.github/ISSUE_TEMPLATE/bug_report.yml).
