# Dashboard app actions

Hold OK or press Menu on a dashboard app to open **Reorder apps**, **Remove from Home**,
or **App info**. Dock actions also appear when the dock is enabled.

Reorder immediately picks up the selected app. Use the arrow buttons to move it;
Left and Right stop at the row edges. Press OK or Back once to finish and save.
The selected app stays highlighted while the others dim. Dock guidance appears
beside the app name; lower grid rows use compact footer guidance. The controls
reuse the Back icon and two small vector assets, with no icon-pack dependency.

Remove from Home immediately hides the app's shortcut. The app remains installed
and can be restored from All Apps. If saving the choice fails, the shortcut
stays visible and the launcher reports the error.

App info opens the selected app's Android settings page without changing its settings.

Dashboard app menus, the Settings long-press menu, and app-library menus share a rounded,
translucent graphite surface with a silver focus highlight and white text. The treatment uses
native drawables, with no blur renderer or additional dependencies.

Action icons reuse existing launcher vectors alongside three small Open, Remove from Home, and
Info vectors. Android positions the menu above or below its app tile according to available space;
oversized menus scroll rather than hiding actions at the display edge. Opening a menu does not
activate its first action when the long-press is released.

Animations off and Reduced motion disable popup transitions and smooth overflow scrolling.
The same motion policy applies to library focus zoom. Navigation pills, Inputs, library controls,
and Settings focus share the glass palette; selected settings keep their existing state markers.

Module cards use the upstream reorder behavior; the default-home shortcut is not movable. The Settings menu's multi-app reorder and Edit dock flows remain available.


Fork design history: [tile actions design](superpowers/specs/2026-10-24-dashboard-tile-actions-design.md),
[tile actions plan](superpowers/plans/2026-10-24-dashboard-tile-actions.md), and
[glass consistency plan](superpowers/plans/2026-10-24-glass-consistency.md).
