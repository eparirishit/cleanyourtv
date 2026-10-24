package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppAdapterTest {

    @Test
    public void onlyAppTilesOfferDashboardActions() {
        assertTrue(AppAdapter.hasDashboardActions(AppAdapter.Entry.APP));
        assertFalse(AppAdapter.hasDashboardActions(AppAdapter.Entry.HOME_SETTINGS));
        assertFalse(AppAdapter.hasDashboardActions(AppAdapter.Entry.CARD));
    }
}
