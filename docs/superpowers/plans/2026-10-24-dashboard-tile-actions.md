# Dashboard Tile Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a glass long-press menu to dashboard app tiles for reordering or immediately removing a tile from Home.

**Architecture:** `AppAdapter` identifies real app tiles and sends their long press, with its current position and anchor, to the existing `TileListener`. `MainActivity` shows the two-row menu and reuses `beginReorder`, `focusTile`, `LibraryApps.save`, and `reloadApps`; no new persistence or reorder path is introduced.

**Tech Stack:** Java, Android TV views, `PopupWindow`, RecyclerView, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-24-dashboard-tile-actions-design.md`

## Global Constraints

- Removal immediately hides only the dashboard tile; it must not uninstall or disable the app.
- All Apps remains the restore path.
- Existing non-app-tile long-press behavior remains unchanged.
- Reuse the current glass popup and reorder system; add no dependencies.

## Review Focus

- A failed `LibraryApps.save` keeps the tile visible and reports the existing save-failure message.
- A long press during active reorder does not create a second popup or interrupt movement.
- Home Settings and module-card tiles retain their existing system app-info long press.
- Removing the focused final/only visible app leaves the dashboard in a valid focus state after reload.
- Restoring a removed app from All Apps makes it reappear on the dashboard.

---

### Task 1: Dashboard long-press action menu

**Files:**
- Modify: `launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java:51-60, 417-425`
- Modify: `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java:846-872, 2243-2338`
- Create: `launcher/app/src/test/java/com/gathin/tvhome/AppAdapterTest.java`

**Interfaces:**
- Consumes: `AppAdapter.TileListener`, `MainActivity.beginReorder()`, `MainActivity.focusTile(int)`, `LibraryApps.save(Context, String, boolean)`.
- Produces: `TileListener.onTileLongPressed(int position, View anchor)` and `AppAdapter.hasDashboardActions(int entryKind)`.

- [ ] **Step 1: Write the failing action-eligibility test**

```java
@Test public void onlyAppTilesOfferDashboardActions() {
    assertTrue(AppAdapter.hasDashboardActions(AppAdapter.Entry.APP));
    assertFalse(AppAdapter.hasDashboardActions(AppAdapter.Entry.HOME_SETTINGS));
    assertFalse(AppAdapter.hasDashboardActions(AppAdapter.Entry.CARD));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `env ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :app:testDebugUnitTest --offline --tests com.gathin.tvhome.AppAdapterTest`

Expected: FAIL because `hasDashboardActions` does not exist.

- [ ] **Step 3: Route real app-tile long presses to the dashboard controller**

Add `static boolean hasDashboardActions(int entryKind)` and `TileListener.onTileLongPressed(int, View)`. In `onBindViewHolder`, call the listener only for `Entry.APP`; leave the existing app-info handler in place for every other entry kind.

- [ ] **Step 4: Add the dashboard two-row menu**

Implement `showTileActions(int position, View anchor)` in `MainActivity` using `glassPopup` and `addSettingsMenuRow`. The first row calls a small `startReorderAt(int)` helper that reuses `beginReorder`, focuses `position`, and shows the existing reorder hint. The second row calls `LibraryApps.save(this, adapter.packageAt(position), false)` and reloads only after it succeeds. Ignore a long press while `reordering` is already true.

- [ ] **Step 5: Run the focused test to verify it passes**

Run: `env ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :app:testDebugUnitTest --offline --tests com.gathin.tvhome.AppAdapterTest`

Expected: PASS.

- [ ] **Step 6: Run the full unit suite and build the release APK**

Run: `env ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :app:testDebugUnitTest :app:assembleRelease --offline --console=plain`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Install and verify on the projector**

Verify a dashboard app long press opens both menu choices; reorder begins on that app; removal hides only that tile; add it back from All Apps; confirm a non-app tile still opens its existing app-info action.

- [ ] **Step 8: Commit**

```bash
git add launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java \
        launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java \
        launcher/app/src/test/java/com/gathin/tvhome/AppAdapterTest.java
git commit -m "Add dashboard tile actions"
```
