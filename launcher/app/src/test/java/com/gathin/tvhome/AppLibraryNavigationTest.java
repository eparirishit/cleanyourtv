package com.gathin.tvhome;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class AppLibraryNavigationTest {
    @Test public void snapshotKeepsHomePackagesInTheirExistingOrder() {
        List<String> homePackages = Arrays.asList(
                "com.netflix.mediaclient",
                "com.amazon.amazonvideo.livingroom",
                "com.apple.atve.androidtv.appletv",
                "com.google.android.youtube.tv");

        List<String> snapshot = AppLibraryNavigation.snapshot(homePackages);

        assertEquals(homePackages, snapshot);
        assertNotSame(homePackages, snapshot);
    }

    @Test public void onlyCompletedLibraryChangeRequestsHomeReload() {
        assertTrue(AppLibraryNavigation.shouldReload(AppLibraryActivity.REQUEST_CODE, ActivityResult.OK));
        assertFalse(AppLibraryNavigation.shouldReload(AppLibraryActivity.REQUEST_CODE, ActivityResult.CANCELED));
        assertFalse(AppLibraryNavigation.shouldReload(99, ActivityResult.OK));
    }

    private static final class ActivityResult {
        static final int OK = -1;
        static final int CANCELED = 0;
    }
}
