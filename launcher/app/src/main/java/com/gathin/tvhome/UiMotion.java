package com.gathin.tvhome;

final class UiMotion {
    private UiMotion() { }

    static int duration(boolean animations, boolean reducedMotion, int normalMillis) {
        return animations && !reducedMotion ? normalMillis : 0;
    }
}
