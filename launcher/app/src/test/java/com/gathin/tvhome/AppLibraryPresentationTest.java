package com.gathin.tvhome;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AppLibraryPresentationTest {

    @Test
    public void homeBadgeOnlyAppearsForAppsAlreadyOnHome() throws Exception {
        try {
            Class<?> presentation = Class.forName("com.gathin.tvhome.AppLibraryPresentation");
            Method visible = presentation.getDeclaredMethod("showHomeBadge", boolean.class);

            assertTrue((Boolean) visible.invoke(null, true));
            assertFalse((Boolean) visible.invoke(null, false));
        } catch (ClassNotFoundException exception) {
            fail("AppLibraryPresentation must define the Home badge rule");
        }
    }

    @Test
    public void libraryTilesDoNotShowSecondaryStatusText() throws Exception {
        Class<?> presentation = Class.forName("com.gathin.tvhome.AppLibraryPresentation");
        try {
            Method visible = presentation.getDeclaredMethod("showStatus");
            assertFalse((Boolean) visible.invoke(null));
        } catch (NoSuchMethodException exception) {
            fail("AppLibraryPresentation must define the library status rule");
        }
    }

    @Test
    public void typingInTheFilterDoesNotMoveFocusIntoTheGrid() throws Exception {
        Class<?> presentation = Class.forName("com.gathin.tvhome.AppLibraryPresentation");
        try {
            Method focusGrid = presentation.getDeclaredMethod("shouldFocusGrid", boolean.class);
            assertFalse((Boolean) focusGrid.invoke(null, true));
            assertTrue((Boolean) focusGrid.invoke(null, false));
        } catch (NoSuchMethodException exception) {
            fail("AppLibraryPresentation must define the filter focus rule");
        }
    }

    @Test
    public void homeBadgeUsesACompactDiameter() throws Exception {
        Class<?> presentation = Class.forName("com.gathin.tvhome.AppLibraryPresentation");
        try {
            Method diameter = presentation.getDeclaredMethod("homeBadgeDiameterDp");
            assertEquals(22, diameter.invoke(null));
        } catch (NoSuchMethodException exception) {
            fail("AppLibraryPresentation must define the Home badge diameter");
        }
    }

    @Test
    public void focusedTileUsesASubtleZoom() throws Exception {
        Class<?> presentation = Class.forName("com.gathin.tvhome.AppLibraryPresentation");
        try {
            Method scale = presentation.getDeclaredMethod("tileScale", boolean.class);
            assertEquals(1.04f, (Float) scale.invoke(null, true), 0.001f);
            assertEquals(1f, (Float) scale.invoke(null, false), 0.001f);
        } catch (NoSuchMethodException exception) {
            fail("AppLibraryPresentation must define the tile focus scale");
        }
    }

    @Test
    public void focusedTileUsesAFrostedWhiteHighlight() throws Exception {
        String drawable = new String(Files.readAllBytes(Paths.get(
                "src/main/res/drawable/app_library_focus_ring.xml")), StandardCharsets.UTF_8);

        assertTrue(drawable.contains("#E6FFFFFF"));
        assertTrue(drawable.contains("#0DFFFFFF"));
        assertFalse(drawable.contains("#B7D2FF"));
    }

    @Test
    public void headerControlsUseTheSameFrostedWhiteFocusHighlight() throws Exception {
        String drawable = new String(Files.readAllBytes(Paths.get(
                "src/main/res/drawable/app_library_glass.xml")), StandardCharsets.UTF_8);

        assertTrue(drawable.contains("#E6FFFFFF"));
        assertFalse(drawable.contains("#B7D2FF"));
    }
}
