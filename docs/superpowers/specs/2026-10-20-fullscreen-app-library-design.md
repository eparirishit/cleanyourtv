# Full-Screen App Library Design

## Goal

Replace CleanYourTV's small App Library popup with a remote-friendly, full-screen **All apps** screen that follows the approved minimalist prototype. The change must preserve the user's current Home layout, dock order, launcher default, app availability, and protected applications.

## Scope

- Add a dedicated `AppLibraryActivity` rather than expanding the existing popup.
- Present a dark, full-screen utility view with a back control, **All apps** heading, filter field, and a six-column app grid with generous row spacing.
- Include both Android TV (Leanback) and ordinary launcher apps. This keeps the projector's mobile Netflix package available.
- Reuse the existing explicit app-library persistence so apps manually added to the library remain visible.
- Preserve existing Home behavior:
  - Selecting an app already on Home launches it.
  - Selecting an app not on Home adds it to Home.
  - Long-press exposes Open, Add/Remove from Home, and App info.
- Back returns to the Home screen without changing focus unexpectedly.

## Interaction

- Initial focus lands on the first app tile, with the filter field directly reachable by remote navigation.
- Typing filters the displayed applications by label.
- Each grid tile uses the app's banner or icon and accessible app label. Focus uses the launcher’s existing visual language.
- The app grid scrolls vertically when required; navigation remains predictable with a D-pad remote.

## Safety And Compatibility

- Do not disable, hide, uninstall, or change any installed app package.
- Do not alter the active Home handler or recovery launcher state.
- Preserve browser, Apple TV, Desktop Launcher, StarShare, Prime Video, and the user-configured dock.
- Keep the existing App Library shortcut in the Home footer; it opens the new full-screen activity.
- Retain the old popup implementation only until the new screen is verified, then remove unreachable code in the same focused change.

## Architecture

- `MainActivity` launches `AppLibraryActivity` for App Library requests.
- Shared package discovery and explicit-library persistence move into a reusable catalog helper so Home and All Apps observe the same app list.
- The new activity owns filtering, grid rendering, focus restoration, and long-press menu presentation.
- App operations invoke the existing shortcut and application-info mechanisms rather than duplicating package-management behavior.

## Validation

- Build unit tests and release APK successfully.
- Install as an in-place update on the projector without changing the default Home setting.
- Verify Netflix appears in All apps, filtering works, remote Back returns Home, and adding/removing a Home shortcut remains reversible.
- Capture a projector screenshot for user review before treating the UI as final.
