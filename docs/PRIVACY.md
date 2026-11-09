# Privacy and permissions

Veil Panel does not have Internet permission. It has no ads, analytics or account to sign into. It does not send your notifications, wallpaper or settings to a server.

## What Veil keeps

| Information | How it is used |
| --- | --- |
| Notifications | Kept in memory to show the current panel. Veil does not keep a notification-history database. |
| Panel wallpaper | A private copy of the image you choose, used for the background and glass effects. Your original image is unchanged. |
| Imported font | A private copy used for the panel’s text. |
| Settings | Saved on your device so the panel keeps your chosen layout and behaviour. |

Android’s automatic app backup is disabled for Veil. You can make your own backup in **About & backup → Backup & transfer settings**.

## Why the permissions are needed

**Accessibility** lets Veil handle the top-edge gesture and help close Android’s panel when it opens at the same time. Veil does not read the contents of other apps’ screens through Accessibility. It leaves the lock screen to Android.

**Notification access** lets Veil read the notifications Android provides. It also lets you open them, use available actions, reply or dismiss them. Notification access can expose sensitive message content, so only enable it if you are comfortable letting Veil display that content.

**Display over other apps** lets Veil show its saved wallpaper while another app stays open underneath.

Veil does not ask for screen recording, root, Shizuku, camera access or a computer connection. Quick settings such as Wi-Fi and the flashlight remain in your phone’s original panel.

An enabled gesture-navigation service can ask Veil to close its open panel. This lets a navigation swipe dismiss the panel without sending you home from the app underneath. That request does not give the gesture app access to your notifications or settings.

You can turn these permissions off in Android settings. Use **Pause panel** in Veil’s setup page if you just want to stop using its panel.

## Backups you create

An exported ZIP contains your settings and can include your chosen images and imported font. Notifications and Android permission approvals are not included.

You choose where to save the backup through Android’s file picker. If you choose cloud storage, that provider handles the upload. Veil itself does not upload it. Treat the ZIP as a personal file and share it only if you want someone else to have its contents.

Downloading an APK or a source ZIP does not back up your personal setup.

## Screenshots and problem reports

Check screenshots and videos before sharing them: they may show messages, account names or other personal details. Hide anything you do not want public.

Veil’s routine notification logs do not include notification text, notification keys or package names. A full Android log can still contain sensitive information from the system or other apps. If a log is needed to investigate a problem, review it and share only the relevant part.

For a security concern, follow [Security reporting](../SECURITY.md) rather than posting private details in a public issue.
