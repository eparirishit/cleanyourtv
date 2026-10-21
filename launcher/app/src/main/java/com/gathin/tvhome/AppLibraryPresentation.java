package com.gathin.tvhome;

final class AppLibraryPresentation {
    private AppLibraryPresentation() { }

    static boolean showHomeBadge(boolean onHome) {
        return onHome;
    }

    static boolean showStatus() {
        return false;
    }

    static boolean shouldFocusGrid(boolean filterFocused) {
        return !filterFocused;
    }

    static int homeBadgeDiameterDp() {
        return 22;
    }

    static float tileScale(boolean focused) {
        return focused ? 1.04f : 1f;
    }
}
