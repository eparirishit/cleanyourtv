package com.gathin.tvhome;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppLibraryCatalogTest {
    private static final long NOW = 100L * 24 * 60 * 60 * 1000;

    @Test public void leanbackEntryWinsWhenPackageAlsoHasOrdinaryLauncherEntry() {
        AppLibraryCatalog.Entry leanback = new AppLibraryCatalog.Entry(
                "com.example.video", "Video", true, NOW - 10);
        AppLibraryCatalog.Entry ordinary = new AppLibraryCatalog.Entry(
                "com.example.video", "Video phone", false, NOW - 10);

        List<AppLibraryCatalog.Entry> catalog = AppLibraryCatalog.mergeAndSort(
                Collections.singletonList(leanback), Collections.singletonList(ordinary),
                Collections.emptySet(), NOW);

        assertEquals(1, catalog.size());
        assertTrue(catalog.get(0).leanback);
        assertEquals("Video", catalog.get(0).label);
    }

    @Test public void ordinaryLauncherOnlyAppRemainsInCatalog() {
        AppLibraryCatalog.Entry mobileNetflix = new AppLibraryCatalog.Entry(
                "com.netflix.mediaclient", "Netflix", false, NOW - 10);

        List<AppLibraryCatalog.Entry> catalog = AppLibraryCatalog.mergeAndSort(
                Collections.emptyList(), Collections.singletonList(mobileNetflix),
                Collections.emptySet(), NOW);

        assertEquals(1, catalog.size());
        assertEquals("com.netflix.mediaclient", catalog.get(0).packageName);
        assertFalse(catalog.get(0).leanback);
    }

    @Test public void missingHomeShortcutSortsBeforeHomeApp() {
        AppLibraryCatalog.Entry missing = new AppLibraryCatalog.Entry(
                "com.example.browser", "Browser", true, NOW - 10);
        AppLibraryCatalog.Entry home = new AppLibraryCatalog.Entry(
                "com.example.video", "Video", true, NOW - 10);

        List<AppLibraryCatalog.Entry> catalog = AppLibraryCatalog.mergeAndSort(
                Arrays.asList(home, missing), Collections.emptyList(),
                new HashSet<>(Collections.singletonList(home.packageName)), NOW);

        assertEquals(missing.packageName, catalog.get(0).packageName);
        assertEquals(home.packageName, catalog.get(1).packageName);
    }

    @Test public void filterTrimsAndMatchesLabelsWithoutCaseSensitivity() {
        List<AppLibraryCatalog.Entry> catalog = Arrays.asList(
                new AppLibraryCatalog.Entry("com.netflix.mediaclient", "Netflix", false, NOW),
                new AppLibraryCatalog.Entry("com.amazon.amazonvideo.livingroom", "Prime Video", true, NOW));

        List<AppLibraryCatalog.Entry> matches = AppLibraryCatalog.filter(catalog, "  NET  ");

        assertEquals(1, matches.size());
        assertEquals("com.netflix.mediaclient", matches.get(0).packageName);
    }

    @Test public void changingHomeStateOnlyUpdatesTheSelectedPackage() {
        String existingHome = "com.example.video";
        String addedHome = "com.netflix.mediaclient";
        HashSet<String> original = new HashSet<>(Collections.singletonList(existingHome));

        HashSet<String> afterAdding = AppLibraryCatalog.withHomeState(original, addedHome, true);
        HashSet<String> afterRemoving = AppLibraryCatalog.withHomeState(afterAdding, existingHome, false);

        assertTrue(afterAdding.contains(existingHome));
        assertTrue(afterAdding.contains(addedHome));
        assertFalse(afterRemoving.contains(existingHome));
        assertTrue(afterRemoving.contains(addedHome));
        assertTrue(original.contains(existingHome));
        assertFalse(original.contains(addedHome));
    }

    @Test public void filterKeepsSelectedPackageWhenItStillMatches() {
        List<AppLibraryCatalog.Entry> catalog = Arrays.asList(
                new AppLibraryCatalog.Entry("com.netflix.mediaclient", "Netflix", false, NOW),
                new AppLibraryCatalog.Entry("com.amazon.amazonvideo.livingroom", "Prime Video", true, NOW));
        List<AppLibraryCatalog.Entry> matches = AppLibraryCatalog.filter(catalog, "net");

        assertEquals("com.netflix.mediaclient", AppLibraryCatalog.selectionAfterFilter(
                matches, "com.netflix.mediaclient"));
    }
}
