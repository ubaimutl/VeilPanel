# License and third-party notices

## Project license

Veil Panel is a modified PanOSP derivative distributed under **GPL-3.0-or-later**, matching the upstream project’s explicit license declaration. The existing full [LICENSE](LICENSE) is preserved. This fork is not relicensed as MIT or Apache-only. Keep the corresponding source and applicable notices with releases.

[NOTICE](NOTICE) records upstream authorship, the fork baseline and modifications.

## Included components

| Component | Origin / scope | Terms and retained notice |
| --- | --- | --- |
| PanOSP | [GibDev project](https://gitlab.com/gibdev-group/gibapps/panosp), fork baseline `b59224e28d9a2a2c867dbb21bf8850a584779037` | [GPL-3.0-or-later](LICENSE) |
| GibModules ColorPicker and GibUI | Vendored snapshots under `modules/GibModules`; integrity manifests under `emp` | [Apache-2.0](modules/GibModules/LICENSE); snapshot sources/notices retained |
| AndroidLiquidGlass / Backdrop shaders | [Kyant project](https://github.com/Kyant0/AndroidLiquidGlass), pinned revision `65ab177e90e5c1d8c62e70cf7755841982da65f6` | [Apache-2.0 license](app/src/main/assets/third_party/android-liquid-glass/LICENSE.txt) and [provenance / modifications](app/src/main/assets/third_party/android-liquid-glass/NOTICE.txt) |
| Roboto | The Roboto Project Authors; bundled settings font from [Google Fonts](https://github.com/google/fonts/tree/main/ofl/roboto) | [SIL OFL 1.1 and copyright notice](app/src/main/assets/fonts/roboto/OFL.txt) |
| Josefin Sans | Josefin Sans Project Authors; bundled optional font | Full copyright and [SIL OFL 1.1 notice](app/src/main/assets/fonts/NOTICE.txt) |
| Material Components for Android | [Google Material Components](https://github.com/material-components/material-components-android), version 1.13.0, resolved by Gradle | Apache-2.0; supplied distribution notices retained |
| AndroidX ViewPager / CustomView and transitive AndroidX libraries | [Android Open Source Project](https://source.android.com/) dependencies resolved by Gradle | Apache-2.0; retain supplied distribution notices when packaging dependencies |

The Kyant code is an adaptation of its AGSL refraction/highlight shaders for Android Views. This notification build does not embed the Compose library. Existing font names and reserved-name restrictions remain applicable to the font.

Apache-2.0 components may be included in a GPLv3 project while retaining their notices; see the [Apache Software Foundation’s compatibility explanation](https://www.apache.org/licenses/GPL-compatibility). Their presence does not convert the whole application to Apache-only terms.

JUnit and Robolectric are test dependencies, not application features. Review any newly added dependency or media asset for its own license before a release. This document is a component/provenance map, not a substitute for the actual bundled licenses.

The original Veil Panel notification-stack SVG/vector mark is included under the project’s GPL-3.0-or-later license, copyright 2026 Veil Panel contributors. Third-party app icons and notification text visible in supplied demonstrations are not Veil Panel branding.
