# Full-Screen App Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the App Library popup with a remote-friendly, full-screen All Apps screen while preserving every existing Home shortcut and app operation.

**Architecture:** Extract installed-app discovery and filtering into a reusable catalogue used by Home and a new `AppLibraryActivity`. The activity owns the six-column grid, filtering, focus, and action menu; `MainActivity` only opens it with a snapshot of packages currently on Home and refreshes after a saved shortcut change.

**Tech Stack:** Java 17, Android SDK 26–34, AndroidX RecyclerView, JUnit 4, Gradle 8.5.2.

**Spec:** `docs/superpowers/specs/2026-10-20-fullscreen-app-library-design.md`

## Global Constraints

- Do not disable, hide, uninstall, or change any installed app package.
- Do not alter the active Home handler or recovery launcher state.
- Preserve Browser, Apple TV, Desktop Launcher, StarShare, Prime Video, and the configured dock.
- Include Leanback and ordinary launcher apps, including `com.netflix.mediaclient`.
- Use a dark full-screen screen with back control, heading, filter, six columns, and generous vertical spacing.
- Keep press-to-open/add and long-press Open, Add/Remove from Home, and App info behavior reversible.
- Build and install only as an in-place package update; never reset launcher data.

## Review Focus

- A mobile-only app such as Netflix remains present in All Apps and can be added to Home; Task 1 test verifies dual-category inclusion and Task 3 validates it on the projector.
- A blank, mixed-case, or whitespace-padded filter does not hide valid labels unexpectedly; Task 1 test pins case-insensitive trimmed matching.
- Adding or removing a shortcut persists only that Home choice and keeps the user in All Apps; Task 2 test covers local Home-set mutation and Task 3 validates the round trip.
- Pressing Back from the grid, filter, or action menu returns to the Home footer without changing the system Home handler; Task 2 remote-navigation check and Task 3 projector validation cover this.
- A package refresh or App Info return cannot leave stale focus or a stale action menu; Task 2 lifecycle check and Task 3 return-path check cover this.

## File Structure

- Create: `launcher/app/src/main/java/com/gathin/tvhome/AppLibraryCatalog.java` — shared launchable-app discovery, ordering, and filter helpers.
- Create: `launcher/app/src/main/java/com/gathin/tvhome/AppLibraryActivity.java` — full-screen grid, selection behavior, long-press actions, and result reporting.
- Create: `launcher/app/src/main/res/layout/activity_app_library.xml` — dark full-screen header, back control, filter field, and grid host.
- Create: `launcher/app/src/main/res/layout/item_app_library.xml` — accessible, focusable application card.
- Create: `launcher/app/src/main/res/drawable/app_library_tile.xml` — normal and focused tile styling.
- Create: `launcher/app/src/test/java/com/gathin/tvhome/AppLibraryCatalogTest.java` — catalogue ordering and filter coverage.
- Modify: `launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java` — use the shared catalogue when composing Home entries.
- Modify: `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java` — launch the activity with current Home packages and refresh on `RESULT_OK`.
- Modify: `launcher/app/src/main/AndroidManifest.xml` and `launcher/app/src/main/res/values/strings.xml` — activity declaration and screen copy.
- Delete: `launcher/app/src/main/java/com/gathin/tvhome/AppLibraryDialog.java` — remove the obsolete popup once the activity is wired.

### Task 1: Share The Installed-App Catalogue

**Files:**
- Create: `launcher/app/src/main/java/com/gathin/tvhome/AppLibraryCatalog.java`
- Create: `launcher/app/src/test/java/com/gathin/tvhome/AppLibraryCatalogTest.java`
- Modify: `launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java:224-256`

**Interfaces:**
- Consumes: `PackageManager`, `ResolveInfo`, `LibraryApps`, and the current Home package set.
- Produces: `AppLibraryCatalog.Entry` with `ResolveInfo`, package, label, Leanback status, and install time.
- Produces: `AppLibraryCatalog.load(Context, Set<String>)` and `AppLibraryCatalog.filter(List<Entry>, String)`.

- [ ] **Step 1: Write failing catalogue tests**

Test that a Leanback entry wins over a duplicate normal-launcher entry, a mobile-only entry remains included, missing Home shortcuts sort before Home apps, and filtering trims input and matches labels case-insensitively.

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `./gradlew test --tests com.gathin.tvhome.AppLibraryCatalogTest`

Expected: FAIL because `AppLibraryCatalog` does not exist.

- [ ] **Step 3: Implement the shared catalogue**

Implement `load(Context context, Set<String> homePackages)` by querying both launcher categories, deduplicating by package while preferring Leanback, excluding the launcher and unexported components, sanitizing labels with `Cards.cleanLabel`, and retaining `LibraryApps.comparePriority` ordering. Adapt `AppAdapter.reload` to consume the catalogue while retaining its hidden-app, card, dock, and artwork rules.

- [ ] **Step 4: Run focused and adjacent tests**

Run: `./gradlew test --tests com.gathin.tvhome.AppLibraryCatalogTest --tests com.gathin.tvhome.LibraryAppsTest`

Expected: PASS.

- [ ] **Step 5: Commit the catalogue extraction**

Run: `git add launcher/app/src/main/java/com/gathin/tvhome/AppLibraryCatalog.java launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java launcher/app/src/test/java/com/gathin/tvhome/AppLibraryCatalogTest.java && git commit -m "Extract shared app library catalogue"`

### Task 2: Build The Full-Screen All Apps Activity

