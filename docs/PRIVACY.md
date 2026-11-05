# Privacy and access

## Network and tracking

The notification-focused manifest has no Internet permission. Its declared dependencies are local modules and AndroidX UI libraries; no advertising or analytics SDK is declared. There is no account, analytics endpoint or remote notification upload in this implementation. These statements describe this source branch, not every future contribution or a formal security certification.

## What stays on the device

| Data | Use and storage |
| --- | --- |
| Notifications | Android-supplied content and actions are held in the process-local repository for display; this is not a notification-history database. |
| Wallpaper | A bounded private saved copy supplies the backdrop and optional glass. Original source files are unchanged. |
| Custom font | A validated private TTF/OTF copy; invalid imports preserve the prior font. |
| Preferences | Local app preferences. A user-requested settings ZIP can include preferences, wallpaper, imported font and a managed panel background. |

Android automatic app backup is disabled in the manifest. Advanced & about → Backup & transfer settings lets you export a ZIP using Android’s document picker. The archive does not include notifications or OS permission grants. Its destination may be a local or cloud document provider you choose; protect and share that archive accordingly. A source archive or APK is not a backup of your personal configuration.

## Access explained

Accessibility handles the edge gesture, trusted overlay and native-panel dismissal. The service configuration sets `canRetrieveWindowContent=false`; it does not obtain other apps’ view trees. Notification access is separately required to receive notifications. Display-over-apps access provides the non-Activity wallpaper host. The notification build does not request root, Shizuku, screen recording or `WRITE_SECURE_SETTINGS`.

The custom control center is unavailable. The current notification app does not ask for camera, flashlight controls or protected system-settings write access.

## Diagnostics and reports

Routine listener, repository and notification-card action logs do not include notification keys, package names or notification text. Notification-card launch failures also omit exception details that could contain identifiers. Exceptions, Android logs and other apps’ logs can still contain sensitive information; do not assume an arbitrary logcat dump is safe to publish.

Share the smallest relevant, reviewed excerpt. Remove notification contents, accounts, app-specific identifiers, network names, serial numbers and unrelated app data. Screenshots and videos need the same review. Use synthetic notifications for demos.

Never commit signing keys, local SDK configuration, device dumps or personal app-data exports. Review screenshots, logs and release assets before sharing. Automated pattern scans are not a substitute for that review.
