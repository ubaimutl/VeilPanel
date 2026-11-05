# GibUI — navigation and background selector

Status: **EXPERIMENTAL**, embedded Android library, Java 17 / minSdk 23.
The background selector depends on `colorpicker-android`; navigation has no colorpicker coupling.
The host owns navigation titles, page contents, insets,
preferences and lifetime; no application services or inter-app runtime are used.

## Implemented experimental capability

`fr.gibapps.ui.navigation.SectionPager(Context)` is a vertical View containing a
fixed, horizontally scrolling section bar and a page area. Use on the main thread.

- `setPrimaryNavigation(true)`: enable the primary-navigation mode. Call it before
  `addSection`; changing this mode after the first section throws `IllegalStateException`.
- `addSection(CharSequence title, int iconResId, View page)`: append a primary section.
  Its tab displays the icon above the label in a 76 dp-high header. Labels may use up
  to two lines, and every tab keeps a minimum width of 72 dp.
- `addSection(CharSequence, View)`: append an unattached host page; first is selected.
  Supply a ScrollView per page when independent vertical scrolling is required.
- `getSectionCount()`, `getCurrentSection()`: empty selection is -1.
- `selectSection(int, boolean)`: invalid and unchanged indices are ignored.
- `showAdjacentSection(int)`: sign of direction selects a neighbour when one exists; edges are bounded like AndroidX ViewPager/GibClock.
- `setSwipeEnabled(boolean)`, `isSwipeEnabled()`: swipe is initially disabled.
- `setOnSectionSelectedListener(...)`: optional callback after selection changes.
- `setRightOverflowIndicator(View)`: optional unattached host affordance with host
  text/contentDescription. It overlays the right edge only while content is hidden.
  Its click reveals hidden tabs; it is never a page. Null removes the affordance.
- `setGestureProtection(GestureProtection)`: optional predicate for touched custom
  controls. SeekBar visible bands (+12dp vertically) are always protected, across
  the page width. The predicate adds protection for custom controls, without a
  dependency on their concrete implementation. Null clears this extension.

`SectionState` is the platform-free selection/overflow policy, tested separately.
Horizontal paging itself is deliberately delegated to `androidx.viewpager.widget.ViewPager`
(the same AndroidX engine used by GibClock) rather than reimplemented in GibUI. `SectionPager`
remains the stable GibUI facade: hosts still provide existing `View` pages, callbacks, tab
appearance and gesture exclusions, while AndroidX owns drag tracking, fling thresholds,
settling, frame timing and cancellation. Pages are kept warm with an offscreen limit sized
to the current section count so page creation/layout does not enter the gesture path.
Navigation is bounded at the first and last page, matching GibClock/ViewPager. SeekBar bands
and host-declared custom controls can retain their complete horizontal gesture via
`setGestureProtection(...)`. No preference or host value is changed.

Hosts that already provide their own section navigation can call
`setNavigationVisible(false)`. This hides only SectionPager's internal navigation
chrome; ViewPager paging, swipe, selection state and callbacks remain active.

In the default secondary-navigation mode, titles retain 15sp text in a 52 dp-high
header and scroll rather than shrinking. The active tab is bold,
selected for accessibility and highlighted, and is brought back into view. A title
wider than the viewport can itself be scrolled. The overflow affordance is physical
right; automatic RTL reversal is not part of this first experimental capability.

## Host responsibilities and reserved extensions

The host retains page index across reconstruction/recreation and supplies scroll
containers. Page children keep scroll state while switching existing pages; rebuilding
pages creates new children. No ViewModel, persistence layer or automatic restoration.

The overflow View and gesture predicate are available extension contracts. Full visual themes, cards and alternative tab renderers remain reserved.
The background selector and drawable below are now implemented experimentally.

## Build and checks

From the module root project, with SDK configured:
`./gradlew :gib-ui-android:assembleDebug :gib-ui-android:testDebugUnitTest`

Tests: pure state/bounds/intent plus Robolectric navigation, tabs, overflow,
protected controls, cancellation and vertical gestures. Device accessibility,
real touch, scrolling and animation smoothness still require manual validation.

## Optional appearance

`setAppearance(SectionAppearance)` accepts five immutable Android ARGB colors (surface, active/inactive text, active/inactive tab). Null restores the original defaults. No dimensions, selection, gestures or callbacks are changed. The navigation library has no style-library dependency.


## Background selection — EXPERIMENTAL

Package: `fr.gibapps.ui.background`. Extracted from the validated PanOSP selector;
no PanOSP/GlGib/GibClock dependencies and no GibStyle dependency. OpenGL remains in
GlGib. Navigation sources and contracts are unchanged.

