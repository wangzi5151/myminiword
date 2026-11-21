package com.wangzi5151.myminiword.world;

/**
 * Biome table. Biomes are selected from smooth temperature/humidity fields plus
 * altitude, so borders drift naturally instead of forming straight lines.
 */
public final class Biome {
    public static final int OCEAN = 0;
    public static final int BEACH = 1;
    public static final int PLAINS = 2;
    public static final int FOREST = 3;
    public static final int BIRCH_FOREST = 4;
    public static final int TAIGA = 5;
    public static final int SNOWY_PLAINS = 6;
    public static final int DESERT = 7;
    public static final int SAVANNA = 8;
    public static final int JUNGLE = 9;
    public static final int SWAMP = 10;
    public static final int MOUNTAIN = 11;
    public static final int SNOWY_PEAKS = 12;

    public static final int TREE_NONE = 0;
    public static final int TREE_OAK = 1;
    public static final int TREE_BIRCH = 2;
    public static final int TREE_SPRUCE = 3;
    public static final int TREE_JUNGLE = 4;
    public static final int TREE_ACACIA = 5;
    public static final int TREE_PALM = 6;
    public static final int TREE_CACTUS = 7;
    public static final int TREE_SWAMP = 8;

    public final int id;
    public final String name;
    public final int top;
    public final int filler;
    public final int tree;
    public final float treeDensity;   // trees per chunk (average)
    public final float plantChance;   // chance per grass column
    public final boolean grassy;

    private Biome(int id, String name, int top, int filler, int tree,
                  float treeDensity, float plantChance, boolean grassy) {
        this.id = id;
        this.name = name;
        this.top = top;
        this.filler = filler;
        this.tree = tree;
        this.treeDensity = treeDensity;
        this.plantChance = plantChance;
        this.grassy = grassy;
    }

    public static final Biome[] ALL = new Biome[]{
            new Biome(OCEAN, "海洋", BlockType.GRAVEL.id, BlockType.SAND.id, TREE_NONE, 0f, 0f, false),
            new Biome(BEACH, "沙滩", BlockType.SAND.id, BlockType.SANDSTONE.id, TREE_PALM, 0.35f, 0.005f, false),
            new Biome(PLAINS, "平原", BlockType.GRASS.id, BlockType.DIRT.id, TREE_OAK, 1.4f, 0.16f, true),
            new Biome(FOREST, "森林", BlockType.GRASS.id, BlockType.DIRT.id, TREE_OAK, 6.5f, 0.22f, true),
            new Biome(BIRCH_FOREST, "白桦林", BlockType.GRASS.id, BlockType.DIRT.id, TREE_BIRCH, 5.5f, 0.18f, true),
            new Biome(TAIGA, "针叶林", BlockType.GRASS.id, BlockType.DIRT.id, TREE_SPRUCE, 5.0f, 0.10f, true),
            new Biome(SNOWY_PLAINS, "雪原", BlockType.SNOW.id, BlockType.DIRT.id, TREE_SPRUCE, 1.0f, 0.04f, false),
            new Biome(DESERT, "沙漠", BlockType.SAND.id, BlockType.SANDSTONE.id, TREE_CACTUS, 0.9f, 0.01f, false),
            new Biome(SAVANNA, "稀树草原", BlockType.GRASS.id, BlockType.DIRT.id, TREE_ACACIA, 1.2f, 0.12f, true),
            new Biome(JUNGLE, "丛林", BlockType.GRASS.id, BlockType.DIRT.id, TREE_JUNGLE, 7.5f, 0.35f, true),
            new Biome(SWAMP, "沼泽", BlockType.GRASS.id, BlockType.MUD.id, TREE_SWAMP, 2.4f, 0.20f, true),
            new Biome(MOUNTAIN, "山地", BlockType.STONE.id, BlockType.STONE.id, TREE_SPRUCE, 0.8f, 0.03f, false),
            new Biome(SNOWY_PEAKS, "雪峰", BlockType.SNOW.id, BlockType.STONE.id, TREE_NONE, 0f, 0f, false)
    };

    public static Biome get(int id) {
        if (id < 0 || id >= ALL.length) return ALL[PLAINS];
        return ALL[id];
    }

    /**
     * @param temp   -1..1 (cold..hot)
     * @param humid  -1..1 (dry..wet)
     * @param height surface height
     * @param sea    sea level
     */
    public static int select(float temp, float humid, int height, int sea) {
        if (height < sea - 1) return OCEAN;
        if (height <= sea + 1) return BEACH;
        boolean cold = temp < -0.25f;
        boolean cool = temp < -0.10f;
        boolean hot = temp > 0.30f;
        boolean dry = humid < -0.06f;
        boolean wet = humid > 0.20f;

        if (height > 50) return SNOWY_PEAKS;
        if (height > 42) return cold || cool ? SNOWY_PEAKS : MOUNTAIN;
        if (cold) return humid > 0.10f ? TAIGA : SNOWY_PLAINS;
        if (cool) return humid > 0.26f ? TAIGA : PLAINS;
        if (hot && dry) return DESERT;
        if (hot) return wet ? JUNGLE : SAVANNA;
        if (wet && height <= sea + 7) return SWAMP;
        if (humid > 0.30f) return BIRCH_FOREST;
        if (humid > 0.04f) return FOREST;
        return PLAINS;
    }

    /** Biome name for the HUD; falls back to 平原 for unknown ids. */
    public static String nameOf(int id) {
        return get(id).name;
    }
}
