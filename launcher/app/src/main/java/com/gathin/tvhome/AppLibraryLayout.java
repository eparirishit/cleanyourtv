package com.gathin.tvhome;

final class AppLibraryLayout {
    private static final int COLUMNS = 5;
    private static final float ITEM_EXTRA_DP = 50f;

    private AppLibraryLayout() { }

    static int columns() {
        return COLUMNS;
    }

    static Geometry geometry(int gridWidthPx, int columns, float density) {
        int cellWidth = columns <= 0 ? gridWidthPx : gridWidthPx / columns;
        int gap = Math.max(1, Math.round(20 * density));
        int tileWidth = Math.max(1, cellWidth - gap);
        int tileHeight = AppAdapter.tileHeightPx(tileWidth);
        return new Geometry(tileWidth, tileHeight, tileHeight + Math.round(ITEM_EXTRA_DP * density));
    }

    static final class Geometry {
        final int tileWidthPx;
        final int tileHeightPx;
        final int itemHeightPx;

        Geometry(int tileWidthPx, int tileHeightPx, int itemHeightPx) {
            this.tileWidthPx = tileWidthPx;
            this.tileHeightPx = tileHeightPx;
            this.itemHeightPx = itemHeightPx;
        }
    }
}
