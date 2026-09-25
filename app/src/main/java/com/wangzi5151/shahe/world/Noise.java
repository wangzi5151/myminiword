package com.wangzi5151.shahe.world;

import java.util.Random;

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
}
