# Help and limitations

Veil Panel is experimental. It has been tested on **REDMAGIC 10 Pro with Android 16 / REDMAGIC OS 11.0.6_EU**. Other phones and software versions have not been confirmed, even if the app installs successfully.

Android 13 is the minimum version. Your phone also needs separate notification and quick-settings panels; a single combined panel is not supported yet.

## Gestures and system panels

Android still controls its own panels and top-edge gestures. On some phones or in some apps, Android’s panel may appear instead of Veil, or briefly appear alongside it. Earlier overlap problems have not returned in the confirmed test setup, but that does not guarantee the same result on other phones.

If you have trouble opening Veil:

- Check **Setup & access** and confirm Accessibility is enabled.
- Pull from the left side. You can adjust the split in **Gestures & shortcuts**.
- In a fullscreen app, try a fresh second pull after revealing the system bars.
- If you use another gesture app, check whether its active area overlaps Veil’s gesture or dismiss handle.

The two-swipe option applies when Veil can detect that an app has hidden both status and navigation bars. Apps that hide only the status bar use one pull. A navigation replacement that removes the navigation bar entirely can also result in one-pull behaviour.

Under **Motion**, **Recover native shade after lost gestures** helps when Android takes over a pull. **Cover status area during edge recovery** can temporarily hide the status icons while that happens. Both can be turned off if they cause problems on your phone.

## Content near the status bar

**Wallpaper & glass → Scroll beneath status icons** lets notifications pass behind the status icons. If moving content looks split at that boundary, turn it off. The icons will sit over a strip of the panel background, and notifications will stop below them.

System popup banners and screenshot controls can appear behind Veil. A part of a banner may still be visible in the transparent status-bar area. Veil does not currently guarantee that these popups appear above the panel or are completely covered.

The lock screen and other protected screens stay under Android’s control. Some apps may prevent Veil from appearing over them.

## Wallpaper and readability

Veil uses the image you choose, rather than a live view of your home screen or the app underneath. It does not copy live wallpaper animation. Choose a new panel image when you want to change the background.

Bright images receive extra shading to keep notification text readable. This can make cards darker than the opacity or tint value you selected.

Blur and glass can look different or run less smoothly on different phones. If needed, reduce blur or turn off glass effects. The saved-image blur works without Android’s system blur, including in power saver.

If opening Veil pauses a video or app, choose a panel wallpaper and enable **Display over other apps**. Without them, Veil uses an older background method that can interrupt the app underneath.

## Missing notifications or message text

Veil can show only the notification content that an app shares with Android. Conversation notifications can include multiple messages, but Veil cannot recover messages that the sending app leaves out. Open that app to read the full conversation.

Some notification layouts, images and actions may look or work differently from your phone’s original panel. Replying can also interrupt the app you were using.

If notifications stop updating, check notification access in **Setup & access**. Your phone’s battery or background-app restrictions may also interrupt notification delivery.

An imported font may not include every language or symbol. Try another font if characters are missing.

## Updates and backups

Install updates over the existing app to keep your setup. If Android reports a signing conflict, you may be using a build from a different source. Export your settings before uninstalling anything; uninstalling or clearing storage removes Veil’s saved data.

Backups can include images and fonts, but they do not restore notifications or permission approvals. On another phone or after a fresh installation, enable access again. Keep your original image and font files too.

## Report a problem

Use the [bug report form](../.github/ISSUE_TEMPLATE/bug_report.yml). Include:

- Veil version, phone model, Android version and phone software version.
- What you did, what you expected and what happened.
- Whether the problem happens in a particular app or in fullscreen mode.
- Whether you use a gesture-navigation app.

For a gesture problem, mention whether your phone normally has separate notification and quick-settings panels. Hide personal details in any screenshots or recordings. Do not post device serial numbers, IMEI, private messages or full Android logs.
