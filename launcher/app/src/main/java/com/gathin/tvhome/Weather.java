package com.gathin.tvhome;

import java.time.Instant;
import java.time.ZoneId;

final class Weather {
    static final ZoneId ZONE = ZoneId.of("America/New_York");
    static final long MAX_AGE_MS = 60 * 60_000L;

    enum Scene { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, RAIN, SNOW, STORM }

    private Weather() { }

    static Scene scene(int code) {
        switch (code) {
            case 0: return Scene.CLEAR;
            case 1: case 2: return Scene.PARTLY_CLOUDY;
            case 3: return Scene.CLOUDY;
            case 45: case 48: return Scene.FOG;
            case 51: case 53: case 55: case 56: case 57:
            case 61: case 63: case 65: case 66: case 67:
            case 80: case 81: case 82: return Scene.RAIN;
            case 71: case 73: case 75: case 77: case 85: case 86: return Scene.SNOW;
            case 95: case 96: case 99: return Scene.STORM;
            default: throw new IllegalArgumentException("Unknown weather code");
        }
    }

    static final class Snapshot {
        final double celsius;
        final int code;
        final Scene scene;
        final long observedAt;
        final long sunrise;
        final long sunset;

        Snapshot(double celsius, int code, long observedAt, long sunrise, long sunset) {
            if (!Double.isFinite(celsius) || celsius < -100 || celsius > 70
                    || observedAt <= 0 || sunrise <= 0 || sunset <= sunrise
                    || sunset - sunrise >= 24 * 60 * 60_000L) {
                throw new IllegalArgumentException("Invalid weather data");
            }
            this.celsius = celsius;
            this.code = code;
            this.scene = Weather.scene(code);
            this.observedAt = observedAt;
            this.sunrise = sunrise;
            this.sunset = sunset;
        }

        boolean usable(long now) {
            return now >= observedAt - 15 * 60_000L && now - observedAt <= MAX_AGE_MS
                    && date(now).equals(date(observedAt))
                    && date(now).equals(date(sunrise)) && date(now).equals(date(sunset));
        }

        boolean isDay(long now) {
            return now >= sunrise && now < sunset;
        }

        String temperature(boolean useCelsius) {
            return Math.round(useCelsius ? celsius : celsius * 9 / 5 + 32) + (useCelsius ? "°C" : "°F");
        }

        int paletteMinute(long now) {
            if (now < sunrise) {
                return now < sunrise - 45 * 60_000L ? 0
                        : 320 + (int) ((now - sunrise + 45 * 60_000L) * 130 / (45 * 60_000L));
            }
            if (now < sunset) return 450 + (int) ((now - sunrise) * 690 / (sunset - sunrise));
            return now > sunset + 90 * 60_000L ? 1400
                    : 1140 + (int) ((now - sunset) * 210 / (90 * 60_000L));
        }

        private java.time.LocalDate date(long time) {
            return Instant.ofEpochMilli(time).atZone(ZONE).toLocalDate();
        }
    }
}
