package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

final class WeatherController {
    static final String ENABLED_KEY = "weather_enabled";
    private static final String ENDPOINT = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=39.9526&longitude=-75.1652&current=temperature_2m,weather_code"
            + "&daily=sunrise,sunset&forecast_days=1&timezone=America%2FNew_York&timeformat=unixtime";
    private static final long REFRESH_MS = 15 * 60_000L;
    private final SharedPreferences choices;
    private final SharedPreferences cache;
    private final Consumer<Weather.Snapshot> listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private Weather.Snapshot snapshot;
    private boolean active;
    private boolean closed;
    private boolean loading;
    private long nextRequest;

    WeatherController(Context context, Consumer<Weather.Snapshot> listener) {
        this.listener = listener;
        choices = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        cache = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE);
        try {
            snapshot = decode(cache.getString("payload", ""));
            long age = System.currentTimeMillis() - cache.getLong("fetched_at", 0);
            if (age >= 0 && age < REFRESH_MS && snapshot.usable(System.currentTimeMillis())) {
                nextRequest = SystemClock.elapsedRealtime() + REFRESH_MS - age;
            }
        } catch (Exception invalid) {
            snapshot = null;
        }
    }

    void start() {
        active = true;
        update();
    }

    void stop() {
        active = false;
    }

    void close() {
        active = false;
        closed = true;
        worker.shutdownNow();
    }

    void update() {
        if (!active || closed) return;
        if (!choices.getBoolean(ENABLED_KEY, true)) {
            listener.accept(null);
            return;
        }
        if (snapshot != null && !snapshot.usable(System.currentTimeMillis())) {
            snapshot = null;
            nextRequest = 0;
        }
        publish();
        if (loading || SystemClock.elapsedRealtime() < nextRequest) return;
        loading = true;
        worker.execute(() -> {
            Weather.Snapshot result = null;
            String payload = null;
            try {
                payload = fetch();
                result = decode(payload);
                if (!result.usable(System.currentTimeMillis())) throw new IllegalArgumentException("Stale weather");
            } catch (Exception failure) {
                result = null;
                Log.w("TvHome.Weather", "Current weather unavailable", failure);
            }
            Weather.Snapshot next = result;
            String raw = payload;
            main.post(() -> {
                loading = false;
                if (closed) return;
                nextRequest = SystemClock.elapsedRealtime() + (next != null ? REFRESH_MS : 5 * 60_000L);
                if (next != null) {
                    snapshot = next;
                    cache.edit().putString("payload", raw).putLong("fetched_at", System.currentTimeMillis()).apply();
                }
                if (active) publish();
            });
        });
    }

    private void publish() {
        listener.accept(snapshot != null && snapshot.usable(System.currentTimeMillis()) ? snapshot : null);
    }

    private static Weather.Snapshot decode(String payload) throws Exception {
        if (payload.length() > 32_768) throw new IllegalArgumentException("Oversized weather response");
        JSONObject response = new JSONObject(payload);
        if (!"°C".equals(response.getJSONObject("current_units").getString("temperature_2m"))) {
            throw new IllegalArgumentException("Unexpected temperature units");
        }
        JSONObject current = response.getJSONObject("current");
        JSONObject daily = response.getJSONObject("daily");
        return new Weather.Snapshot(current.getDouble("temperature_2m"), current.getInt("weather_code"),
                Math.multiplyExact(current.getLong("time"), 1000L),
                Math.multiplyExact(daily.getJSONArray("sunrise").getLong(0), 1000L),
                Math.multiplyExact(daily.getJSONArray("sunset").getLong(0), 1000L));
    }

    private static String fetch() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setInstanceFollowRedirects(false);
        try {
            if (connection.getResponseCode() != 200) throw new java.io.IOException("Weather HTTP error");
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted() || bytes.size() + count > 32_768) {
                        throw new java.io.IOException("Weather response interrupted or oversized");
                    }
                    bytes.write(buffer, 0, count);
                }
                return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }
}
