package com.wangzi5151.myminiword.world;

import java.util.Random;

/**
 * Compact Perlin/value noise toolkit used by the terrain, cave and vegetation
 * generators. Everything is deterministic for a given seed so chunks can be
 * regenerated in any order (and on any thread) and still match.
 */
public class Noise {
    private final int[] perm = new int[512];

    public Noise(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;
        Random r = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private static float grad(int hash, float x, float y) {
        switch (hash & 7) {
            case 0: return x + y;
            case 1: return -x + y;
            case 2: return x - y;
            case 3: return -x - y;
            case 4: return x;
            case 5: return -x;
            case 6: return y;
            default: return -y;
        }
    }

    private static float grad3(int hash, float x, float y, float z) {
        switch (hash & 15) {
            case 0: return x + y;
            case 1: return -x + y;
            case 2: return x - y;
            case 3: return -x - y;
            case 4: return x + z;
            case 5: return -x + z;
            case 6: return x - z;
            case 7: return -x - z;
            case 8: return y + z;
            case 9: return -y + z;
            case 10: return y - z;
            case 11: return -y - z;
            case 12: return x + y;
            case 13: return -x + y;
            case 14: return y - z;
            default: return -y - z;
        }
    }

    public float perlin2(float x, float y) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        float xf = x - (float) Math.floor(x);
        float yf = y - (float) Math.floor(y);
        float u = fade(xf);
        float v = fade(yf);
        int aa = perm[perm[xi] + yi];
        int ab = perm[perm[xi] + yi + 1];
        int ba = perm[perm[xi + 1] + yi];
        int bb = perm[perm[xi + 1] + yi + 1];
        float x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1, yf), u);
        float x2 = lerp(grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1), u);
        return lerp(x1, x2, v);
    }

    public float perlin3(float x, float y, float z) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        int zi = (int) Math.floor(z) & 255;
        float xf = x - (float) Math.floor(x);
        float yf = y - (float) Math.floor(y);
        float zf = z - (float) Math.floor(z);
        float u = fade(xf);
        float v = fade(yf);
        float w = fade(zf);

        int a = perm[xi] + yi;
        int aa = perm[a] + zi;
        int ab = perm[a + 1] + zi;
        int b = perm[xi + 1] + yi;
        int ba = perm[b] + zi;
        int bb = perm[b + 1] + zi;

        float x1 = lerp(grad3(perm[aa], xf, yf, zf), grad3(perm[ba], xf - 1, yf, zf), u);
        float x2 = lerp(grad3(perm[ab], xf, yf - 1, zf), grad3(perm[bb], xf - 1, yf - 1, zf), u);
        float y1 = lerp(x1, x2, v);

        x1 = lerp(grad3(perm[aa + 1], xf, yf, zf - 1), grad3(perm[ba + 1], xf - 1, yf, zf - 1), u);
        x2 = lerp(grad3(perm[ab + 1], xf, yf - 1, zf - 1), grad3(perm[bb + 1], xf - 1, yf - 1, zf - 1), u);
        float y2 = lerp(x1, x2, v);

        return lerp(y1, y2, w);
    }

    public float fbm2(float x, float y, int octaves, float lacunarity, float gain) {
        float sum = 0f;
        float amp = 1f;
        float freq = 1f;
        float norm = 0f;
        for (int i = 0; i < octaves; i++) {
            sum += amp * perlin2(x * freq, y * freq);
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return sum / norm;
    }

    public float fbm3(float x, float y, float z, int octaves, float lacunarity, float gain) {
        float sum = 0f;
        float amp = 1f;
        float freq = 1f;
        float norm = 0f;
        for (int i = 0; i < octaves; i++) {
            sum += amp * perlin3(x * freq, y * freq, z * freq);
            norm += amp;
            amp *= gain;
            freq *= lacunarity;
        }
        return sum / norm;
    }

    /**
     * Ridged multifractal in 0..1. Peaks are sharp, valleys are flat, which makes
     * convincing mountain crests when multiplied by an elevation mask.
     */
    public float ridged2(float x, float y, int octaves) {
        float sum = 0f;
        float amp = 0.5f;
        float freq = 1f;
        float norm = 0f;
        float prev = 1f;
        for (int i = 0; i < octaves; i++) {
            float n = 1f - Math.abs(perlin2(x * freq, y * freq));
            n *= n;
            sum += n * amp * prev;
            norm += amp;
            prev = n;
            amp *= 0.5f;
            freq *= 2f;
        }
        return norm == 0f ? 0f : sum / norm;
    }

    /** Warped fbm, useful for organic biome borders and cloud shapes. */
    public float warped2(float x, float y, float warpScale, float warpAmount, int octaves) {
        float wx = x + perlin2(x * warpScale + 11.3f, y * warpScale - 4.7f) * warpAmount;
        float wy = y + perlin2(x * warpScale - 7.9f, y * warpScale + 3.1f) * warpAmount;
        return fbm2(wx, wy, octaves, 2f, 0.5f);
    }
}
