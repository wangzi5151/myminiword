package com.wangzi5151.myminiword.world;

import java.util.Random;

public class TerrainGenerator {
    private final Noise heightNoise;
    private final Noise detailNoise;
    private final Noise biomeNoise;
    private final Noise mountainNoise;

    public static final int SEA_LEVEL = 26;

    public TerrainGenerator(long seed) {
        heightNoise = new Noise(seed);
        detailNoise = new Noise(seed * 31 + 7);
        biomeNoise = new Noise(seed * 131 + 17);
        mountainNoise = new Noise(seed * 977 + 53);
    }

    public void generate(Chunk chunk) {
        int baseX = chunk.cx * Chunk.SIZE_X;
        int baseZ = chunk.cz * Chunk.SIZE_Z;

        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                int wx = baseX + x;
                int wz = baseZ + z;
                int surface = surfaceHeight(wx, wz);
                float biome = biomeNoise.fbm2(wx * 0.004f, wz * 0.004f, 2, 2f, 0.5f);
                boolean cold = biome > 0.35f;
                boolean desert = biome < -0.38f;
                boolean sandy = surface <= SEA_LEVEL + 1;

                for (int y = 0; y <= surface; y++) {
                    byte id;
                    if (y == 0) {
                        id = (byte) BlockType.BEDROCK.id;
                    } else if (y < surface - 3) {
                        id = (byte) BlockType.STONE.id;
                    } else if (y < surface) {
                        id = (byte) ((sandy || desert) ? BlockType.SANDSTONE.id : BlockType.DIRT.id);
                    } else {
                        if (sandy) id = (byte) BlockType.SAND.id;
                        else if (desert) id = (byte) BlockType.SAND.id;
                        else if (cold) id = (byte) BlockType.SNOW.id;
                        else id = (byte) BlockType.GRASS.id;
                    }
                    chunk.set(x, y, z, id);
                }

                for (int y = surface + 1; y <= SEA_LEVEL; y++) {
                    chunk.set(x, y, z, (byte) BlockType.WATER.id);
                }
            }
        }

        generateOres(chunk, baseX, baseZ);
        generateTrees(chunk, baseX, baseZ);
        generateBoulders(chunk, baseX, baseZ);
    }

    private void generateBoulders(Chunk chunk, int baseX, int baseZ) {
        Random r = new Random((long) chunk.cx * 6151L + (long) chunk.cz * 4297L + 91L);
        int n = r.nextInt(3);
        for (int i = 0; i < n; i++) {
            int x = 2 + r.nextInt(Chunk.SIZE_X - 4);
            int z = 2 + r.nextInt(Chunk.SIZE_Z - 4);
            int s = surfaceHeight(baseX + x, baseZ + z);
            int id = chunk.get(x, s, z);
            if (id != BlockType.GRASS.id && id != BlockType.STONE.id && id != BlockType.SAND.id
                    && id != BlockType.SNOW.id) continue;
            int blob = r.nextInt(2);
            for (int dx = -1; dx <= blob; dx++) {
                for (int dz = -1; dz <= blob; dz++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        if (r.nextFloat() < 0.25f) continue;
                        int bx = x + dx, bz = z + dz, by = s + dy;
                        if (dy == 1 && r.nextBoolean()) continue;
                        chunk.set(bx, by, bz, (byte) (r.nextInt(4) == 0
                                ? BlockType.MOSSY_COBBLE.id : BlockType.STONE.id));
                    }
                }
            }
        }
    }

    private void generateOres(Chunk chunk, int baseX, int baseZ) {
        Random r = new Random((long) chunk.cx * 918273645L + (long) chunk.cz * 564738291L);
        for (int y = 1; y < 54; y++) {
            for (int x = 0; x < Chunk.SIZE_X; x++) {
                for (int z = 0; z < Chunk.SIZE_Z; z++) {
                    if (chunk.get(x, y, z) != (byte) BlockType.STONE.id) continue;
                    float f = r.nextFloat();
                    if (y < 16 && f < 0.0022f) {
                        chunk.set(x, y, z, (byte) BlockType.DIAMOND_ORE.id);
                    } else if (y < 28 && f < 0.0045f) {
                        chunk.set(x, y, z, (byte) BlockType.GOLD_ORE.id);
                    } else if (y < 42 && f < 0.009f) {
                        chunk.set(x, y, z, (byte) BlockType.IRON_ORE.id);
                    } else if (y < 54 && f < 0.018f) {
                        chunk.set(x, y, z, (byte) BlockType.COAL_ORE.id);
                    } else if (f < 0.023f) {
                        chunk.set(x, y, z, (byte) (r.nextBoolean() ? BlockType.GRAVEL.id : BlockType.CLAY.id));
                    }
                }
            }
        }
    }

    private void generateTrees(Chunk chunk, int baseX, int baseZ) {
        Random r = new Random((long) chunk.cx * 341873128712L + (long) chunk.cz * 132897987541L);
        int trees = 2 + r.nextInt(4);
        for (int i = 0; i < trees; i++) {
            int x = 2 + r.nextInt(Chunk.SIZE_X - 4);
            int z = 2 + r.nextInt(Chunk.SIZE_Z - 4);
            int surface = surfaceHeight(baseX + x, baseZ + z);
            if (chunk.get(x, surface, z) != (byte) BlockType.GRASS.id) continue;
            float biome = biomeNoise.fbm2((baseX + x) * 0.004f, (baseZ + z) * 0.004f, 2, 2f, 0.5f);
            // Dense forests in the neutral biome band, sparse elsewhere.
            if (Math.abs(biome) > 0.22f && r.nextFloat() < 0.7f) continue;
            boolean birch = r.nextInt(4) == 0;
            int trunk = birch ? BlockType.BIRCH_LOG.id : BlockType.LOG.id;
            placeTree(chunk, x, surface + 1, z, 4 + r.nextInt(3), trunk);
        }
    }

    public int surfaceHeight(int wx, int wz) {
        // Broad, low-frequency rolling hills: large smooth swells.
        float base = heightNoise.fbm2(wx * 0.0032f, wz * 0.0032f, 3, 2f, 0.5f);
        // Gentle medium undulation for natural variation without jaggedness.
        float rolling = heightNoise.fbm2(wx * 0.010f, wz * 0.010f, 2, 2f, 0.5f);
        float h = 28f + base * 16f + rolling * 4f;
        // Soft foothills that ramp in smoothly instead of rising as cliffs.
        float mountain = mountainNoise.fbm2(wx * 0.0016f, wz * 0.0016f, 3, 2f, 0.5f);
        if (mountain > 0.32f) {
            float m = mountain - 0.32f;
            h += m * m * 62f + m * 12f;
        }
        // Very soft surface detail (sub-block) to avoid a flat, tiled look.
        h += detailNoise.fbm2(wx * 0.05f, wz * 0.05f, 2, 2f, 0.5f) * 0.7f;
        int surface = (int) Math.round(h);
        if (surface < 2) surface = 2;
        if (surface > Chunk.SIZE_Y - 6) surface = Chunk.SIZE_Y - 6;
        return surface;
    }

    private void placeTree(Chunk chunk, int x, int y, int z, int height, int trunk) {
        for (int i = 0; i < height; i++) {
            chunk.set(x, y + i, z, (byte) trunk);
        }
        int top = y + height;
        for (int dy = -2; dy <= 1; dy++) {
            int radius = (dy >= 1) ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0 && dy < 1) continue;
                    int lx = x + dx;
                    int ly = top + dy;
                    int lz = z + dz;
                    if (chunk.get(lx, ly, lz) == 0) {
                        chunk.set(lx, ly, lz, (byte) BlockType.LEAVES.id);
                    }
                }
            }
        }
    }
}
