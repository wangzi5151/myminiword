package com.wangzi5151.myminiword.engine;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class TextureAtlas {
    public static final int TILE_PX = 32;
    public static final int TILES = 16;        // grid columns
    public static final int TILE_COUNT = 64;   // number of tiles (4 atlas rows)
    public static final int ATLAS_PX = TILE_PX * TILES;
    private static final float INSET = 0.5f / ATLAS_PX;

    public static final int GRASS_TOP = 0;
    public static final int GRASS_SIDE = 1;
    public static final int DIRT = 2;
    public static final int STONE = 3;
    public static final int COBBLE = 4;
    public static final int SAND = 5;
    public static final int LOG_SIDE = 6;
    public static final int LOG_TOP = 7;
    public static final int LEAVES = 8;
    public static final int PLANKS = 9;
    public static final int WATER = 10;
    public static final int GLASS = 11;
    public static final int BEDROCK = 12;
    public static final int SNOW = 13;
    public static final int BRICK = 14;
    public static final int OBSIDIAN = 15;
    public static final int GRAVEL = 16;
    public static final int COAL_ORE = 17;
    public static final int IRON_ORE = 18;
    public static final int GOLD_ORE = 19;
    public static final int DIAMOND_ORE = 20;
    public static final int SANDSTONE = 21;
    public static final int MOSSY_COBBLE = 22;
    public static final int ICE = 23;
    public static final int CLAY = 24;
    public static final int IRON_BLOCK = 25;
    public static final int GOLD_BLOCK = 26;
    public static final int DIAMOND_BLOCK = 27;
    public static final int BOOKSHELF = 28;
    public static final int GLOWSTONE = 29;
    public static final int BIRCH_LOG_SIDE = 30;
    public static final int BIRCH_PLANKS = 31;
    public static final int STONE_BRICK = 32;
    public static final int RAIL_X = 33;
    public static final int RAIL_Z = 34;
    public static final int WHITE_PLASTER = 35;
    public static final int RED_PLASTER = 36;
    public static final int YELLOW_PLASTER = 37;
    public static final int BLUE_PLASTER = 38;
    public static final int DARK_PLASTER = 39;
    // ---- urban / nature extension tiles ----
    public static final int ASPHALT = 40;
    public static final int CONCRETE = 41;
    public static final int ROOF_TILE = 42;
    public static final int DARK_GLASS = 43;
    public static final int STEEL = 44;
    public static final int HEDGE = 45;
    public static final int FLOWER_RED = 46;
    public static final int FLOWER_YELLOW = 47;
    public static final int TALL_GRASS = 48;
    public static final int CACTUS = 49;
    public static final int CACTUS_TOP = 50;
    public static final int GRANITE = 51;
    public static final int BASALT = 52;
    public static final int TERRACOTTA = 53;
    public static final int ROOF_SLATE = 54;
    public static final int LAMP = 55;
    public static final int MUD = 56;
    public static final int MOSSY_BRICK = 57;
    public static final int FERN = 58;
    public static final int SPRUCE_LEAVES = 59;
    public static final int JUNGLE_LEAVES = 60;
    public static final int TILE_MAX = 60;

    private int textureId = 0;
    private int cloudTextureId = 0;
    private final int[][] tiles = new int[TILE_COUNT][TILE_PX * TILE_PX];
    private final int[] atlasPixels = new int[ATLAS_PX * ATLAS_PX];
    private int[] cloudPixels;

    public TextureAtlas() {
        build();
        compose();
        buildClouds();
    }

    private void set(int t, int x, int y, int c) {
        if (x < 0 || y < 0 || x >= TILE_PX || y >= TILE_PX) return;
        tiles[t][y * TILE_PX + x] = c;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private static int rgb(int r, int g, int b) {
        return 0xff000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int rgba(int r, int g, int b, int a) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private void fillTile(int t, int c) {
        int[] p = tiles[t];
        for (int i = 0; i < p.length; i++) p[i] = c;
    }

    private void noiseTile(int t, int base, int amount, Random r) {
        int br = (base >> 16) & 0xff, bg = (base >> 8) & 0xff, bb = base & 0xff;
        int ba = (base >>> 24) & 0xff;
        for (int y = 0; y < TILE_PX; y++)
            for (int x = 0; x < TILE_PX; x++) {
                int d = r.nextInt(amount * 2 + 1) - amount;
                set(t, x, y, rgba(br + d, bg + d, bb + d, ba));
            }
    }

    private void build() {
        Random r = new Random(1337);

        noiseTile(GRASS_TOP, rgb(88, 156, 50), 22, r);
        for (int i = 0; i < 60; i++) {
            int x = r.nextInt(TILE_PX), y = r.nextInt(TILE_PX);
            set(GRASS_TOP, x, y, rgb(64 + r.nextInt(30), 130 + r.nextInt(40), 40 + r.nextInt(25)));
        }
        noiseTile(DIRT, rgb(134, 96, 67), 18, r);
        noiseTile(STONE, rgb(128, 128, 130), 14, r);
        for (int i = 0; i < 10; i++) {
            int x = r.nextInt(TILE_PX), y = r.nextInt(TILE_PX);
            set(STONE, x, y, rgb(96, 96, 100));
        }
        noiseTile(COBBLE, rgb(116, 116, 118), 10, r);
        for (int i = 0; i < 22; i++) {
            int bx = r.nextInt(TILE_PX - 6), by = r.nextInt(TILE_PX - 6);
            int w = 4 + r.nextInt(6), h = 4 + r.nextInt(6);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    set(COBBLE, bx + x, by + y, rgb(84, 84, 86));
        }
        noiseTile(SAND, rgb(222, 210, 165), 12, r);
        noiseTile(SNOW, rgb(242, 244, 250), 7, r);

        fillTile(GRASS_SIDE, rgb(134, 96, 67));
        for (int y = 0; y < TILE_PX; y++)
            for (int x = 0; x < TILE_PX; x++) {
                int d = r.nextInt(37) - 18;
                set(GRASS_SIDE, x, y, rgb(134 + d, 96 + d, 67 + d));
            }
        for (int x = 0; x < TILE_PX; x++) {
            int h = 6 + r.nextInt(4);
            for (int y = 0; y < h; y++) {
                int d = r.nextInt(41) - 20;
                set(GRASS_SIDE, x, y, rgb(88 + d, 156 + d, 50 + d));
            }
        }

        fillTile(LOG_SIDE, rgb(104, 78, 48));
        for (int x = 0; x < TILE_PX; x++) {
            if (x % 9 == 0 || x % 13 == 5) {
                for (int y = 0; y < TILE_PX; y++) {
                    int d = r.nextInt(21) - 10;
                    set(LOG_SIDE, x, y, rgb(78 + d, 56 + d, 33 + d));
                }
            } else {
                for (int y = 0; y < TILE_PX; y++) {
                    int d = r.nextInt(17) - 8;
                    set(LOG_SIDE, x, y, rgb(104 + d, 78 + d, 48 + d));
                }
            }
        }
        fillTile(LOG_TOP, rgb(164, 130, 82));
        for (int y = 0; y < TILE_PX; y++)
            for (int x = 0; x < TILE_PX; x++) {
                int dx = x - TILE_PX / 2, dy = y - TILE_PX / 2;
                int dist = (int) Math.sqrt(dx * dx + dy * dy);
                if (dist % 3 == 0) set(LOG_TOP, x, y, rgb(126, 96, 58));
                else if (dist % 3 == 1) set(LOG_TOP, x, y, rgb(150, 118, 74));
            }
        set(LOG_TOP, TILE_PX / 2, TILE_PX / 2, rgb(90, 66, 40));

        fillTile(LEAVES, rgba(58, 118, 40, 255));
        for (int y = 0; y < TILE_PX; y++)
            for (int x = 0; x < TILE_PX; x++) {
                int d = r.nextInt(61) - 30;
                if (r.nextInt(100) < 18) set(LEAVES, x, y, rgba(0, 0, 0, 0));
                else set(LEAVES, x, y, rgba(58 + d, 118 + d, 40 + d, 255));
            }
        for (int i = 0; i < 22; i++) {
            set(LEAVES, r.nextInt(TILE_PX), r.nextInt(TILE_PX), rgba(24, 56, 20, 255));
        }

        fillTile(PLANKS, rgb(170, 133, 78));
        for (int y = 0; y < TILE_PX; y++)
            for (int x = 0; x < TILE_PX; x++) {
                if (y % 8 == 0) set(PLANKS, x, y, rgb(112, 84, 46));
                else {
                    int d = r.nextInt(21) - 10;
                    set(PLANKS, x, y, rgb(170 + d, 133 + d, 78 + d));
                }
            }

        fillTile(WATER, rgba(46, 98, 200, 150));
        for (int y = 0; y < TILE_PX; y++)
            for (int x = 0; x < TILE_PX; x++) {
                int wave = (int) (Math.sin((x + y * 0.5) * 0.7) * 10);
                int d = r.nextInt(15) - 7 + wave;
                set(WATER, x, y, rgba(46 + d, 98 + d, 200 + d, 150));
            }

        fillTile(GLASS, rgba(216, 238, 255, 26));
        for (int i = 0; i < TILE_PX; i++)
            for (int w = 0; w < 2; w++) {
                set(GLASS, i, w, rgba(226, 244, 255, 210));
                set(GLASS, i, TILE_PX - 1 - w, rgba(226, 244, 255, 210));
                set(GLASS, w, i, rgba(226, 244, 255, 210));
                set(GLASS, TILE_PX - 1 - w, i, rgba(226, 244, 255, 210));
            }

        noiseTile(BEDROCK, rgb(58, 58, 60), 34, r);
        noiseTile(OBSIDIAN, rgb(28, 20, 44), 12, r);

        fillTile(BRICK, rgb(150, 68, 54));
        for (int y = 0; y < TILE_PX; y++)
            for (int x = 0; x < TILE_PX; x++) {
                if (y % 8 == 0) {
                    set(BRICK, x, y, rgb(196, 194, 188));
                    continue;
                }
                int offset = ((y / 8) % 2) * 8;
                if ((x + offset) % 16 == 0) set(BRICK, x, y, rgb(196, 194, 188));
                else {
                    int d = r.nextInt(21) - 10;
                    set(BRICK, x, y, rgb(150 + d, 68 + d, 54 + d));
                }
            }

        // gravel
        noiseTile(GRAVEL, rgb(126, 122, 120), 22, r);
        for (int i = 0; i < 46; i++) {
            set(GRAVEL, r.nextInt(TILE_PX), r.nextInt(TILE_PX),
                    rgb(94 + r.nextInt(54), 92 + r.nextInt(54), 90 + r.nextInt(54)));
        }

        // ores
        oreTile(COAL_ORE, r, rgb(38, 38, 42), rgb(72, 72, 76));
        oreTile(IRON_ORE, r, rgb(198, 150, 108), rgb(152, 112, 80));
        oreTile(GOLD_ORE, r, rgb(240, 208, 92), rgb(184, 152, 52));
        oreTile(DIAMOND_ORE, r, rgb(112, 232, 234), rgb(58, 172, 182));

        // sandstone
        noiseTile(SANDSTONE, rgb(222, 210, 166), 10, r);
        for (int y = 0; y < TILE_PX; y++) {
            if (y % 8 == 0) {
                for (int x = 0; x < TILE_PX; x++) set(SANDSTONE, x, y, rgb(198, 186, 142));
            }
        }

        // mossy cobblestone
        for (int y = 0; y < TILE_PX; y++)
            for (int x = 0; x < TILE_PX; x++) {
                int d = r.nextInt(19) - 9;
                set(MOSSY_COBBLE, x, y, rgb(112 + d, 112 + d, 114 + d));
            }
        for (int i = 0; i < 30; i++) {
            int bx = r.nextInt(TILE_PX), by = r.nextInt(TILE_PX);
            int n = 3 + r.nextInt(5);
            for (int j = 0; j < n; j++) {
                int d = r.nextInt(30);
                set(MOSSY_COBBLE, bx + r.nextInt(4) - 1, by + r.nextInt(4) - 1, rgb(56 + d, 104 + d, 46 + d));
            }
        }

        // ice
        fillTile(ICE, rgba(150, 202, 242, 235));
        for (int i = 0; i < 24; i++) {
            set(ICE, r.nextInt(TILE_PX), r.nextInt(TILE_PX),
                    rgba(200 + r.nextInt(40), 228 + r.nextInt(27), 235 + r.nextInt(20), 245));
        }

        // clay
        noiseTile(CLAY, rgb(162, 170, 180), 10, r);

        // metal / gem blocks
        bevelBlock(IRON_BLOCK, rgb(222, 222, 226), rgb(176, 176, 182));
        bevelBlock(GOLD_BLOCK, rgb(248, 216, 92), rgb(196, 162, 44));
        bevelBlock(DIAMOND_BLOCK, rgb(122, 234, 232), rgb(70, 176, 186));

        // bookshelf
        fillTile(BOOKSHELF, rgb(160, 124, 72));
        int[] bookCols = {0xC24B3A, 0x3A62C2, 0x2E9B4D, 0xC2A43A, 0x8A3AC2};
        for (int y = 0; y < TILE_PX; y++)
            for (int x = 0; x < TILE_PX; x++) {
                if (y % 8 == 0) set(BOOKSHELF, x, y, rgb(112, 84, 46));
                else {
                    int c = bookCols[(x / 4) % 5];
                    int d = r.nextInt(21) - 10;
                    set(BOOKSHELF, x, y, rgb(((c >> 16) & 0xff) + d, ((c >> 8) & 0xff) + d, (c & 0xff) + d));
                }
            }

        // glowstone
        noiseTile(GLOWSTONE, rgb(222, 178, 94), 16, r);
        for (int i = 0; i < 30; i++) {
            set(GLOWSTONE, r.nextInt(TILE_PX), r.nextInt(TILE_PX), rgb(255, 238, 156));
        }

        // birch log side
        fillTile(BIRCH_LOG_SIDE, rgb(222, 220, 210));
        for (int y = 0; y < TILE_PX; y++)
            for (int x = 0; x < TILE_PX; x++) {
                int d = r.nextInt(17) - 8;
                set(BIRCH_LOG_SIDE, x, y, rgb(222 + d, 220 + d, 210 + d));
            }
        for (int i = 0; i < 12; i++) {
            int bx = r.nextInt(TILE_PX), by = r.nextInt(TILE_PX);
            for (int y = 0; y < 2; y++)
                for (int x = 0; x < 3; x++)
                    set(BIRCH_LOG_SIDE, bx + x, by + y, rgb(60, 56, 52));
        }

        // birch planks
        fillTile(BIRCH_PLANKS, rgb(206, 190, 150));
        for (int y = 0; y < TILE_PX; y++)
            for (int x = 0; x < TILE_PX; x++) {
                if (y % 8 == 0) set(BIRCH_PLANKS, x, y, rgb(160, 146, 112));
                else {
                    int d = r.nextInt(17) - 8;
                    set(BIRCH_PLANKS, x, y, rgb(206 + d, 190 + d, 150 + d));
                }
            }

        // stone brick
        fillTile(STONE_BRICK, rgb(122, 122, 126));
        for (int y = 0; y < TILE_PX; y++)
            for (int x = 0; x < TILE_PX; x++) {
                int offset = ((y / 8) % 2) * 8;
                boolean mortar = (y % 8 == 0) || ((x + offset) % 16 == 0);
                if (mortar) set(STONE_BRICK, x, y, rgb(168, 168, 172));
                else {
                    int d = r.nextInt(17) - 8;
                    set(STONE_BRICK, x, y, rgb(122 + d, 122 + d, 126 + d));
                }
            }

        railTile(RAIL_X, true, r);
        railTile(RAIL_Z, false, r);

        noiseTile(WHITE_PLASTER, rgb(238, 240, 242), 6, r);
        noiseTile(RED_PLASTER, rgb(168, 54, 48), 8, r);
        noiseTile(YELLOW_PLASTER, rgb(226, 196, 84), 8, r);
        noiseTile(BLUE_PLASTER, rgb(70, 110, 170), 8, r);
        noiseTile(DARK_PLASTER, rgb(58, 50, 48), 8, r);

        // ---- urban / nature extension tiles ----
        asphaltTile(r);
        concreteTile(r);
        roofShingleTile(r);
        darkGlassTile(r);
        steelTile(r);
        hedgeTile(r);
        flowerTile(FLOWER_RED, r, rgb(206, 44, 58), rgb(248, 214, 206));
        flowerTile(FLOWER_YELLOW, r, rgb(240, 202, 58), rgb(255, 244, 194));
        grassSpriteTile(TALL_GRASS, r, rgb(86, 150, 52), 15);
        cactusTile(r);
        noiseTile(GRANITE, rgb(158, 120, 118), 13, r);
        for (int i = 0; i < 60; i++) {
            set(GRANITE, r.nextInt(TILE_PX), r.nextInt(TILE_PX),
                    rgb(186 + r.nextInt(30), 150 + r.nextInt(24), 146 + r.nextInt(24)));
        }
        noiseTile(BASALT, rgb(72, 72, 80), 11, r);
        for (int x = 0; x < TILE_PX; x += 7) {
            for (int y = 0; y < TILE_PX; y++) {
                int d = r.nextInt(13) - 6;
                set(BASALT, x, y, rgb(54 + d, 54 + d, 62 + d));
            }
        }
        terracottaTile(r);
        roofSlateTile(r);
        lampTile(r);
        noiseTile(MUD, rgb(92, 76, 58), 16, r);
        for (int i = 0; i < 28; i++) {
            set(MUD, r.nextInt(TILE_PX), r.nextInt(TILE_PX), rgb(66 + r.nextInt(22), 54 + r.nextInt(18), 40 + r.nextInt(14)));
        }
        System.arraycopy(tiles[STONE_BRICK], 0, tiles[MOSSY_BRICK], 0, TILE_PX * TILE_PX);
        for (int i = 0; i < 46; i++) {
            int bx = r.nextInt(TILE_PX), by = r.nextInt(TILE_PX);
            int n = 2 + r.nextInt(5);
            for (int j = 0; j < n; j++) {
                int d = r.nextInt(26);
                set(MOSSY_BRICK, bx + r.nextInt(5) - 2, by + r.nextInt(5) - 2, rgb(58 + d, 104 + d, 48 + d));
            }
        }
        fernTile(r);
        leavesVariant(SPRUCE_LEAVES, r, 40, 88, 58);
        leavesVariant(JUNGLE_LEAVES, r, 74, 164, 62);
    }

    private void asphaltTile(Random r) {
        noiseTile(ASPHALT, rgb(58, 58, 63), 8, r);
        for (int i = 0; i < 46; i++) {
            int g = 72 + r.nextInt(34);
            set(ASPHALT, r.nextInt(TILE_PX), r.nextInt(TILE_PX), rgb(g, g, g + 4));
        }
        for (int i = 0; i < 3; i++) {
            int x = r.nextInt(TILE_PX - 8), y = r.nextInt(TILE_PX - 4);
            for (int k = 0; k < 8; k++) set(ASPHALT, x + k, y + (k & 1), rgb(40, 40, 44));
        }
    }

    private void concreteTile(Random r) {
        noiseTile(CONCRETE, rgb(190, 190, 186), 6, r);
        for (int i = 0; i < 22; i++) {
            int g = 200 + r.nextInt(24);
            set(CONCRETE, r.nextInt(TILE_PX), r.nextInt(TILE_PX), rgb(g, g, g - 4));
        }
        for (int x = 0; x < TILE_PX; x++) {
            set(CONCRETE, x, 0, rgb(168, 168, 164));
            set(CONCRETE, x, 16, rgb(172, 172, 168));
        }
        for (int y = 0; y < TILE_PX; y++) {
            set(CONCRETE, 0, y, rgb(168, 168, 164));
            set(CONCRETE, 16, y, rgb(172, 172, 168));
        }
    }

    private void roofShingleTile(Random r) {
        fillTile(ROOF_TILE, rgb(176, 78, 54));
        for (int y = 0; y < TILE_PX; y++) {
            int row = y / 6;
            int offset = (row % 2) * 6;
            boolean seamRow = (y % 6 == 0) || (y % 6 == 5);
            for (int x = 0; x < TILE_PX; x++) {
                boolean seamCol = ((x + offset) % 12 == 0);
                int d = r.nextInt(19) - 9;
                if (seamRow || seamCol) set(ROOF_TILE, x, y, rgb(126 + d, 52 + d, 38 + d));
                else set(ROOF_TILE, x, y, rgb(186 + d, 86 + d, 60 + d));
            }
        }
    }

    private void darkGlassTile(Random r) {
        fillTile(DARK_GLASS, rgba(30, 44, 62, 200));
        for (int y = 0; y < TILE_PX; y++) {
            for (int x = 0; x < TILE_PX; x++) {
                int d = (x + y) % 16;
                int a = 190 + d * 3;
                set(DARK_GLASS, x, y, rgba(34 + d, 52 + d, 76 + d, a > 255 ? 255 : a));
            }
        }
        for (int i = 0; i < TILE_PX; i++) {
            set(DARK_GLASS, i, 0, rgba(168, 196, 222, 235));
            set(DARK_GLASS, 0, i, rgba(168, 196, 222, 235));
            set(DARK_GLASS, i, TILE_PX - 1, rgba(120, 150, 178, 235));
            set(DARK_GLASS, TILE_PX - 1, i, rgba(120, 150, 178, 235));
        }
        for (int i = 0; i < 12; i++) {
            set(DARK_GLASS, 4 + i, 24 - i, rgba(226, 244, 255, 120));
        }
    }

    private void steelTile(Random r) {
        for (int y = 0; y < TILE_PX; y++) {
            int g = 132 + (y % 8 == 0 ? 26 : 0);
            for (int x = 0; x < TILE_PX; x++) {
                int d = r.nextInt(9) - 4;
                set(STEEL, x, y, rgb(g + d, g + d, g + 4 + d));
            }
        }
        for (int y = 3; y < TILE_PX; y += 8) {
            for (int x = 3; x < TILE_PX; x += 8) {
                set(STEEL, x, y, rgb(186, 188, 194));
                set(STEEL, x + 1, y, rgb(92, 94, 100));
                set(STEEL, x, y + 1, rgb(92, 94, 100));
            }
        }
    }

    private void hedgeTile(Random r) {
        fillTile(HEDGE, rgb(44, 92, 40));
        for (int i = 0; i < 260; i++) {
            int x = r.nextInt(TILE_PX), y = r.nextInt(TILE_PX);
            int d = r.nextInt(46) - 16;
            set(HEDGE, x, y, rgb(52 + d, 104 + d, 44 + d));
        }
        for (int i = 0; i < 40; i++) {
            set(HEDGE, r.nextInt(TILE_PX), r.nextInt(TILE_PX), rgb(28, 62, 28));
        }
    }

    private void flowerTile(int t, Random r, int petal, int highlight) {
        fillTile(t, 0x00000000);
        int stem = rgb(64, 118, 46);
        for (int y = 16; y < TILE_PX; y++) {
            set(t, TILE_PX / 2, y, stem);
            set(t, TILE_PX / 2 + 1, y, rgb(52, 98, 38));
        }
        for (int i = 0; i < 6; i++) {
            set(t, TILE_PX / 2 - 4 + i, 22 + (i & 1), rgb(72, 128, 50));
            set(t, TILE_PX / 2 + 4 - i, 19 + (i & 1), rgb(72, 128, 50));
        }
        int cx = TILE_PX / 2, cy = 13;
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int d2 = dx * dx + dy * dy;
                if (d2 > 18) continue;
                int c = (d2 > 8) ? petal : highlight;
                set(t, cx + dx, cy + dy, c);
            }
        }
        set(t, cx, cy, rgb(250, 226, 120));
        for (int i = 0; i < 5; i++) {
            set(t, cx + r.nextInt(7) - 3, cy + r.nextInt(7) - 3, petal);
        }
    }

    private void grassSpriteTile(int t, Random r, int base, int blades) {
        fillTile(t, 0x00000000);
        for (int i = 0; i < blades; i++) {
            int x = 1 + r.nextInt(TILE_PX - 2);
            int h = 10 + r.nextInt(TILE_PX - 8);
            int lean = r.nextInt(3) - 1;
            for (int k = 0; k < h; k++) {
                int d = r.nextInt(37) - 18;
                int c = (base & 0xff00ff00) | 0xff000000;
                set(t, x + (k / 6) * lean, TILE_PX - 1 - k,
                        rgb(((base >> 16) & 0xff) + d, ((base >> 8) & 0xff) + d, (base & 0xff) + d));
            }
        }
    }

    private void cactusTile(Random r) {
        fillTile(CACTUS, rgb(62, 122, 62));
        for (int y = 0; y < TILE_PX; y++) {
            for (int x = 0; x < TILE_PX; x++) {
                int d = r.nextInt(15) - 7;
                int rib = (x % 8 < 2) ? -16 : 0;
                set(CACTUS, x, y, rgb(62 + d + rib, 122 + d + rib, 62 + d + rib));
            }
        }
        for (int i = 0; i < 26; i++) {
            int x = r.nextInt(TILE_PX), y = r.nextInt(TILE_PX);
            set(CACTUS, x, y, rgb(226, 232, 206));
            set(CACTUS, x + 1, y, rgb(96, 140, 82));
        }
        fillTile(CACTUS_TOP, rgb(74, 134, 70));
        for (int y = 0; y < TILE_PX; y++) {
            for (int x = 0; x < TILE_PX; x++) {
                int dx = x - TILE_PX / 2, dy = y - TILE_PX / 2;
                if (dx * dx + dy * dy > 196) continue;
                int d = r.nextInt(13) - 6;
                set(CACTUS_TOP, x, y, rgb(84 + d, 146 + d, 76 + d));
            }
        }
    }

    private void terracottaTile(Random r) {
        noiseTile(TERRACOTTA, rgb(186, 106, 74), 12, r);
        for (int y = 0; y < TILE_PX; y += 8) {
            for (int x = 0; x < TILE_PX; x++) set(TERRACOTTA, x, y, rgb(148, 82, 58));
        }
        for (int i = 0; i < 30; i++) {
            set(TERRACOTTA, r.nextInt(TILE_PX), r.nextInt(TILE_PX), rgb(206, 134, 98));
        }
    }

    private void roofSlateTile(Random r) {
        fillTile(ROOF_SLATE, rgb(78, 86, 100));
        for (int y = 0; y < TILE_PX; y++) {
            int row = y / 5;
            int offset = (row % 2) * 5;
            for (int x = 0; x < TILE_PX; x++) {
                boolean seam = (y % 5 == 0) || ((x + offset) % 10 == 0);
                int d = r.nextInt(12) - 6;
                set(ROOF_SLATE, x, y, seam ? rgb(52 + d, 58 + d, 70 + d) : rgb(84 + d, 92 + d, 108 + d));
            }
        }
    }

    private void lampTile(Random r) {
        fillTile(LAMP, rgb(64, 56, 46));
        for (int y = 2; y < TILE_PX - 2; y++) {
            for (int x = 2; x < TILE_PX - 2; x++) {
                int dx = x - TILE_PX / 2, dy = y - TILE_PX / 2;
                int d2 = dx * dx + dy * dy;
                if (d2 > 150) continue;
                int warm = 255 - Math.min(90, d2 / 2);
                set(LAMP, x, y, rgb(warm, (int) (warm * 0.86f), (int) (warm * 0.58f)));
            }
        }
        for (int i = 0; i < TILE_PX; i++) {
            set(LAMP, i, 0, rgb(58, 50, 42));
            set(LAMP, i, 1, rgb(92, 78, 60));
            set(LAMP, i, TILE_PX - 1, rgb(58, 50, 42));
            set(LAMP, 0, i, rgb(58, 50, 42));
            set(LAMP, 1, i, rgb(92, 78, 60));
            set(LAMP, TILE_PX - 1, i, rgb(58, 50, 42));
        }
    }

    private void fernTile(Random r) {
        fillTile(FERN, 0x00000000);
        for (int f = 0; f < 4; f++) {
            int baseX = 8 + r.nextInt(16);
            int baseY = TILE_PX - 1;
            int h = 12 + r.nextInt(16);
            for (int k = 0; k < h; k++) {
                int x = baseX + (int) (Math.sin(k * 0.42f) * (k * 0.35f));
                int d = r.nextInt(31) - 12;
                set(FERN, x, baseY - k, rgb(58 + d, 122 + d, 54 + d));
                if (k % 3 == 0 && k > 2) {
                    set(FERN, x - 2, baseY - k, rgb(48 + d, 104 + d, 44 + d));
                    set(FERN, x + 2, baseY - k, rgb(48 + d, 104 + d, 44 + d));
                }
            }
        }
    }

    private void leavesVariant(int t, Random r, int cr, int cg, int cb) {
        fillTile(t, rgba(cr, cg, cb, 255));
        for (int y = 0; y < TILE_PX; y++) {
            for (int x = 0; x < TILE_PX; x++) {
                int d = r.nextInt(51) - 24;
                if (r.nextInt(100) < 16) set(t, x, y, 0x00000000);
                else set(t, x, y, rgba(cr + d, cg + d, cb + d, 255));
            }
        }
        for (int i = 0; i < 26; i++) {
            set(t, r.nextInt(TILE_PX), r.nextInt(TILE_PX), rgba(cr - 30, cg - 34, cb - 20, 255));
        }
    }

    private void railTile(int t, boolean alongU, Random r) {
        fillTile(t, 0x00000000);
        int tie = rgb(126, 96, 58);
        int tieDark = rgb(96, 72, 42);
        int rail = rgb(196, 198, 206);
        int railDark = rgb(130, 132, 142);
        if (alongU) {
            for (int x = 4; x < TILE_PX; x += 8) {
                for (int y = 2; y < TILE_PX - 2; y++) {
                    set(t, x, y, tie);
                    set(t, x + 1, y, tieDark);
                }
            }
            for (int x = 0; x < TILE_PX; x++) {
                set(t, x, 10, railDark);
                set(t, x, 11, rail);
                set(t, x, 20, rail);
                set(t, x, 21, railDark);
            }
        } else {
            for (int y = 4; y < TILE_PX; y += 8) {
                for (int x = 2; x < TILE_PX - 2; x++) {
                    set(t, x, y, tie);
                    set(t, x, y + 1, tieDark);
                }
            }
            for (int y = 0; y < TILE_PX; y++) {
                set(t, 10, y, railDark);
                set(t, 11, y, rail);
                set(t, 20, y, rail);
                set(t, 21, y, railDark);
            }
        }
    }

    private void oreTile(int t, Random r, int c1, int c2) {
        noiseTile(t, rgb(128, 128, 130), 14, r);
        int clusters = 4 + r.nextInt(3);
        for (int i = 0; i < clusters; i++) {
            int cx = r.nextInt(TILE_PX), cy = r.nextInt(TILE_PX);
            int n = 7 + r.nextInt(7);
            for (int j = 0; j < n; j++) {
                int x = cx + r.nextInt(7) - 3;
                int y = cy + r.nextInt(7) - 3;
                set(t, x, y, (j % 2 == 0) ? c1 : c2);
            }
        }
    }

    private void bevelBlock(int t, int light, int dark) {
        fillTile(t, light);
        for (int i = 0; i < TILE_PX; i++) {
            set(t, i, 0, dark);
            set(t, i, 1, light);
            set(t, 0, i, dark);
            set(t, TILE_PX - 1, i, dark);
            set(t, TILE_PX - 2, i, light);
            set(t, i, TILE_PX - 1, dark);
        }
    }

    private void compose() {
        for (int t = 0; t < TILE_COUNT; t++) {
            int ox = (t % TILES) * TILE_PX;
            int oy = (t / TILES) * TILE_PX;
            for (int y = 0; y < TILE_PX; y++) {
                System.arraycopy(tiles[t], y * TILE_PX, atlasPixels, (oy + y) * ATLAS_PX + ox, TILE_PX);
            }
        }
    }

    private void buildClouds() {
        int size = 256;
        cloudPixels = new int[size * size];
        long seed = 99L;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float v = tileableFbm(x, y, size, 6, 4, seed);
                float a = smooth(0.52f, 0.72f, v);
                int bright = (int) (235 + 20 * smooth(0.6f, 0.9f, v));
                cloudPixels[y * size + x] = rgba(bright, bright, (int) (bright * 0.97f), (int) (a * 235));
            }
        }
    }

    private static float smooth(float edge0, float edge1, float x) {
        float t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3 - 2 * t);
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static float tileableFbm(int x, int y, int size, int baseCells, int octaves, long seed) {
        float sum = 0f, amp = 1f, norm = 0f;
        int cells = baseCells;
        for (int o = 0; o < octaves; o++) {
            sum += amp * valueNoiseWrapped(x, y, size, cells, seed + o * 71);
            norm += amp;
            amp *= 0.5f;
            cells *= 2;
        }
        return sum / norm;
    }

    private static float valueNoiseWrapped(int x, int y, int size, int cells, long seed) {
        float fx = (float) x / size * cells;
        float fy = (float) y / size * cells;
        int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy);
        float tx = fx - x0, ty = fy - y0;
        int x1 = (x0 + 1) % cells, y1 = (y0 + 1) % cells;
        x0 = ((x0 % cells) + cells) % cells;
        y0 = ((y0 % cells) + cells) % cells;
        float v00 = hash2(x0, y0, seed);
        float v10 = hash2(x1, y0, seed);
        float v01 = hash2(x0, y1, seed);
        float v11 = hash2(x1, y1, seed);
        float sx = tx * tx * (3 - 2 * tx);
        float sy = ty * ty * (3 - 2 * ty);
        float a = v00 + (v10 - v00) * sx;
        float b = v01 + (v11 - v01) * sx;
        return a + (b - a) * sy;
    }

    private static float hash2(int x, int y, long seed) {
        long h = seed;
        h = h * 6364136223846793005L + (x * 73856093L);
        h = h * 6364136223846793005L + (y * 19349663L);
        h ^= (h >>> 29);
        h *= 0xbf58476d1ce4e5b9L;
        h ^= (h >>> 32);
        return (h & 0xffffff) / (float) 0xffffff;
    }

    private static byte[] toRgba(int[] px) {
        byte[] data = new byte[px.length * 4];
        int off = 0;
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            data[off++] = (byte) (c & 0xff);
            data[off++] = (byte) ((c >> 8) & 0xff);
            data[off++] = (byte) ((c >> 16) & 0xff);
            data[off++] = (byte) ((c >>> 24) & 0xff);
        }
        return data;
    }

    public void upload() {
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        textureId = ids[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        byte[] data = toRgba(atlasPixels);
        ByteBuffer buf = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
        buf.put(data);
        buf.position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, ATLAS_PX, ATLAS_PX, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
    }

    public void uploadClouds() {
        int size = 256;
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        cloudTextureId = ids[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cloudTextureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        byte[] data = toRgba(cloudPixels);
        ByteBuffer buf = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
        buf.put(data);
        buf.position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, size, size, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
    }

    public int getTextureId() {
        return textureId;
    }

    public int getCloudTextureId() {
        return cloudTextureId;
    }

    public android.graphics.Bitmap tileBitmap(int tile) {
        if (tile < 0 || tile >= TILE_COUNT) return null;
        int[] px = tiles[tile];
        android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(TILE_PX, TILE_PX, android.graphics.Bitmap.Config.ARGB_8888);
        b.setPixels(px, 0, TILE_PX, 0, 0, TILE_PX, TILE_PX);
        return b;
    }

    /** Average opaque colour of a tile, used for break/impact particles. */
    public int averageTileColor(int tile) {
        if (tile < 0 || tile >= TILE_COUNT) return 0xff888888;
        int[] px = tiles[tile];
        long sr = 0, sg = 0, sb = 0;
        int n = 0;
        for (int c : px) {
            int a = (c >>> 24) & 0xff;
            if (a < 40) continue;
            sr += (c >> 16) & 0xff;
            sg += (c >> 8) & 0xff;
            sb += c & 0xff;
            n++;
        }
        if (n == 0) return 0xff888888;
        return 0xff000000 | ((int) (sr / n) << 16) | ((int) (sg / n) << 8) | (int) (sb / n);
    }

    public static float[] tileUV(int tile) {
        int col = tile % TILES;
        int row = tile / TILES;
        float u0 = (float) col / TILES + INSET;
        float v0 = (float) row / TILES + INSET;
        float u1 = (float) (col + 1) / TILES - INSET;
        float v1 = (float) (row + 1) / TILES - INSET;
        return new float[]{u0, v0, u1, v1};
    }
}