- `BackgroundConfig(int type, int color1, int color2, int direction)`: immutable,
  platform-free value. Public final fields; type clamped 0..2, direction 0..5,
  colors forced opaque. Constants `SOLID`, `LINEAR`, `RADIAL`, `TOP_BOTTOM`,
  `LEFT_RIGHT`, `BOTTOM_TOP`, `RIGHT_LEFT`, `CENTER_OUT`, `OUT_CENTER`.
  Type and direction are independently bounded, not silently reconciled.
- `GibBackgroundDrawable(BackgroundConfig, float cornerRadius)` or the original
  `(int type, int color1, int color2, int direction, float cornerRadius)` signature.
  Radius in pixels; new drawable per owner. Standard Drawable bounds, alpha and
  color filter. Same PanOSP linear/radial formulas and corner-covering radius.
- `BackgroundSelector(Context, BackgroundConfig, int accentColor)` is a vertical
  Android View containing the four preference-like rows. Use an Activity/themed
  Activity context and the main thread. No persistence, migration or saved state.
- `getConfig()`, `setConfig(BackgroundConfig)`: host input is silent. A different
  config supersedes an open dialog without publishing stale rollback. An echo of
  the latest callback leaves the dialog open.
- `setOnBackgroundChangedListener(OnBackgroundChangedListener)` receives complete
  immutable configurations via `onBackgroundChanged(BackgroundConfig)` on the UI
  thread. Null clears the listener. The host applies/previews and optionally
  persists the result. It can echo the received value through `setConfig`.
- The type dialog exposes **Solid / Gradient only**. Gradient derives type 1/2
  from the retained direction. The six thumbnails have order **0,2,1,3,4,5**.
  Each thumbnail immediately updates the config and notifies; Cancel/back returns
  the full original config and notifies. OK keeps/notifies the current config.
  Notifications may repeat an equal config on OK/cancel: these are not a distinct
  persistence transaction API. Hosts should not deduce commit-only semantics.
- Color dialogs use the shared SvColorPicker, applying on OK only; Cancel does not
  notify or change the config. Circular 26dp swatches, whole-row click targets,
  thumbnail/dialog dimensions and palette retained from PanOSP.
- `dismissDialogs()` synchronously closes the dialog and rolls back a pending
  direction preview. Called automatically on detach. Hosts can also call it
  before destroying/replacing a screen. Configuration/state restoration across
  Activity recreation remains a host responsibility.

The accent is supplied at construction and can be updated in place. The dark component palette is scoped to
its own resources; no global Activity theme changes. Strings belong to GibUI in
English, French, Spanish, German, Italian and Portuguese, including accessible
names for the six directions and hue slider.

### Future application migration (not performed in this lot)

PanOSP will replace its drawable and selector methods with imports/bindings to
this API; its preference keys and other appearance controls stay local. GlGib
will bind the full config to its existing snapshot/GL renderer and backup format.
Hosts must include `colorpicker-android` when sourcing GibUI as a Gradle project.
Consumer EMP source manifests must be updated at migration, not in this lot.
No new reserved backend or placeholder code has been introduced.


### Alpha contract — correction authorized after faithful extraction

`setAlpha(x)` applies the clamped alpha once in all three modes. Solid mode carries
alpha in the Paint color; gradient colors carry alpha while Paint alpha is 255.
Geometry, directions and corner coverage are unchanged. At anti-aliased rounded
edges, geometric pixel coverage still modulates alpha as normal.

The original PanOSP extraction applied alpha in both Paint and gradient colors
(80 produced approximately 25; 128 approximately 64). This historical reference is
recorded in the extraction report. Aya authorized a separate correction before
consumer migration; it is now implemented and tested at 255, 128, 80 and 0 for solid,
linear and radial backgrounds. Alpha 255 and 0 are unchanged; translucent gradients
will be more opaque than the original PanOSP implementation at the same value.
Status remains EXPERIMENTAL. No application has been migrated by this correction.


### Dynamic accent and optional color summaries — EXPERIMENTAL

- `setAccentColor(int)` / `getAccentColor()`: preserve the constructor's ARGB
  convention (no forced opacity). The setter immediately retints the open dialog's
  selected thumbnail border, action buttons, native checked rows and hue SeekBar.
  Future dialogs and subsequent thumbnail selections use the latest value.
  Neither the component nor its dialog is recreated; selection, pending color,
  cancellation baseline and host config remain intact. No change callback fires.
- `setColor1Summary(CharSequence)` / `setColor2Summary(CharSequence)`: host-supplied
  text rendered in the existing secondary 14sp style. Null or zero-length text
  hides the summary line (GONE); both start hidden. No default explanatory strings
  or consumer-resource dependency. The second summary is retained while its whole
  row is hidden in Solid mode and reappears in Gradient mode.

All four setters/getters follow the component's main-thread contract. Accent and
summaries are presentation only: no preferences, background-config mutation or
application callback. The host supplies them again when recreating the component.
