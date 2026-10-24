# Dashboard tile long-press actions

## Intent

Let the owner hold OK on an app tile on the dashboard to either reorder the dashboard or remove that app from Home. Removal is immediate, never uninstalls or disables the app, and remains reversible from All Apps.

## Approach

The selected approach extends the dashboard tile listener so `MainActivity` owns the menu and actions. This reuses its existing glass popup, reorder mode, focus handling, and app reload flow.

Keeping the popup in `AppAdapter` was rejected because it would duplicate dashboard state and make the adapter responsible for persistence and navigation. Adding a second reorder flow was rejected because the existing mode already handles movement, saving, and cancellation.

## Behavior

- A long press on an app tile opens a two-item glass menu: **Reorder apps** and **Remove from Home**.
- **Reorder apps** begins the current reorder mode and focuses the app that opened the menu.
- **Remove from Home** immediately saves that package as hidden from Home and reloads the dashboard.
- A save failure leaves the dashboard unchanged and reports the existing failure message.
- Non-app tiles retain their current behavior.

## Validation

- Add focused unit coverage for the long-press action routing and immediate-remove rule.
- Run the launcher unit tests and release build.
- Install the APK on the projector and verify both menu actions; restore a removed app from All Apps to confirm reversibility.
