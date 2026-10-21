package com.gathin.tvhome;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

public class AppLibraryLayoutTest {

    @Test
    public void tilesFitInsideEachGridCell() {
        AppLibraryLayout.Geometry geometry = AppLibraryLayout.geometry(1668, 6, 2.25f);

        assertTrue(geometry.tileWidthPx < 1668 / 6);
        assertTrue(geometry.itemHeightPx > geometry.tileHeightPx);
    }

    @Test
    public void tilesKeepTheDashboardWideAspectRatio() {
        AppLibraryLayout.Geometry geometry = AppLibraryLayout.geometry(1668, 6, 2.25f);

        assertEquals(Math.round(geometry.tileWidthPx * 101f / 180f), geometry.tileHeightPx);
    }

    @Test
    public void tilesUseTheAvailableCellWidthWithOnlyASmallGap() {
        AppLibraryLayout.Geometry geometry = AppLibraryLayout.geometry(1668, 6, 2.25f);

        assertEquals(1668 / 6 - Math.round(20 * 2.25f), geometry.tileWidthPx);
    }

    @Test
    public void appLibraryUsesFiveColumns() throws Exception {
        try {
            Method columns = AppLibraryLayout.class.getDeclaredMethod("columns");
            assertEquals(5, columns.invoke(null));
        } catch (NoSuchMethodException exception) {
            fail("AppLibraryLayout must define the app library column count");
        }
    }

    @Test
    public void rowsKeepExtraRoomBetweenTileLabels() {
        AppLibraryLayout.Geometry geometry = AppLibraryLayout.geometry(1668, 5, 2.25f);

        assertEquals(geometry.tileHeightPx + Math.round(50 * 2.25f), geometry.itemHeightPx);
    }
}
