# Glass Consistency Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans for this approved native implementation.

**Goal:** Keep launcher menus readable, fully reachable, visually consistent, and respectful of motion settings.

**Architecture:** Reuse Android's anchored PopupWindow positioning and ScrollView overflow handling. Share context-menu action binding and presentation between the dashboard, Settings menu, and app library; keep their existing action ownership and lifecycle. Share material colors without adding a rendering engine.

**Tech Stack:** Existing Java, native Android views/vector drawables, JUnit.

**Spec:** User-approved seven-point Liquid Glass review in this conversation.

## Constraints
- Preserve app data, home shortcuts, wallpapers, and existing menu actions.
- No new dependencies, live blur/refraction, commits, or pushes.
- Continue the existing local checkout and its uncommitted weather/menu work.

## Review Focus
- Bottom-row and edge tiles: every menu action must remain reachable.
- Small screens/large text: constrain the popup to the visible frame and scroll overflow.
- Long-press release: must not accidentally activate the first action.
- Animations off/reduced motion: menus and library focus must stay static.
- Light artwork behind menus: labels and focus must remain distinguishable.

## Tasks
- [x] Add failing motion-policy and menu-surface/overflow checks; confirm the existing clipped App Info row.
- [x] Add a shared native menu presenter/action binder; use positive anchor offsets, explicit measured bounds, and scrolling overflow. Preserve dismissal callbacks and selected-package capture.
- [x] Add small Open, Remove from Home, and Info vectors; reuse existing icons for other actions.
- [x] Apply shared material colors to context menus, Inputs, navigation pills, library controls, and Settings choices; preserve content artwork.
- [x] Route popup/library focus transitions through the existing animation preferences; keep normal transitions restrained.
- [x] Run focused checks and the full existing unit suite; assemble release, review, and validate on the projector without changing app settings or home membership.

## Execution Record
- Baseline: app-library App Info row is only 34 pixels tall on the 1080p projector, rather than the intended 96 pixels.
- Native PopupWindow supports automatic above/below placement; no custom placement engine is needed.
- Motion and overflow checks failed before implementation. The contrast check also caught dim secondary labels in focused rows; focused labels now brighten without changing the background material.
- Review caught default smooth scrolling in ScrollView; it now follows the same motion policy as popup entrance animations.
- Release build and 156 unit tests pass. Projector verification confirms complete 96-pixel action rows above Netflix's bottom-row library tile and beside a right-edge tile; remote selection opens Netflix's correct App Info screen.
- Dashboard and Settings context menus retain all actions and visible first-action focus. No app settings or home membership were changed during verification.
- Not hardware-tested: alternate resolutions/large system font sizes and disabled-motion preferences; their overflow, contrast, and motion rules have automated checks. Live blur/refraction remains intentionally out of scope.
