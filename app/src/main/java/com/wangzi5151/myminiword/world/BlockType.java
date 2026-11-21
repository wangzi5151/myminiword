package com.wangzi5151.myminiword.world;

import com.wangzi5151.myminiword.engine.TextureAtlas;

public class BlockType {
    public static final int FACE_PX = 0;
    public static final int FACE_NX = 1;
    public static final int FACE_PY = 2;
    public static final int FACE_NY = 3;
    public static final int FACE_PZ = 4;
    public static final int FACE_NZ = 5;

    public final int id;
    public final String name;
    public final boolean solid;
    public final boolean opaque;
    public final boolean liquid;
    public final boolean cutout;
    public final boolean cross;
    public final float hardness;

    private final int top;
    private final int bottom;
    private final int side;

    public BlockType(int id, String name, boolean solid, boolean opaque, boolean liquid, boolean cutout,
                     float hardness, int top, int bottom, int side) {
        this(id, name, solid, opaque, liquid, cutout, false, hardness, top, bottom, side);
    }

    public BlockType(int id, String name, boolean solid, boolean opaque, boolean liquid, boolean cutout,
                     boolean cross, float hardness, int top, int bottom, int side) {
        this.id = id;
        this.name = name;
        this.solid = solid;
        this.opaque = opaque;
        this.liquid = liquid;
        this.cutout = cutout;
        this.cross = cross;
        this.hardness = hardness;
        this.top = top;
        this.bottom = bottom;
        this.side = side;
    }

    public int getTile(int face) {
        switch (face) {
            case FACE_PY:
                return top;
            case FACE_NY:
                return bottom;
            default:
                return side;
        }
    }

    static final int NONE = -1;

    public static final BlockType[] BLOCKS = new BlockType[64];

