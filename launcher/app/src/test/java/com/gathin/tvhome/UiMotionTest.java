package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UiMotionTest {
    @Test public void normalMotionUsesTheRequestedDuration() {
        assertEquals(160, UiMotion.duration(true, false, 160));
        assertEquals(220, UiMotion.duration(true, false, 220));
    }

    @Test public void animationsOffNeverAnimates() {
        assertEquals(0, UiMotion.duration(false, false, 160));
        assertEquals(0, UiMotion.duration(false, true, 160));
    }

    @Test public void reducedMotionKeepsTheseTransitionsStatic() {
        assertEquals(0, UiMotion.duration(true, true, 160));
    }
}