**Files:**
- Create: `launcher/app/src/main/java/com/gathin/tvhome/AppLibraryActivity.java`
- Create: `launcher/app/src/main/res/layout/activity_app_library.xml`
- Create: `launcher/app/src/main/res/layout/item_app_library.xml`
- Create: `launcher/app/src/main/res/drawable/app_library_tile.xml`
- Modify: `launcher/app/src/main/AndroidManifest.xml:41-75`
- Modify: `launcher/app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `AppLibraryCatalog`, `LibraryApps.save(Context, String, boolean)`, and `EXTRA_HOME_PACKAGES` containing packages currently shown by Home.
- Produces: `RESULT_OK` only when a shortcut choice is saved; Home uses it to reload once.

- [ ] **Step 1: Write failing interaction tests**

Extend `AppLibraryCatalogTest` to prove adding a non-Home package marks only that package as Home, removing a Home package clears only that package, and filtering after either transition retains the selected package when it still matches.

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `./gradlew test --tests com.gathin.tvhome.AppLibraryCatalogTest`

Expected: FAIL until the selection and Home-state helpers exist.

- [ ] **Step 3: Implement `AppLibraryActivity` and its layouts**

Create a private landscape activity with black background, focusable Back control, existing library heading, filter field, and vertical `RecyclerView` using `GridLayoutManager(6)`. Use dark tiles with a clear focus state and wider vertical row separation than the Home dock. Clicking opens a Home app with its preferred Leanback component or saves an absent app through `LibraryApps.save` and refreshes in place. Long press exposes Open, Add/Remove from Home, and App info. App Info refreshes on return; Back closes the action menu first, otherwise finishes to Home. Preserve focused package after filtering, saving, and lifecycle refreshes.

- [ ] **Step 4: Verify debug behavior**

Run: `./gradlew assembleDebug --offline --console=plain`

Expected: SUCCESS with no new runtime permission or exported activity. Install as an in-place update and verify six columns, filter reachability, mobile Netflix, add/remove round trip, long-press actions, Back, and App Info return using the projector remote.

- [ ] **Step 5: Commit the activity and visuals**

Run: `git add launcher/app/src/main/java/com/gathin/tvhome/AppLibraryActivity.java launcher/app/src/main/res/layout/activity_app_library.xml launcher/app/src/main/res/layout/item_app_library.xml launcher/app/src/main/res/drawable/app_library_tile.xml launcher/app/src/main/AndroidManifest.xml launcher/app/src/main/res/values/strings.xml launcher/app/src/test/java/com/gathin/tvhome/AppLibraryCatalogTest.java && git commit -m "Add full-screen app library"`

### Task 3: Replace The Popup Entry Point And Validate On The Projector

**Files:**
- Modify: `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java:213,258-263,1053-1067`
- Delete: `launcher/app/src/main/java/com/gathin/tvhome/AppLibraryDialog.java`
- Create: `launcher/app/src/test/java/com/gathin/tvhome/AppLibraryNavigationTest.java`

**Interfaces:**
- Consumes: `AppLibraryActivity.EXTRA_HOME_PACKAGES`, `AppLibraryActivity.REQUEST_CODE`, and its `RESULT_OK` contract.
- Produces: Home footer navigation that starts the full-screen activity and restores focus to the App Library button after return.

- [ ] **Step 1: Write the failing Home handoff test**

Add a package-private handoff helper with a test that proves a Home package snapshot is passed unchanged and `RESULT_OK` requests one reload while a canceled return does not.

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `./gradlew test --tests com.gathin.tvhome.AppLibraryNavigationTest`

Expected: FAIL because the activity-result handoff does not yet exist.

- [ ] **Step 3: Replace `AppLibraryDialog` with the activity-result handoff**

Remove popup state, refresh calls, and popup-focus callbacks from `MainActivity`. Start `AppLibraryActivity` from the existing footer with the current `AppAdapter` package snapshot. Reload and restore footer focus only after `RESULT_OK`; a normal Back return restores focus without changing shortcuts. Delete the old dialog after this is the sole entry point.

- [ ] **Step 4: Run full verification and projector acceptance**

Run: `./gradlew test assembleRelease --offline --console=plain`

Expected: SUCCESS. Install `app/build/outputs/apk/release/app-release.apk` as an in-place update. Confirm via ADB that `com.gathin.tvhome/.MainActivity` remains the Home activity, then verify All Apps opens full-screen, Netflix appears, six-column remote navigation and filtering work, long-press actions work, Back returns to the footer, and the dock remains `Netflix → Prime Video → Apple TV → YouTube`.

- [ ] **Step 5: Capture, commit, and push the verified feature**

Save a projector screenshot for user review, then run: `git add launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java launcher/app/src/test/java/com/gathin/tvhome/AppLibraryNavigationTest.java && git rm launcher/app/src/main/java/com/gathin/tvhome/AppLibraryDialog.java && git commit -m "Open app library as full screen" && git push origin main`

## Self-Review

- **Spec coverage:** Task 1 preserves TV and ordinary app discovery; Task 2 implements the approved full-screen visual and actions; Task 3 wires Home navigation, removes the popup, and validates the projector update.
- **Step scan:** Each task has a failing test, one clear implementation step, focused verification, and a scoped commit. UI behavior has a named remote validation because this project has JVM tests only.
- **Type consistency:** `AppLibraryCatalog.Entry`, `load(Context, Set<String>)`, `filter(List<Entry>, String)`, `EXTRA_HOME_PACKAGES`, and `RESULT_OK` are defined once and reused consistently.
- **Review focus:** All five likely failure paths have an owning automated or projector validation step.
- **Proportion:** The plan locks decisions without prescribing implementation line by line.