    public static final BlockType AIR = register(new BlockType(0, "air", false, false, false, false, -1, NONE, NONE, NONE));
    public static final BlockType GRASS = register(new BlockType(1, "grass", true, true, false, false, 0.6f, TextureAtlas.GRASS_TOP, TextureAtlas.DIRT, TextureAtlas.GRASS_SIDE));
    public static final BlockType DIRT = register(new BlockType(2, "dirt", true, true, false, false, 0.5f, TextureAtlas.DIRT, TextureAtlas.DIRT, TextureAtlas.DIRT));
    public static final BlockType STONE = register(new BlockType(3, "stone", true, true, false, false, 1.5f, TextureAtlas.STONE, TextureAtlas.STONE, TextureAtlas.STONE));
    public static final BlockType COBBLE = register(new BlockType(4, "cobblestone", true, true, false, false, 1.6f, TextureAtlas.COBBLE, TextureAtlas.COBBLE, TextureAtlas.COBBLE));
    public static final BlockType SAND = register(new BlockType(5, "sand", true, true, false, false, 0.5f, TextureAtlas.SAND, TextureAtlas.SAND, TextureAtlas.SAND));
    public static final BlockType LOG = register(new BlockType(6, "log", true, true, false, false, 1.2f, TextureAtlas.LOG_TOP, TextureAtlas.LOG_TOP, TextureAtlas.LOG_SIDE));
    public static final BlockType LEAVES = register(new BlockType(7, "leaves", true, true, false, true, 0.25f, TextureAtlas.LEAVES, TextureAtlas.LEAVES, TextureAtlas.LEAVES));
    public static final BlockType PLANKS = register(new BlockType(8, "planks", true, true, false, false, 1.2f, TextureAtlas.PLANKS, TextureAtlas.PLANKS, TextureAtlas.PLANKS));
    public static final BlockType WATER = register(new BlockType(9, "water", false, false, true, false, -1, TextureAtlas.WATER, TextureAtlas.WATER, TextureAtlas.WATER));
    public static final BlockType GLASS = register(new BlockType(10, "glass", true, false, false, true, 0.4f, TextureAtlas.GLASS, TextureAtlas.GLASS, TextureAtlas.GLASS));
    public static final BlockType BEDROCK = register(new BlockType(11, "bedrock", true, true, false, false, -1, TextureAtlas.BEDROCK, TextureAtlas.BEDROCK, TextureAtlas.BEDROCK));
    public static final BlockType SNOW = register(new BlockType(12, "snow", true, true, false, false, 0.4f, TextureAtlas.SNOW, TextureAtlas.SNOW, TextureAtlas.SNOW));
    public static final BlockType BRICK = register(new BlockType(13, "brick", true, true, false, false, 1.8f, TextureAtlas.BRICK, TextureAtlas.BRICK, TextureAtlas.BRICK));
    public static final BlockType OBSIDIAN = register(new BlockType(14, "obsidian", true, true, false, false, 5.0f, TextureAtlas.OBSIDIAN, TextureAtlas.OBSIDIAN, TextureAtlas.OBSIDIAN));
    public static final BlockType GRAVEL = register(new BlockType(15, "gravel", true, true, false, false, 0.6f, TextureAtlas.GRAVEL, TextureAtlas.GRAVEL, TextureAtlas.GRAVEL));
    public static final BlockType COAL_ORE = register(new BlockType(16, "coal_ore", true, true, false, false, 2.0f, TextureAtlas.COAL_ORE, TextureAtlas.COAL_ORE, TextureAtlas.COAL_ORE));
    public static final BlockType IRON_ORE = register(new BlockType(17, "iron_ore", true, true, false, false, 2.2f, TextureAtlas.IRON_ORE, TextureAtlas.IRON_ORE, TextureAtlas.IRON_ORE));
    public static final BlockType GOLD_ORE = register(new BlockType(18, "gold_ore", true, true, false, false, 2.2f, TextureAtlas.GOLD_ORE, TextureAtlas.GOLD_ORE, TextureAtlas.GOLD_ORE));
    public static final BlockType DIAMOND_ORE = register(new BlockType(19, "diamond_ore", true, true, false, false, 2.5f, TextureAtlas.DIAMOND_ORE, TextureAtlas.DIAMOND_ORE, TextureAtlas.DIAMOND_ORE));
    public static final BlockType SANDSTONE = register(new BlockType(20, "sandstone", true, true, false, false, 1.4f, TextureAtlas.SANDSTONE, TextureAtlas.SANDSTONE, TextureAtlas.SANDSTONE));
    public static final BlockType MOSSY_COBBLE = register(new BlockType(21, "mossy", true, true, false, false, 1.7f, TextureAtlas.MOSSY_COBBLE, TextureAtlas.MOSSY_COBBLE, TextureAtlas.MOSSY_COBBLE));
    public static final BlockType ICE = register(new BlockType(22, "ice", true, false, false, true, 0.6f, TextureAtlas.ICE, TextureAtlas.ICE, TextureAtlas.ICE));
    public static final BlockType CLAY = register(new BlockType(23, "clay", true, true, false, false, 0.7f, TextureAtlas.CLAY, TextureAtlas.CLAY, TextureAtlas.CLAY));
    public static final BlockType IRON_BLOCK = register(new BlockType(24, "iron_block", true, true, false, false, 3.0f, TextureAtlas.IRON_BLOCK, TextureAtlas.IRON_BLOCK, TextureAtlas.IRON_BLOCK));
    public static final BlockType GOLD_BLOCK = register(new BlockType(25, "gold_block", true, true, false, false, 3.0f, TextureAtlas.GOLD_BLOCK, TextureAtlas.GOLD_BLOCK, TextureAtlas.GOLD_BLOCK));
    public static final BlockType DIAMOND_BLOCK = register(new BlockType(26, "diamond_block", true, true, false, false, 3.2f, TextureAtlas.DIAMOND_BLOCK, TextureAtlas.DIAMOND_BLOCK, TextureAtlas.DIAMOND_BLOCK));
    public static final BlockType BOOKSHELF = register(new BlockType(27, "bookshelf", true, true, false, false, 1.3f, TextureAtlas.PLANKS, TextureAtlas.PLANKS, TextureAtlas.BOOKSHELF));
    public static final BlockType GLOWSTONE = register(new BlockType(28, "glowstone", true, true, false, false, 0.6f, TextureAtlas.GLOWSTONE, TextureAtlas.GLOWSTONE, TextureAtlas.GLOWSTONE));
    public static final BlockType BIRCH_LOG = register(new BlockType(29, "birch_log", true, true, false, false, 1.2f, TextureAtlas.LOG_TOP, TextureAtlas.LOG_TOP, TextureAtlas.BIRCH_LOG_SIDE));
    public static final BlockType BIRCH_PLANKS = register(new BlockType(30, "birch_planks", true, true, false, false, 1.2f, TextureAtlas.BIRCH_PLANKS, TextureAtlas.BIRCH_PLANKS, TextureAtlas.BIRCH_PLANKS));
    public static final BlockType STONE_BRICK = register(new BlockType(31, "stone_brick", true, true, false, false, 1.8f, TextureAtlas.STONE_BRICK, TextureAtlas.STONE_BRICK, TextureAtlas.STONE_BRICK));
    public static final BlockType RAIL_X = register(new BlockType(32, "rail_x", false, false, false, true, 0.3f, TextureAtlas.RAIL_X, TextureAtlas.RAIL_X, TextureAtlas.RAIL_X));
    public static final BlockType RAIL_Z = register(new BlockType(33, "rail_z", false, false, false, true, 0.3f, TextureAtlas.RAIL_Z, TextureAtlas.RAIL_Z, TextureAtlas.RAIL_Z));
    public static final BlockType WHITE_PLASTER = register(new BlockType(34, "white_plaster", true, true, false, false, 1.0f, TextureAtlas.WHITE_PLASTER, TextureAtlas.WHITE_PLASTER, TextureAtlas.WHITE_PLASTER));
    public static final BlockType RED_PLASTER = register(new BlockType(35, "red_plaster", true, true, false, false, 1.0f, TextureAtlas.RED_PLASTER, TextureAtlas.RED_PLASTER, TextureAtlas.RED_PLASTER));
    public static final BlockType YELLOW_PLASTER = register(new BlockType(36, "yellow_plaster", true, true, false, false, 1.0f, TextureAtlas.YELLOW_PLASTER, TextureAtlas.YELLOW_PLASTER, TextureAtlas.YELLOW_PLASTER));
    public static final BlockType BLUE_PLASTER = register(new BlockType(37, "blue_plaster", true, true, false, false, 1.0f, TextureAtlas.BLUE_PLASTER, TextureAtlas.BLUE_PLASTER, TextureAtlas.BLUE_PLASTER));
    public static final BlockType DARK_PLASTER = register(new BlockType(38, "dark_plaster", true, true, false, false, 1.0f, TextureAtlas.DARK_PLASTER, TextureAtlas.DARK_PLASTER, TextureAtlas.DARK_PLASTER));

