# Calendar paper animation references

The custom Canvas mesh in `PaperTearGeometry.java` and `MinimalCalendarView.java`
was implemented for this app. It does not import the libraries or copy their code.

Design reference: https://github.com/eschao/android-PageFlip (Apache-2.0).
Its README describes cylindrical curling, a shaded folded back, and edge/base shadows.
The calendar adds its own ragged binding, paper scraps, gravity fall and forward-only navigation.

Settings grouping was checked against compose-miuix-ui/miuix Divider.kt.
The settings screen uses spacing without separators for a quieter group appearance.

## Revised single-sheet effect
The current renderer has no debris, particles, jagged edge clipping or separate back-face overlay. A continuous cylinder curl follows the drag, then the entire sheet falls with a gentle tilt and a matching soft shadow. The sheet stays opaque and leaves the viewport physically. The geometry test also checks monotonically ordered mesh rows to reject self-intersections.
