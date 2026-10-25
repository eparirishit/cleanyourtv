# Dashboard app actions

Hold OK on a dashboard app to open **Reorder apps**, **Remove from Home**, or **App info**.

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

Module cards and the default-home shortcut retain their existing long-press
behavior. The Settings menu's multi-app reorder and Edit dock flows remain available.