    // ---- Urban / nature extension set (ids 39..58) ----
    public static final BlockType ASPHALT = register(new BlockType(39, "asphalt", true, true, false, false, 1.0f, TextureAtlas.ASPHALT, TextureAtlas.ASPHALT, TextureAtlas.ASPHALT));
    public static final BlockType CONCRETE = register(new BlockType(40, "concrete", true, true, false, false, 1.2f, TextureAtlas.CONCRETE, TextureAtlas.CONCRETE, TextureAtlas.CONCRETE));
    public static final BlockType ROOF_TILE = register(new BlockType(41, "roof_tile", true, true, false, false, 1.2f, TextureAtlas.ROOF_TILE, TextureAtlas.ROOF_TILE, TextureAtlas.ROOF_TILE));
    public static final BlockType DARK_GLASS = register(new BlockType(42, "dark_glass", true, false, false, true, 0.6f, TextureAtlas.DARK_GLASS, TextureAtlas.DARK_GLASS, TextureAtlas.DARK_GLASS));
    public static final BlockType STEEL = register(new BlockType(43, "steel", true, true, false, false, 3.0f, TextureAtlas.STEEL, TextureAtlas.STEEL, TextureAtlas.STEEL));
    public static final BlockType HEDGE = register(new BlockType(44, "hedge", true, true, false, true, 0.5f, TextureAtlas.HEDGE, TextureAtlas.HEDGE, TextureAtlas.HEDGE));
    public static final BlockType FLOWER_RED = register(new BlockType(45, "flower_red", false, false, false, true, true, 0.05f, TextureAtlas.FLOWER_RED, TextureAtlas.FLOWER_RED, TextureAtlas.FLOWER_RED));
    public static final BlockType FLOWER_YELLOW = register(new BlockType(46, "flower_yellow", false, false, false, true, true, 0.05f, TextureAtlas.FLOWER_YELLOW, TextureAtlas.FLOWER_YELLOW, TextureAtlas.FLOWER_YELLOW));
    public static final BlockType TALL_GRASS = register(new BlockType(47, "tall_grass", false, false, false, true, true, 0.05f, TextureAtlas.TALL_GRASS, TextureAtlas.TALL_GRASS, TextureAtlas.TALL_GRASS));
    public static final BlockType CACTUS = register(new BlockType(48, "cactus", true, true, false, false, 0.5f, TextureAtlas.CACTUS_TOP, TextureAtlas.CACTUS_TOP, TextureAtlas.CACTUS));
    public static final BlockType GRANITE = register(new BlockType(49, "granite", true, true, false, false, 1.8f, TextureAtlas.GRANITE, TextureAtlas.GRANITE, TextureAtlas.GRANITE));
    public static final BlockType BASALT = register(new BlockType(50, "basalt", true, true, false, false, 2.0f, TextureAtlas.BASALT, TextureAtlas.BASALT, TextureAtlas.BASALT));
    public static final BlockType TERRACOTTA = register(new BlockType(51, "terracotta", true, true, false, false, 1.4f, TextureAtlas.TERRACOTTA, TextureAtlas.TERRACOTTA, TextureAtlas.TERRACOTTA));
    public static final BlockType ROOF_SLATE = register(new BlockType(52, "roof_slate", true, true, false, false, 1.6f, TextureAtlas.ROOF_SLATE, TextureAtlas.ROOF_SLATE, TextureAtlas.ROOF_SLATE));
    public static final BlockType LAMP = register(new BlockType(53, "lamp", true, true, false, false, 0.5f, TextureAtlas.LAMP, TextureAtlas.LAMP, TextureAtlas.LAMP));
    public static final BlockType MUD = register(new BlockType(54, "mud", true, true, false, false, 0.7f, TextureAtlas.MUD, TextureAtlas.MUD, TextureAtlas.MUD));
    public static final BlockType MOSSY_BRICK = register(new BlockType(55, "mossy_brick", true, true, false, false, 1.8f, TextureAtlas.MOSSY_BRICK, TextureAtlas.MOSSY_BRICK, TextureAtlas.MOSSY_BRICK));
    public static final BlockType FERN = register(new BlockType(56, "fern", false, false, false, true, true, 0.05f, TextureAtlas.FERN, TextureAtlas.FERN, TextureAtlas.FERN));
    public static final BlockType SPRUCE_LEAVES = register(new BlockType(57, "spruce_leaves", true, true, false, true, 0.25f, TextureAtlas.SPRUCE_LEAVES, TextureAtlas.SPRUCE_LEAVES, TextureAtlas.SPRUCE_LEAVES));
    public static final BlockType JUNGLE_LEAVES = register(new BlockType(58, "jungle_leaves", true, true, false, true, 0.25f, TextureAtlas.JUNGLE_LEAVES, TextureAtlas.JUNGLE_LEAVES, TextureAtlas.JUNGLE_LEAVES));

