# Getting started

Veil Panel gives your notifications a separate, customizable panel. Your phone’s quick settings stay where they are: pull down from the right to use them.

You need Android 13 or later. So far, physical testing is limited to REDMAGIC 10 Pro with Android 16. Phones with a single combined notification and quick-settings panel are not supported yet. See [compatibility and limitations](LIMITATIONS.md).

## Install and enable the panel

Download the APK from the [releases page](https://github.com/ubaimutl/VeilPanel/releases), install it and open **Veil Panel**. Open **Setup & access** and follow each button to Android’s permission screen.

| Permission | Why Veil needs it |
| --- | --- |
| Accessibility | Opens Veil when you pull down from the notification side and helps keep Android’s panel from opening at the same time. |
| Notification access | Shows your notifications and lets you use their available actions, replies and dismissals. |
| Display over other apps | Shows the panel’s wallpaper over the app you are using, so that app can keep playing underneath. |

If Android says Accessibility access is restricted, open your phone’s **App info** page for Veil Panel. Open the menu and choose **Allow restricted settings**, if that option is available, then return to setup.

You do not need root, Shizuku or a computer to use Veil. The default layout is ready to try once access is enabled.

## Choose a background

Open **Wallpaper & glass → Choose panel wallpaper**. Pick the image you want behind your notifications. To match your home screen, choose the same original image.

Veil saves its own copy. It does not change your home-screen wallpaper. If you change that wallpaper later, choose the new image in Veil too. Live wallpaper movement is not copied.

With Liquid Glass off, **Card opacity** adjusts the card tint from clear to solid. **Card blur** softens the saved wallpaper only inside cards, leaving text sharp. It is separate from **Wallpaper blur**, which softens the whole background. Reopen the panel after changing these settings.

If an app or video pauses when you open the panel, check that you have chosen a panel wallpaper and allowed **Display over other apps**.

## Open, read and close

- Pull down from the left side of the top edge to open Veil.
- Pull down from the right side for your phone’s original quick settings.
- Expand an app’s stack to read its notifications. Expand the silent section when you want to see quieter alerts.
- Tap a notification to open it. Available actions and replies depend on the app that sent it.
- Drag the bottom handle up, or tap it, to close Veil.

In **Gestures & shortcuts**, adjust **Pull area width (%)** and **Pull area height (dp)** to choose where you can pull down to open Veil. A coloured outline updates as you move either slider. **Show pull area** lets you check it again; the outline disappears when you leave settings.

You can also enable **Swipe up again at the end to close**. Scroll to the bottom, lift your finger, then swipe up again. When you feel a light tick, release to close the panel. Pull back before releasing to keep it open. If the cue is silent, check that touch vibration is enabled in Android’s sound and vibration settings.

**Two swipes in fullscreen apps** helps avoid accidental opening when an app hides both system bars. The first pull reveals the bars; a fresh second pull opens Veil. Apps hiding only the status bar use one pull. This can vary with your phone’s navigation setup; see [gesture help](LIMITATIONS.md#gestures-and-system-panels).

You can also add Veil’s **Open notifications** shortcut through a launcher that supports app shortcuts.

Under **Bottom actions**, enable the optional shortcuts and choose Flashlight, Camera, Veil settings, Close panel or None for either side. Lower **Distance from bottom** to move them closer to the edge: zero is the edge and 48 is the default. The dismiss handle keeps its own area between them. Enable **Fade notifications behind buttons** to make scrolling cards disappear gently before reaching the buttons. The flashlight asks for camera permission only when you choose to allow it; it does not take photos.

## Find a look you like

Open **Presets** and try Minimal, Frosted, Glass, Readable, Demo, Orbit, Editorial, Slate, Air or Nocturne. Presets are arranged in a grid. **Undo last theme** stays at the bottom while you browse and restores the look you had immediately before your most recent preset. It remembers one change, so save a backup if you want to keep several setups.

Then explore the settings:

| Setting | What you can change |
| --- | --- |
| Wallpaper & glass | Background image, wallpaper blur, card opacity and separate card blur when Liquid Glass is off. |
| Status bar | Show or hide the icons, choose their colour, and use transparent content or a wallpaper background. |
| Clock & header | Clock and date sizes, alignment and style. |
| Notifications | How much notification text appears before and after expansion. |
| Typography & icons | Panel fonts, text sizes and notification icons. |
| Spacing & layout | Card width, height, gaps and margins. |
| Liquid Glass | Glass highlights and other glass effects. |
| Dismiss handle | Handle position and the size of the area you can grab. |
| Bottom actions | Optional left and right shortcuts, button size and position. |
| Replies & actions | Text or filled Send buttons, Cancel visibility, the app icon, message previews, text size and spacing. |
| Motion | Scrolling bounce and help with gestures that open Android’s panel. |

Use **Search settings** to find a setting by name. Tap a slider’s number to enter an exact value, or use its reset arrow to return to the default. Reopen the panel to see appearance changes.

Open **Settings appearance** to follow your phone’s theme or choose Light or Dark. Follow system is the default. This affects settings screens only.

Under **Status bar**, turn off **Transparent status bar** to keep your saved panel wallpaper behind the icons instead of scrolling notifications. **Status icon colour** lets you choose light or dark icons, or match the panel’s text colour.

You can import a TTF or OTF font, up to 20 MB, for the panel. Keep the original file in case you need it again.

## Save or restore your setup

Open **About & backup → Backup & transfer settings**, then choose **Export complete settings**. Choose where to save the ZIP file.

The backup includes your panel settings, saved wallpaper and imported font. It does not include notifications or Android permissions. Keep it private, especially if it contains personal photos.

To restore it, close the panel, return to **Backup & transfer settings** and choose **Import complete settings**. Importing replaces the current panel setup. If you are moving to another phone, enable the requested permissions there too.

## Pause or update Veil

Choose **Setup & access → Pause panel** to use Android’s panel again. To resume, return to setup and re-enable Veil’s Accessibility service.

Install updates over the existing app to keep your settings. Avoid uninstalling or clearing storage unless you want to erase them. Export a backup before making a fresh installation. If Android refuses an update, see [update help](LIMITATIONS.md#updates-and-backups).
