# Contributing

Veil Panel is a standalone, non-root notification panel. Read [development](docs/DEVELOPMENT.md) and [limitations](docs/LIMITATIONS.md) before changing gestures or permissions.

## Scope

Priorities are reliable gesture handling across devices, physical-device compatibility, accessible layouts, RTL support and measured rendering performance. The custom control center is disabled; do not add a user-facing switch, shortcut or legacy route to enable it as part of unrelated work.

### Accepted integration methods

- Public Android APIs and explicitly user-granted permissions.
- AccessibilityService for supported gestures/global actions, NotificationListenerService for alerts, and Android overlays and system settings intents.
- Isolated OEM integrations with runtime capability checks, a safe fallback and documented physical-device tests.
- Optional one-time ADB grants for a specific feature, with its scope, revocation and fallback documented. Core notifications must work without ADB.

### Out of scope

Root/`su`, Magisk, Shizuku, privileged system-app installs, system-partition changes, bootloader unlocking and persistent ADB/privileged daemons. Do not silently change system settings or display successful state after a failed action.

## Compatibility

Preserve the Android application ID, service/component identities, shortcut actions and preference keys unless a deliberate migration is included. The application identity is `com.ubai.veilpanel`.

A device-specific feature must detect its capability, fail safely, offer an alternative where practical and document the device, Android version and firmware. Emulator success is not OEM compatibility evidence. New permissions, services and OEM integrations need explicit review.

## Validation and pull requests

Run the relevant tests and build checks in [development](docs/DEVELOPMENT.md). For gesture changes, test ordinary and fullscreen apps, fast/slow/held pulls, repeated left pulls, right-side native handoff, dismissal, screen-off/unlock, power saver and permission loss. Report what was and was not tested. Measure performance before claiming it improved.

Each PR must describe the problem and new behavior, Android integration method, permission/dependency/background-work changes, tested devices, unsupported-device behavior and validation. Preserve settings unless an explicit migration is included.

Use synthetic notifications and a permission-free fixture where possible. Do not act on real messages as a test. Automation that takes the global Accessibility connection can disrupt running services; prefer targeted input/window observations on your own device.

Never submit personal notification data, signing keys, credentials, unreviewed media or complete bugreports. Follow [SECURITY.md](SECURITY.md) for sensitive reports. Contributions are GPL-3.0-or-later, with compatible dependency licenses and attribution retained.