    public static final int MAX_ID = 58;

    // Fast lookup tables: hot loops in meshing/collision ask these millions of times.
    public static final boolean[] OPAQUE = new boolean[BLOCKS.length];
    public static final boolean[] SOLID = new boolean[BLOCKS.length];
    public static final boolean[] LIQUID = new boolean[BLOCKS.length];
    public static final boolean[] CUTOUT = new boolean[BLOCKS.length];
    public static final boolean[] CROSS = new boolean[BLOCKS.length];
    public static final float[] HARDNESS = new float[BLOCKS.length];
    /** Blocks that fake a light source by keeping their vertex shade bright. */
    public static final boolean[] GLOW = new boolean[BLOCKS.length];

    static {
        for (int i = 0; i < BLOCKS.length; i++) {
            BlockType b = BLOCKS[i];
            if (b == null) continue;
            OPAQUE[i] = b.opaque;
            SOLID[i] = b.solid;
            LIQUID[i] = b.liquid;
            CUTOUT[i] = b.cutout;
            CROSS[i] = b.cross;
            HARDNESS[i] = b.hardness;
        }
        GLOW[GLOWSTONE.id] = true;
        GLOW[LAMP.id] = true;
        GLOW[GLASS.id] = false;
        // Plants never block movement even if a future tweak marks them solid.
        for (int i = 0; i < BLOCKS.length; i++) {
            if (CROSS[i]) SOLID[i] = false;
        }
    }

    private static BlockType register(BlockType b) {
        if (b.id >= 0 && b.id < BLOCKS.length) BLOCKS[b.id] = b;
        return b;
    }

    public static BlockType get(int id) {
        if (id < 0 || id >= BLOCKS.length || BLOCKS[id] == null) return AIR;
        return BLOCKS[id];
    }

    public static boolean isOpaque(int id) {
        return id > 0 && id < OPAQUE.length && OPAQUE[id];
    }

    public static boolean isSolid(int id) {
        return id > 0 && id < SOLID.length && SOLID[id];
    }

    public static boolean isLiquid(int id) {
        return id > 0 && id < LIQUID.length && LIQUID[id];
    }

    public static boolean isCross(int id) {
        return id > 0 && id < CROSS.length && CROSS[id];
    }

    public static float hardness(int id) {
        if (id <= 0 || id >= HARDNESS.length) return -1f;
        return HARDNESS[id];
    }

    public static final int[] HOTBAR = {
            1, 3, 4, 39, 40, 8, 6, 29, 7, 5, 20, 13, 10, 53, 28, 55
    };
}
