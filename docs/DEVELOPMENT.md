# Development

Use JDK 21 and Android SDK 36. The wrapper pins Gradle 9.7.1; the Android plugin is 9.4.1. Java source compatibility is 17. Configure your SDK using `ANDROID_HOME` or an ignored `local.properties` file.

```sh
./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:lintRelease :app:assembleDebug
```

The debug APK is written under `app/build/outputs/apk/debug/`. A debug build uses your local debug certificate: it cannot update an existing installation signed with another key. Do not uninstall a configured installation merely to bypass a signature mismatch. Preserve the existing application ID and signing key for an in-place update.

Vendored GibModules are checked against the manifests in `emp/` during Gradle configuration. Keep their licenses and hashes intact. Changes to vendored code require review and an intentional manifest update.

## Validation

JVM tests use JUnit and Robolectric. They cover notification grouping, actions, rendering, settings and gesture decisions. These tests do not prove how an OEM SystemUI window races with the overlay. Test real hardware separately, using synthetic notifications and the device matrix in [CONTRIBUTING.md](../CONTRIBUTING.md).

CI builds and tests unsigned development source. It does not install on a phone, modify device settings, sign distribution builds or publish APKs. Never put signing passwords, keystores, device dumps or personal captures in Git.

## Application identity

The application ID and Android namespace are `com.ubai.veilpanel`. Application source lives under `app/src/main/java/com/ubai/veilpanel/`. Keep service names, shortcut actions and preference keys stable for compatible updates.

See [third-party notices](../THIRD_PARTY_NOTICES.md) and [contribution policy](../CONTRIBUTING.md).

## Code map

- `accessibility/` and `ui/ShadeWindowController`: top-edge routing, trusted overlay and native quick-settings handoff.
- `notifications/`: Android listener, in-memory repository, grouping and notification content.
- `ui/ShadeView`, `ui/CurtainMotion`: notification presentation and drag/settle behavior. The content view has no quick-settings mode.
- `ui/PanelStyle`, glass helpers and `image/`: typography, optional rendering and private image storage.
- `MainActivity`: notification settings only. Only notification settings are exposed.
- `app/src/test/`: JVM/Robolectric checks. `modules/GibModules/` and `emp/`: vendored modules and integrity manifests.

Paths above are relative to `app/src/main/java/com/ubai/veilpanel/` unless stated otherwise. Quick-settings tiles are not an enabled app feature. Do not expose a custom control center through settings or intents.

## Distribution

Keep LICENSE, NOTICE and dependency notices with source releases. Use an increasing Android version code for upgrades. Distribution signing keys remain private.

Review source, permissions and media before distribution. Keep signing credentials, local configuration and personal device data out of Git.
