package com.wangzi5151.myminiword;

public class Inventory {
    public final int[] counts = new int[64];
    private static final int MAX = 999;

    public Inventory() {
        counts[4] = 64;    // cobblestone
        counts[8] = 48;    // planks
        counts[10] = 32;   // glass
        counts[28] = 16;   // glowstone
        counts[39] = 64;   // asphalt
        counts[40] = 64;   // concrete
        counts[43] = 32;   // steel
        counts[53] = 16;   // lamp
        counts[44] = 24;   // hedge
        counts[45] = 12;   // red flower
        counts[47] = 24;   // tall grass
    }

    public int count(int id) {
        if (id < 0 || id >= counts.length) return 0;
        return counts[id];
    }

    public void add(int id, int n) {
        if (id <= 0 || id >= counts.length) return;
        counts[id] = Math.min(MAX, counts[id] + n);
    }

    public boolean has(int id, int n) {
        return count(id) >= n;
    }

    public boolean remove(int id, int n) {
        if (id <= 0 || id >= counts.length) return false;
        if (counts[id] < n) return false;
        counts[id] -= n;
        return true;
    }

    // {inputId, inputCount, outputId, outputCount}
    public static final int[][] RECIPES = {
            {6, 1, 8, 4},     // log -> planks x4
            {29, 1, 30, 4},   // birch log -> birch planks x4
            {5, 4, 10, 1},    // sand x4 -> glass
            {4, 4, 3, 1},     // cobble x4 -> stone
            {4, 4, 32, 1},    // cobble x4 -> stone brick
            {3, 1, 4, 1},     // stone -> cobble
            {17, 4, 24, 1},   // iron ore x4 -> iron block
            {18, 4, 25, 1},   // gold ore x4 -> gold block
            {19, 4, 26, 1},   // diamond ore x4 -> diamond block
            {16, 4, 28, 1},   // coal ore x4 -> glowstone
            {15, 4, 23, 1},   // gravel x4 -> clay
            {15, 2, 39, 4},   // gravel x2 -> asphalt x4
            {23, 2, 40, 4},   // clay x2 -> concrete x4
            {23, 2, 51, 2},   // clay x2 -> terracotta x2
            {51, 2, 41, 4},   // terracotta x2 -> roof tiles x4
            {3, 2, 52, 2},    // stone x2 -> roof slate x2
            {7, 2, 44, 2},    // leaves x2 -> hedge x2
            {28, 1, 53, 2}    // glowstone -> lamp x2
    };

    public boolean canCraft(int recipeIndex) {
        int[] r = RECIPES[recipeIndex];
        return has(r[0], r[1]);
    }

    public boolean craft(int recipeIndex) {
        int[] r = RECIPES[recipeIndex];
        if (!remove(r[0], r[1])) return false;
        add(r[2], r[3]);
        return true;
    }
}
