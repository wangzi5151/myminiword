package com.wangzi5151.myminiword.engine;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class TextureAtlas {
    public static final int TILE_PX = 32;
    public static final int TILES = 16;        // grid columns
    public static final int TILE_COUNT = 40;   // number of tiles
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
