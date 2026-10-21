package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;

final class AppLibraryNavigation {
    private AppLibraryNavigation() { }

    static ArrayList<String> snapshot(List<String> homePackages) {
        return new ArrayList<>(homePackages);
    }

    static boolean shouldReload(int requestCode, int resultCode) {
        return requestCode == AppLibraryActivity.REQUEST_CODE && resultCode == -1;
    }
}
