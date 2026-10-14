# Native Miuix design adaptation

Upstream: https://github.com/miuix-kotlin-multiplatform/miuix
Revision: 6ef604ae6d53e4b241cdb89ccbf2caf86a7a33d0 (0.9.4)
License: Apache-2.0; full license in assets/licenses/miuix/LICENSE.txt.

The app remains Java / Android Views. No Compose, Kotlin UI runtime, vendored
Miuix modules or additional Gradle dependencies are included.

| Native implementation | Upstream reference | Parameters |
| --- | --- | --- |
| MiuixSurfaceDrawable | SquirclePath.kt | extension 1.1, cubic control 0.643; radius clamped to half the shorter side |
| UiKit buttons | Button.kt / TextStyles.kt | radius 16 dp, min height 40 dp, horizontal inset 16 dp, default text 17 sp |
| Card surfaces | Card.kt | radius 16 dp |
| Settings rows | Component.kt | inset 16 dp, title 17 sp |
| Native dropdown | Dropdown.kt / ListPopup.kt | radius 16 dp, horizontal inset 20 dp, min row height 56 dp, selected check 20 dp |
| Month/year tabs | TabRow.kt / TabRowWithContour | outer radius 13 dp, inner radius 8 dp, contour 5 dp, height 45 dp |
| MiuixSwitch | Switch.kt | capsule 49 × 28 dp, thumb diameter 20 dp, positions 4/25 dp, pressed scale 1.127 |
| Dialog surfaces | DialogContentLayout.kt | radius 32 dp; compact date/year picker width retained at user's request |
| Default palette | Colors.kt | light primary #3482FF, dark primary #277AF7; neutral surfaces and alpha text roles |

Calendar cell sizing, traditional date content and forward-only paper tear are
app-specific. Four optional accent palettes remain available. Compact toolbar
widths adapt below 360 dp; the date picker retains the user's compact actions.

Device screenshots and interaction verification are still required to confirm
visual parity on the target device. Source measurements alone cannot establish
pixel-identical rendering across fonts, density and HyperOS versions.

User-requested compact sizing (2026-10-14): settings rows 56 dp with 8 dp
vertical insets; row controls 36 dp; top tabs 40 dp and Today 80 × 36 dp.
Today has text only in both modes. Settings values disable horizontal scrolling
so single-line auto sizing measures their actual available width.

## Tear calendar photo background
The selected background now fills the tear-calendar screen. A dark veil and translucent native surfaces preserve label contrast. Image decoding runs outside the UI thread, is cached by background revision/size, rejects stale results, and releases bitmaps on detach. The tear snapshot includes the photo portion of the sheet. Clearing or losing access to the photo restores the normal palette.
