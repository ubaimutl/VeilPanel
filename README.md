# Veil Panel

<img src="docs/branding/veil-mark.svg" width="80" height="80" alt="Veil Panel notification-stack logo">

A customizable Android notification panel with grouped cards, wallpaper-based glass and a curtain you can drag to close.

**Experimental · Android 13+ · Tested only on REDMAGIC 10 Pro / Android 16**

Veil Panel is an overlay built on [PanOSP](https://gitlab.com/gibdev-group/gibapps/panosp). It keeps your phone’s original quick settings on the right. It works without root or Shizuku on the tested setup, but does not replace SystemUI at the framework level.

[Version 1.0.0 release notes](docs/releases/v1.0.0.md) · [Get started](docs/GETTING_STARTED.md) · [Known limitations](docs/LIMITATIONS.md) · [Build](docs/DEVELOPMENT.md) · [Contribute](CONTRIBUTING.md) · [License](LICENSE)

## Preview

Captured on REDMAGIC 10 Pro / Android 16 with Veil Panel and a customized appearance. These show one configuration; spacing, fonts, icons, glass and the dismiss handle can all be adjusted.

| Notification panel | Silent notifications expanded |
| :---: | :---: |
| ![Notification panel with an app stack and collapsed silent notifications](docs/media/notification-panel.jpg) | ![Silent notification section expanded below the app stack](docs/media/silent-expanded.jpg) |

| Dragging the curtain closed | Customization settings |
| :---: | :---: |
| ![Panel partially dismissed using the bottom handle](docs/media/curtain-dismissal.jpg) | ![Veil Panel settings organized into collapsible sections](docs/media/appearance-settings.jpg) |

### Gesture demo

<a href="docs/media/veil-panel-demo.mp4"><img src="docs/media/gesture-demo.gif" width="320" alt="Veil Panel opening, expanding silent notifications and closing with a curtain drag"></a>

[Watch or download the full 23-second video](docs/media/veil-panel-demo.mp4).

## An iOS-style panel, adjusted your way

Veil Panel can be customized for an **iOS-style Notification Centre appearance**: rounded translucent cards, app stacks, wallpaper blur, custom fonts and a curtain-style dismissal. Adjust card margins, width, height, spacing, corner radius, icons and text, then tune the blur and optional glass effect to your taste. The appearance is configurable rather than a fixed theme.

This changes the notification panel’s presentation on Android. Device compatibility and system-control restrictions still apply.

## What it does

- Groups notifications by app, with expandable stacks and a collapsed silent section.
- Opens notifications, exposes available actions and supports dismissal and replies where Android provides them.
- Uses a saved wallpaper for the background, with adjustable blur and an optional Kyant-derived glass experiment.
- Offers Minimal, Frosted, Glass and Readable appearance presets, with one-level Undo.
- Lets you tune cards, custom TTF/OTF fonts, clock and date layout, an optional glass clock and status-bar scrolling.
- Provides a draggable bottom handle with adjustable size, grab area and position.
- Supports a launcher shortcut and a two-swipe protection option for fullscreen apps.
- Exports and restores settings, saved wallpaper and imported fonts in a portable ZIP archive.

There is no Internet permission, advertising or analytics SDK in the notification build. Read the [privacy details](docs/PRIVACY.md) before enabling notification and Accessibility access.

## Compatibility and current status

The tested configuration uses **separate notification and quick-settings panels**: left opens Veil; right opens the original controls. Combined all-in-one native shades are not supported yet. A successful build or emulator test is not a compatibility claim for another phone.

**Tested device:** no visible native-shade overlap is currently reported in the confirmed REDMAGIC 10 Pro / Android 16 / REDMAGIC OS 11.0.6_EU configuration. Other models and firmware versions remain unverified. This is an experimental overlay, so the result does not guarantee identical gesture handling on every device. See [limitations](docs/LIMITATIONS.md).

## Design principles

Veil Panel is a **standalone notification panel**. It works with your existing launcher and does not require or bundle any launcher app.

- **No root or Shizuku:** no privileged daemon, persistent ADB connection, bootloader unlock or system-partition modification is required or planned.
- **Supported Android interfaces:** Accessibility for gestures, NotificationListenerService for alerts, overlays for presentation and system intents for fallbacks.
- **Minimal, explicit access:** core notifications never depend on an ADB grant. An optional one-time grant may be considered for a specific future feature only with documentation, revocation and an ordinary-app fallback.
- **Local data:** notification content, settings, wallpaper and fonts stay on the device; no analytics or networking is added.
- **Capability checks:** unsupported actions must fail clearly, and device-specific methods need isolated implementations and physical-device testing.

## Notification panel only

The custom control center is **disabled and unavailable** in this build. There is no setting, shortcut or legacy editor route to enable it. A right-side pull continues to use Android’s original quick settings.

Ordinary app permissions cannot reproduce every stock system toggle or guarantee exclusive native-gesture ownership. Root, Shizuku, privileged system installs and persistent shell backends are outside this project’s scope. This project does not claim to replace SystemUI.

## Setup

1. Install the APK from the [releases page](https://github.com/ubaimutl/VeilPanel/releases), or build the app using the instructions below.
2. Enable Veil Panel’s **Accessibility service**, **Notification access** and **Display over other apps** using its setup page.
3. Choose the original image used for your Home wallpaper. Veil keeps a private copy for its backdrop.
4. Try left and right pulls, then adjust the split and appearance in settings.

If Android blocks a sideloaded Accessibility service, App info → menu → **Allow restricted settings** may be needed. Setup details are in the [getting-started guide](docs/GETTING_STARTED.md).

## Development

JDK 21, Android SDK 36 and the checked-in Gradle wrapper are required. Java source compatibility is 17.

```sh
./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:lintRelease :app:assembleDebug
```

Required GibModules source is included in this repository. See the [build instructions](docs/DEVELOPMENT.md) for setup, testing and signing.

Contributions are welcome, especially reproducible gesture-race cases, device compatibility testing, accessibility and performance. Read [CONTRIBUTING.md](CONTRIBUTING.md).

## License and credits

Veil Panel is distributed under **GPL-3.0-or-later**, preserving PanOSP’s license. The full text is in [LICENSE](LICENSE). [NOTICE](NOTICE) and [third-party notices](THIRD_PARTY_NOTICES.md) cover PanOSP, GibModules, the Apache-2.0 Kyant shaders and the OFL-licensed Josefin Sans font.
