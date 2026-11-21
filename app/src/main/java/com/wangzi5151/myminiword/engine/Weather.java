package com.wangzi5151.myminiword.engine;

import java.util.Random;

/**
 * Lightweight local weather system: a state machine plus camera-centred rain and
 * snow particles. It writes raw vertex data so the renderer can draw everything
 * with the existing line/particle shaders (no extra GL state).
 */
public final class Weather {
    public static final int CLEAR = 0;
    public static final int RAIN = 1;
    public static final int SNOW = 2;
    public static final int STORM = 3;

    public static final String[] NAMES = {"晴朗", "小雨", "降雪", "雷雨"};

    private static final int MAX_RAIN = 480;
    private static final int MAX_SNOW = 240;
    private static final float AREA = 24f;
    private static final float[] CORNERS = {-1, -1, 1, -1, 1, 1, -1, -1, 1, 1, -1, 1};

    public volatile int type = CLEAR;
    public float wetness;
    public float flash;
    private float timer = 90f;
    private float clock;

    private final Random random = new Random(4242);
    private final float[] rainX = new float[MAX_RAIN];
    private final float[] rainY = new float[MAX_RAIN];
    private final float[] rainZ = new float[MAX_RAIN];
    private final float[] rainSpeed = new float[MAX_RAIN];
    private final float[] snowX = new float[MAX_SNOW];
    private final float[] snowY = new float[MAX_SNOW];
    private final float[] snowZ = new float[MAX_SNOW];
    private final float[] snowPhase = new float[MAX_SNOW];

    public final float[] rainVerts = new float[MAX_RAIN * 2 * 3];
    public final float[] snowVerts = new float[MAX_SNOW * 6 * 9];
    public int rainDrops;
    public int snowFlakes;

    public void setType(int t) {
        type = t < 0 || t > 3 ? CLEAR : t;
        timer = 70f + random.nextFloat() * 90f;
    }

    public void update(float dt, float cx, float cy, float cz,
                       boolean auto, int forced, boolean cold) {
        clock += dt;
        if (auto) {
            timer -= dt;
            if (timer <= 0f) {
                int next;
                float roll = random.nextFloat();
                if (cold) next = roll < 0.55f ? SNOW : (roll < 0.8f ? CLEAR : RAIN);
                else next = roll < 0.45f ? CLEAR : (roll < 0.75f ? RAIN : (roll < 0.9f ? STORM : SNOW));
                setType(next);
            }
        } else if (type != forced) {
            setType(forced);
        }

        float targetWet = type == CLEAR ? 0f : (type == SNOW ? 0.55f : 1f);
        wetness += (targetWet - wetness) * Math.min(1f, dt * 0.35f);

        if (type == STORM) {
            flash = Math.max(0f, flash - dt * 3.5f);
            if (random.nextFloat() < dt * 0.14f) flash = 1f;
        } else {
            flash = Math.max(0f, flash - dt * 6f);
        }

        boolean raining = type == RAIN || type == STORM;
        boolean snowing = type == SNOW;
        rainDrops = raining ? (type == STORM ? MAX_RAIN : MAX_RAIN / 2) : 0;
        snowFlakes = snowing ? MAX_SNOW : 0;

        float wind = type == STORM ? 7f : 2.2f;
        for (int i = 0; i < rainDrops; i++) {
            if (rainY[i] <= 0f || rainY[i] < cy - 16f
                    || Math.abs(rainX[i] - cx) > AREA * 1.6f
                    || Math.abs(rainZ[i] - cz) > AREA * 1.6f) {
                spawnRain(i, cx, cy, cz);
                continue;
            }
            rainY[i] -= rainSpeed[i] * dt;
            rainX[i] += wind * dt;
            rainZ[i] += wind * 0.35f * dt;
        }
        for (int i = 0; i < snowFlakes; i++) {
            if (snowY[i] <= 0f || snowY[i] < cy - 16f
                    || Math.abs(snowX[i] - cx) > AREA * 1.6f
                    || Math.abs(snowZ[i] - cz) > AREA * 1.6f) {
                spawnSnow(i, cx, cy, cz);
                continue;
            }
            snowY[i] -= (2.0f + (i % 5) * 0.25f) * dt;
            snowX[i] += (float) Math.sin(clock * 1.3f + snowPhase[i]) * 0.8f * dt + wind * 0.25f * dt;
            snowZ[i] += (float) Math.cos(clock * 1.1f + snowPhase[i] * 1.7f) * 0.6f * dt;
        }
    }

    private void spawnRain(int i, float cx, float cy, float cz) {
        rainX[i] = cx + (random.nextFloat() - 0.5f) * AREA * 2f;
        rainY[i] = cy + 4f + random.nextFloat() * 16f;
        rainZ[i] = cz + (random.nextFloat() - 0.5f) * AREA * 2f;
        rainSpeed[i] = 18f + random.nextFloat() * 10f;
    }

    private void spawnSnow(int i, float cx, float cy, float cz) {
        snowX[i] = cx + (random.nextFloat() - 0.5f) * AREA * 2f;
        snowY[i] = cy + 4f + random.nextFloat() * 16f;
        snowZ[i] = cz + (random.nextFloat() - 0.5f) * AREA * 2f;
        snowPhase[i] = random.nextFloat() * 6.28f;
    }

    /** Vertical rain streaks: two vertices per drop. Returns float count. */
    public int buildRain() {
        int p = 0;
        float[] v = rainVerts;
        for (int i = 0; i < rainDrops; i++) {
            float x = rainX[i], y = rainY[i], z = rainZ[i];
            v[p++] = x; v[p++] = y; v[p++] = z;
            v[p++] = x + 0.06f; v[p++] = y + 0.55f; v[p++] = z + 0.02f;
        }
        return p;
    }

    /** Snow flakes as camera-facing quads for the particle shader. Returns float count. */
    public int buildSnow(float r, float g, float b) {
        int p = 0;
        float[] v = snowVerts;
        for (int i = 0; i < snowFlakes; i++) {
            for (int c = 0; c < 6; c++) {
                v[p++] = snowX[i];
                v[p++] = snowY[i];
                v[p++] = snowZ[i];
                v[p++] = r; v[p++] = g; v[p++] = b;
                v[p++] = 0.75f;
                v[p++] = CORNERS[c * 2];
                v[p++] = CORNERS[c * 2 + 1];
            }
        }
        return p;
    }

    public boolean isPrecipitating() {
        return type == RAIN || type == SNOW || type == STORM;
    }

    public String name() {
        return NAMES[type];
    }
}
