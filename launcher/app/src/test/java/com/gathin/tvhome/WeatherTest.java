package com.gathin.tvhome;

import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class WeatherTest {
    private final long sunrise = time("2026-10-24T11:24:00Z");
    private final long sunset = time("2026-10-24T22:08:00Z");
    private final long now = time("2026-10-24T16:00:00Z");

    @Test public void unitSwitchConvertsTheSameReadingWithoutAnotherRequest() {
        Weather.Snapshot weather = snapshot(20, 0, now);
        assertEquals("68°F", weather.temperature(false));
        assertEquals("20°C", weather.temperature(true));
        assertEquals("32°F", snapshot(0, 0, now).temperature(false));
        assertEquals("-4°F", snapshot(-20, 0, now).temperature(false));
    }

    @Test public void conditionsSelectTheExpectedBackgrounds() {
        assertEquals(Weather.Scene.CLEAR, Weather.scene(0));
        assertEquals(Weather.Scene.PARTLY_CLOUDY, Weather.scene(2));
        assertEquals(Weather.Scene.CLOUDY, Weather.scene(3));
        assertEquals(Weather.Scene.FOG, Weather.scene(48));
        for (int code : new int[]{51, 56, 61, 67, 80, 82}) assertEquals(Weather.Scene.RAIN, Weather.scene(code));
        for (int code : new int[]{71, 77, 85, 86}) assertEquals(Weather.Scene.SNOW, Weather.scene(code));
        for (int code : new int[]{95, 96, 99}) assertEquals(Weather.Scene.STORM, Weather.scene(code));
    }

    @Test public void invalidReadingsCannotBecomeCurrentWeather() {
        for (double temperature : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -101, 71}) {
            try { snapshot(temperature, 0, now); fail(); }
            catch (IllegalArgumentException expected) { }
        }
        try { snapshot(20, 12, now); fail(); }
        catch (IllegalArgumentException expected) { }
        try { new Weather.Snapshot(20, 0, now, sunset, sunrise); fail(); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void cachedDataExpiresAndFutureDataIsRejected() {
        assertTrue(snapshot(20, 0, now - 15 * 60_000L).usable(now));
        assertTrue(snapshot(20, 0, now - Weather.MAX_AGE_MS).usable(now));
        assertFalse(snapshot(20, 0, now - Weather.MAX_AGE_MS - 1).usable(now));
        assertFalse(snapshot(20, 0, now + 16 * 60_000L).usable(now));
    }

    @Test public void aNewPhiladelphiaDayNeverUsesYesterdaysWeather() {
        long previousEvening = time("2026-10-24T03:55:00Z");
        long afterMidnight = time("2026-10-24T04:05:00Z");
        assertFalse(snapshot(20, 0, previousEvening).usable(afterMidnight));
        assertFalse(new Weather.Snapshot(20, 0, now, sunrise - 86_400_000L,
                sunset - 86_400_000L).usable(now));
    }

    @Test public void daylightAndPaletteFollowTodaysActualSunTimes() {
        Weather.Snapshot weather = snapshot(20, 0, now);
        assertFalse(weather.isDay(sunrise - 1));
        assertTrue(weather.isDay(sunrise));
        assertTrue(weather.isDay(sunset - 1));
        assertFalse(weather.isDay(sunset));
        assertEquals(450, weather.paletteMinute(sunrise));
        assertEquals(1140, weather.paletteMinute(sunset));
        assertEquals(0, weather.paletteMinute(sunrise - 60 * 60_000L));
        assertEquals(1400, weather.paletteMinute(sunset + 120 * 60_000L));
    }

    private Weather.Snapshot snapshot(double temperature, int code, long observedAt) {
        return new Weather.Snapshot(temperature, code, observedAt, sunrise, sunset);
    }

    private static long time(String iso) {
        return Instant.parse(iso).toEpochMilli();
    }
}
