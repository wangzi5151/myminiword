package com.wangzi5151.shahe;

public class Inventory {
    public final int[] counts = new int[64];
    private static final int MAX = 999;

    public Inventory() {
        counts[4] = 64;   // cobblestone
        counts[8] = 32;   // planks
        counts[10] = 16;  // glass
        counts[28] = 8;   // glowstone
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
            {15, 4, 23, 1}    // gravel x4 -> clay
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
