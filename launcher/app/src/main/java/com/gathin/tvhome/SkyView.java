package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;

/**
 * A full-screen gradient that follows the clock: night blues, a warm horizon at sunset, a soft
 * glow that travels with the sun. Off by default (config.xml, sky_intensity = 0); a theme turns
 * it on with {@code "sky_intensity": 60}.
 *
 * Like the ambient glow, the backdrop is one small bitmap (480x270) stretched over the screen:
 * a gradient repainted at full resolution every minute is exactly the CPU cost the ambient glow
 * was rewritten to avoid on weak GPUs.
 */
public class SkyView extends View {

    /** Full-screen and faded with setAlpha: without this every redraw of Home goes through an offscreen layer. */
    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private static final int TEX_W = 480;
    private static final int TEX_H = 270;

    /** Opacity of the whole backdrop, 0 to 255, from sky_intensity. */
    private int intensity;
    private int lastMinute = -1;
    private Bitmap texture;
    private BitmapDrawable drawable;
    private Weather.Snapshot weather;
    private boolean active;
    private boolean motion;
    private boolean framePending;
    private final Paint effects = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] particles = new float[72 * 3];
    private final Shader cloud = new RadialGradient(0, 0, 1,
            new int[]{0x30FFFFFF, 0x12FFFFFF, 0x00FFFFFF},
            new float[]{0, 0.5f, 1}, Shader.TileMode.CLAMP);
    private final Shader sunlight = new RadialGradient(0, 0, 1,
            new int[]{0x40FFDFAD, 0x10FFEAC8, 0x00FFEAC8},
            new float[]{0, 0.45f, 1}, Shader.TileMode.CLAMP);
    private final Runnable frame = () -> {
        framePending = false;
        invalidate();
    };

    public SkyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        java.util.Random random = new java.util.Random(23);
        for (int index = 0; index < particles.length; index++) particles[index] = random.nextFloat();
    }

    /** {@code percent} of sky_intensity: 0 hides the backdrop, 100 covers the wallpaper. */
    public void setIntensity(int percent) {
        intensity = Math.max(0, Math.min(100, percent));
        setAlpha(weather == null ? intensity / 100f : 1f);
        setVisibility(intensity == 0 && weather == null ? GONE : VISIBLE);
        if (intensity > 0 || weather != null) render(true);
    }

    void setWeather(Weather.Snapshot snapshot) {
        boolean changed = weather != snapshot;
        weather = snapshot;
        setAlpha(weather == null ? intensity / 100f : 1f);
        setVisibility(intensity == 0 && weather == null ? GONE : VISIBLE);
        if (getVisibility() == VISIBLE) render(changed);
        if (snapshot == null) cancelFrame();
    }

    void start(boolean animate) {
        active = true;
        motion = animate;
        invalidate();
    }

    void stop() {
        active = false;
        cancelFrame();
    }

    private void cancelFrame() {
        removeCallbacks(frame);
        framePending = false;
    }

    /** Picks up a new time of day; cheap and safe to call every minute. */
    public void update() {
        if ((intensity == 0 && weather == null) || getVisibility() != VISIBLE) return;
        render(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (intensity > 0 || weather != null) render(true);
    }

    @Override protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != VISIBLE) cancelFrame();
        else invalidate();
    }

    /** Today's sunrise and sunset at the configured place, or null without one (fixed reference day). */
    @Nullable
    private int[] sunTimes() {
        String lat = getContext().getString(R.string.latitude);
        String lon = getContext().getString(R.string.longitude);
        if (lat.isEmpty() || lon.isEmpty()) return null;
        try {
            return Sun.riseAndSet(LocalDate.now(), Double.parseDouble(lat), Double.parseDouble(lon),
                    ZoneId.systemDefault());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void render(boolean force) {
        Calendar now = Calendar.getInstance();
        int minute = weather != null ? weather.paletteMinute(System.currentTimeMillis())
                : Sky.minuteOfDay(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        if (!force && minute == lastMinute) return;
        lastMinute = minute;

        int[] sun = weather == null ? sunTimes() : null;
        Sky.Palette sky = sun == null ? Sky.at(minute) : Sky.at(minute, sun[0], sun[1]);
        if (texture == null) texture = Bitmap.createBitmap(TEX_W, TEX_H, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(texture);
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float overcast = weather == null || weather.scene == Weather.Scene.CLEAR ? 0
                : weather.scene == Weather.Scene.PARTLY_CLOUDY ? 0.15f
                : weather.scene == Weather.Scene.STORM ? 0.8f : 0.55f;
        paint.setShader(new LinearGradient(0f, 0f, 0f, TEX_H,
                new int[]{Sky.lerpColor(sky.top, 0xFF111B29, overcast),
                        Sky.lerpColor(sky.mid, 0xFF283B4E, overcast),
                        Sky.lerpColor(sky.bottom, 0xFF53697C, overcast)},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, TEX_W, TEX_H, paint);

        if (sky.sun > 0 && overcast < 0.5f) {
            // A warm core low on the horizon, where the sun (or its afterglow) sits.
            int warm = Sky.lerpColor(sky.bottom, 0xFFFFD9A0, 0.65f);
            paint.setShader(new RadialGradient(
                    sky.sunX * TEX_W, TEX_H * 0.80f, TEX_W * 0.55f,
                    new int[]{(sky.sun << 24) | (warm & 0x00FFFFFF), warm & 0x00FFFFFF},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0f, 0f, TEX_W, TEX_H, paint);
        }

        if (drawable == null) {
            drawable = new BitmapDrawable(getResources(), texture);
            drawable.setGravity(Gravity.FILL);
            setBackground(drawable);
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (weather == null) return;
        canvas.save();
        canvas.scale(getWidth() / (float) TEX_W, getHeight() / (float) TEX_H);
        boolean day = weather.isDay(System.currentTimeMillis());
        float seconds = motion ? (SystemClock.uptimeMillis() % 240_000L) / 1000f : 0;
        Weather.Scene scene = weather.scene;
        effects.setShader(null);
        effects.setStyle(Paint.Style.FILL);
        if (!day && (scene == Weather.Scene.CLEAR || scene == Weather.Scene.PARTLY_CLOUDY)) {
            for (int index = 0; index < 42; index++) {
                float twinkle = 0.65f + 0.25f * (float) Math.sin(seconds * 0.7f + index);
                effects.setColor(((int) (140 * twinkle) << 24) | 0xE6EFFF);
                canvas.drawCircle(particles[index * 3] * TEX_W,
                        particles[index * 3 + 1] * 145, 0.35f + particles[index * 3 + 2] * 0.45f, effects);
            }
        }
        if (scene == Weather.Scene.CLEAR || scene == Weather.Scene.PARTLY_CLOUDY) {
            float daylight = Math.max(0, Math.min(1, (System.currentTimeMillis() - weather.sunrise)
                    / (float) (weather.sunset - weather.sunrise)));
            float sunX = day ? 220 + daylight * 180 : 355;
            float sunY = day ? 83 - 8 * (float) Math.sin(daylight * Math.PI) : 75;
            float drift = 3 * (float) Math.sin(seconds * 0.04f);
            effects.setShader(day ? sunlight : cloud);
            if (day) {
                for (int index = 0; index < 3; index++) {
                    canvas.save();
                    canvas.translate(sunX - 25 - index * 16, sunY + 90);
                    canvas.rotate(25 + index * 8 + drift);
                    canvas.scale(16 + index * 7, 165);
                    canvas.drawCircle(0, 0, 1, effects);
                    canvas.restore();
                }
            }
            canvas.save();
            canvas.translate(sunX + drift, sunY);
            float glowSize = day ? 115 + 4 * (float) Math.sin(seconds * 0.15f) : 55;
            canvas.scale(glowSize, glowSize);
            canvas.drawCircle(0, 0, 1, effects);
            canvas.restore();
            effects.setShader(null);
            effects.setColor(day ? 0x90FFE4AB : 0xCADCE7F5);
            canvas.drawCircle(sunX + drift, sunY, day ? 9 : 6, effects);
        }
        if (scene != Weather.Scene.CLEAR) {
            int count = scene == Weather.Scene.PARTLY_CLOUDY ? 5 : scene == Weather.Scene.FOG ? 12 : 10;
            effects.setShader(cloud);
            for (int index = 0; index < count; index++) {
                float width = 85 + particles[index * 3 + 2] * 80;
                float cloudX = (particles[index * 3] * 700 + seconds * (1.2f + index % 3)) % 700 - 110;
                float cloudY = scene == Weather.Scene.FOG ? 65 + index * 14 : 20 + particles[index * 3 + 1] * 110;
                canvas.save();
                canvas.translate(cloudX, cloudY);
                canvas.scale(width, scene == Weather.Scene.FOG ? 22 : 22 + index % 3 * 7);
                canvas.drawCircle(0, 0, 1, effects);
                canvas.restore();
            }
            effects.setShader(null);
        }
        if (scene == Weather.Scene.RAIN || scene == Weather.Scene.STORM || scene == Weather.Scene.SNOW) {
            boolean snow = scene == Weather.Scene.SNOW;
            effects.setColor(snow ? 0xB3E7EDF5 : 0x66BBD2E4);
            effects.setStrokeWidth(snow ? 0 : 0.6f);
            for (int index = 0; index < 72; index++) {
                float seed = particles[index * 3 + 2];
                float particleY = (particles[index * 3 + 1] * 300 + seconds * (snow ? 7 + seed * 9 : 90 + seed * 60)) % 300 - 15;
                float particleX = particles[index * 3] * TEX_W;
                if (snow) {
                    particleX += Math.sin(seconds * 0.4f + index) * 9;
                    canvas.drawCircle(particleX, particleY, 0.5f + seed, effects);
                } else {
                    canvas.drawLine(particleX, particleY, particleX - 3, particleY + 9 + seed * 7, effects);
                }
            }
        }
        canvas.restore();
        if (active && motion && isShown() && getWindowVisibility() == VISIBLE && !framePending) {
            framePending = true;
            postDelayed(frame, 50);
        }
    }
}
