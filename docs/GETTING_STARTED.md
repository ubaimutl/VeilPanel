# Getting started

Veil Panel requires Android 13+. Physical-device testing is limited to REDMAGIC 10 Pro / Android 16 with separate native notification and quick-settings panels. Combined native shades are not supported yet. See [limitations](LIMITATIONS.md).

## Access

Install a compatible build and open the app. Enable these from Setup & access:

| Access | Purpose |
| --- | --- |
| Accessibility | Top-edge gestures, the trusted overlay and native-shade handoff; no app view-tree retrieval |
| Notification access | Read current notifications and their available actions; dispatch requested dismissals |
| Display over other apps | Saved-wallpaper backdrop without launching an Activity over your foreground app |

If Android blocks sideloaded Accessibility access, App info → menu → **Allow restricted settings** may be needed. No root, Shizuku, ongoing ADB connection or one-time ADB grant is required.

Select the original image used as your Home wallpaper. A private copy supplies the backdrop; it does not change Home wallpaper. Without it or overlay access, the older Activity fallback can pause underlying video.

## Gestures and customization

- Left pull opens notifications. Right pull opens Android’s original quick settings; the app has no custom control center.
- Expand app stacks to see individual notifications. Silent notifications start collapsed at the bottom.
- Drag the bottom handle upward, or tap it, to close. Adjust its line size, position and grab area in settings.
- Fullscreen protection uses a first pull to reveal native bars and a fresh second pull to open notifications. Behavior varies by game and ROM.
- Compatible launchers can use **Open notifications**, an ordinary app shortcut. No particular launcher is required.

Start with Minimal, Frosted, Glass or Readable at the top of settings, then customize further. Undo last theme restores the appearance immediately before the last preset. Export a backup before trying several presets.

Settings include card margins, dimensions, spacing, icons, text, blur, optional glass and clock/date controls. Wallpaper & glass → Scroll beneath status icons is enabled by default; turn it off to keep notification content below a wallpaper-backed status strip. Custom TTF/OTF fonts up to 20 MB are copied privately after validation. Keep original font/wallpaper files separately.

For an iOS-style appearance, tune rounded translucent cards, grouped stacks, fonts, wallpaper blur and curtain dismissal. This is a customizable presentation, not a replacement for Android SystemUI.

## Pause and updates

Use Pause panel or disable only this app’s Accessibility service to return to native behavior. Do not uninstall or clear app storage if you want to retain configuration. An in-place APK update must keep the same application ID and signing certificate. Use Advanced & about → Backup & transfer settings to export or restore a portable ZIP. It includes saved wallpaper and imported fonts, but Android permission grants must be enabled separately. Treat the ZIP as personal data.
