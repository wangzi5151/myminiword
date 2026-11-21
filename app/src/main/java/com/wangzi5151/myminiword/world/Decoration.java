package com.wangzi5151.myminiword.world;

import java.util.Random;

/**
 * Biomes-aware decoration pass: trees, plants, boulders and small ruins.
 *
 * Trees belonging to a chunk are planted with a one-chunk margin so canopies that
 * cross a chunk border still appear whole and exactly once.
 */
public final class Decoration {
    private final TerrainGenerator terrain;
    private final long seed;

    Decoration(TerrainGenerator terrain, long seed) {
        this.terrain = terrain;
        this.seed = seed;
    }

    public void populate(Chunk chunk, int baseX, int baseZ, int[][] heights) {
        plants(chunk, baseX, baseZ, heights);
        boulders(chunk, baseX, baseZ, heights);
        trees(chunk, baseX, baseZ);
        ruins(chunk, baseX, baseZ, heights);
    }

    private void plants(Chunk chunk, int baseX, int baseZ, int[][] heights) {
        Random r = new Random(seed * 71 + (long) chunk.cx * 92837111L + (long) chunk.cz * 689287499L);
        for (int x = 1; x < Chunk.SIZE_X - 1; x++) {
            for (int z = 1; z < Chunk.SIZE_Z - 1; z++) {
                int h = heights[x][z];
                if (h < TerrainGenerator.SEA_LEVEL + 1 || h >= Chunk.SIZE_Y - 2) continue;
                int ground = chunk.get(x, h, z);
                if (ground == 0 || chunk.get(x, h + 1, z) != 0) continue;
                int wx = baseX + x, wz = baseZ + z;
                Biome biome = terrain.biomeAt(wx, wz);
                float roll = r.nextFloat();
                if (roll > biome.plantChance) continue;
                int plant;
                if (biome.id == Biome.DESERT) {
                    if (r.nextInt(5) == 0) {
                        int ch = 1 + r.nextInt(3);
                        for (int i = 0; i < ch && h + 1 + i < Chunk.SIZE_Y; i++) {
                            chunk.set(x, h + 1 + i, z, (byte) BlockType.CACTUS.id);
                        }
                    }
                    continue;
                }
                if (ground == BlockType.GRASS.id) {
                    // Flower meadows and fern patches follow a low-frequency field.
                    if (terrain.flora(wx, wz) > 0.24f && r.nextInt(3) != 0) {
                        chunk.set(x, h + 1, z, (byte) ((wx + wz) % 2 == 0
                                ? BlockType.FLOWER_YELLOW.id : BlockType.FLOWER_RED.id));
                        continue;
                    }
                    int pick = r.nextInt(10);
                    if (biome.id == Biome.JUNGLE || biome.id == Biome.SWAMP) {
                        plant = pick < 6 ? BlockType.FERN.id : BlockType.TALL_GRASS.id;
                    } else if (pick < 6) {
                        plant = BlockType.TALL_GRASS.id;
                    } else if (pick < 8) {
                        plant = BlockType.FLOWER_YELLOW.id;
                    } else if (pick < 9) {
                        plant = BlockType.FLOWER_RED.id;
                    } else {
                        plant = BlockType.TALL_GRASS.id;
                    }
                    chunk.set(x, h + 1, z, (byte) plant);
                } else if (ground == BlockType.SAND.id && biome.id == Biome.BEACH) {
                    if (r.nextInt(9) == 0) chunk.set(x, h + 1, z, (byte) BlockType.TALL_GRASS.id);
                }
            }
        }
    }

    private void boulders(Chunk chunk, int baseX, int baseZ, int[][] heights) {
        Random r = new Random(seed * 131 + (long) chunk.cx * 6151L + (long) chunk.cz * 4297L);
        if (r.nextInt(3) != 0) return;
        int x = 2 + r.nextInt(Chunk.SIZE_X - 5);
        int z = 2 + r.nextInt(Chunk.SIZE_Z - 5);
        int h = heights[x][z];
        if (h <= TerrainGenerator.SEA_LEVEL) return;
        int id = chunk.get(x, h, z);
        if (id != BlockType.GRASS.id && id != BlockType.STONE.id && id != BlockType.SAND.id
                && id != BlockType.SNOW.id) return;
        int radius = 1 + r.nextInt(2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= radius; dy++) {
                    if (dx * dx + dz * dz + dy * dy > radius * radius + 1) continue;
                    if (r.nextFloat() < 0.12f) continue;
                    int lx = x + dx, lz = z + dz, ly = h + dy;
                    if (lx < 0 || lx >= Chunk.SIZE_X || lz < 0 || lz >= Chunk.SIZE_Z) continue;
                    if (ly <= h && dy == 0) continue;
                    int block = (r.nextInt(5) == 0) ? BlockType.MOSSY_COBBLE.id : BlockType.STONE.id;
                    chunk.set(lx, ly, lz, (byte) block);
                }
            }
        }
    }

    private void trees(Chunk chunk, int baseX, int baseZ) {
        for (int ncx = chunk.cx - 1; ncx <= chunk.cx + 1; ncx++) {
            for (int ncz = chunk.cz - 1; ncz <= chunk.cz + 1; ncz++) {
                Random r = new Random(seed * 341873128712L + (long) ncx * 132897987541L + (long) ncz * 918273645L);
                int nx0 = ncx * Chunk.SIZE_X;
                int nz0 = ncz * Chunk.SIZE_Z;
                Biome centerBiome = terrain.biomeAt(nx0 + 8, nz0 + 8);
                int count = Math.round(centerBiome.treeDensity + r.nextFloat() * 2f - 1f);
                for (int i = 0; i < count; i++) {
                    int wx = nx0 + r.nextInt(Chunk.SIZE_X);
                    int wz = nz0 + r.nextInt(Chunk.SIZE_Z);
                    // Skip trees far outside this chunk: cheap rejection, keeps canopies whole.
                    if (wx < baseX - 4 || wx > baseX + Chunk.SIZE_X + 4
                            || wz < baseZ - 4 || wz > baseZ + Chunk.SIZE_Z + 4) continue;
                    placeTree(chunk, baseX, baseZ, wx, wz, r);
                }
            }
        }
    }

    private void placeTree(Chunk chunk, int baseX, int baseZ, int wx, int wz, Random r) {
        Biome biome = terrain.biomeAt(wx, wz);
        if (biome.id == Biome.OCEAN || biome.id == Biome.MOUNTAIN || biome.id == Biome.SNOWY_PEAKS) return;
        int h = terrain.surfaceHeight(wx, wz);
        if (h <= TerrainGenerator.SEA_LEVEL + 1 || h > Chunk.SIZE_Y - 8) return;
        int lx = wx - baseX, lz = wz - baseZ;
        if (lx < 0 || lx >= Chunk.SIZE_X || lz < 0 || lz >= Chunk.SIZE_Z) return;
        int ground = chunk.get(lx, h, lz);
        if (ground != BlockType.GRASS.id && ground != BlockType.MUD.id
                && ground != BlockType.SAND.id && ground != BlockType.DIRT.id) return;
        if (chunk.get(lx, h + 1, lz) != 0) return;

        int leaf = BlockType.LEAVES.id;
        if (biome.id == Biome.TAIGA || biome.id == Biome.SNOWY_PLAINS) leaf = BlockType.SPRUCE_LEAVES.id;
        else if (biome.id == Biome.JUNGLE) leaf = BlockType.JUNGLE_LEAVES.id;

        switch (biome.tree) {
            case Biome.TREE_SPRUCE:
                spruce(chunk, baseX, baseZ, wx, h + 1, wz, 6 + r.nextInt(5), leaf);
                break;
            case Biome.TREE_BIRCH:
                simpleTree(chunk, baseX, baseZ, wx, h + 1, wz, 5 + r.nextInt(3),
                        BlockType.BIRCH_LOG.id, leaf, 2, true);
                break;
            case Biome.TREE_JUNGLE:
                jungle(chunk, baseX, baseZ, wx, h + 1, wz, 7 + r.nextInt(5), leaf);
                break;
            case Biome.TREE_ACACIA:
                acacia(chunk, baseX, baseZ, wx, h + 1, wz, 3 + r.nextInt(2), leaf);
                break;
            case Biome.TREE_PALM:
                palm(chunk, baseX, baseZ, wx, h + 1, wz, 4 + r.nextInt(3), leaf);
                break;
            case Biome.TREE_SWAMP:
                swampTree(chunk, baseX, baseZ, wx, h + 1, wz, 3 + r.nextInt(3), leaf);
                break;
            case Biome.TREE_CACTUS:
                for (int i = 0; i < 1 + r.nextInt(3); i++) {
                    put(chunk, baseX, baseZ, wx, h + 1 + i, wz, BlockType.CACTUS.id, false);
                }
                break;
            default:
                simpleTree(chunk, baseX, baseZ, wx, h + 1, wz, 4 + r.nextInt(3),
                        BlockType.LOG.id, leaf, 2, false);
                break;
        }
    }

    private void simpleTree(Chunk chunk, int baseX, int baseZ, int wx, int y, int wz,
                            int height, int trunk, int leaf, int radius, boolean narrow) {
        for (int i = 0; i < height; i++) put(chunk, baseX, baseZ, wx, y + i, wz, trunk, false);
        int top = y + height;
        for (int dy = -2; dy <= 1; dy++) {
            int r = (dy >= 0) ? Math.max(1, radius - 1) : radius;
            if (narrow && dy < 0) r = Math.max(1, radius - 1);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0 && dy < 1) continue;
                    if (Math.abs(dx) == r && Math.abs(dz) == r && dy >= 0) continue;
                    if (Math.abs(dx) == r && Math.abs(dz) == r && ((dx + dz) & 1) == 0) continue;
                    put(chunk, baseX, baseZ, wx + dx, top + dy, wz + dz, leaf, true);
                }
            }
        }
    }

    private void spruce(Chunk chunk, int baseX, int baseZ, int wx, int y, int wz, int height, int leaf) {
        for (int i = 0; i < height; i++) put(chunk, baseX, baseZ, wx, y + i, wz, BlockType.LOG.id, false);
        for (int dy = 2; dy < height; dy++) {
            int radius = Math.min(3, (dy - 1) / 2);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius + 1) continue;
                    if (dx == 0 && dz == 0) continue;
                    put(chunk, baseX, baseZ, wx + dx, y + dy, wz + dz, leaf, true);
                }
            }
        }
        put(chunk, baseX, baseZ, wx, y + height, wz, leaf, true);
        if (terrain.temperature(wx, wz) < -0.26f) {
            snowCap(chunk, baseX, baseZ, wx, y + height, wz, 3, 5);
        }
    }

    /** Puts a thin snow layer on every exposed leaf top inside a small box. */
    private void snowCap(Chunk chunk, int baseX, int baseZ, int wx, int topY, int wz,
                         int radius, int depth) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= depth; dy++) {
                    int y = topY - dy;
                    int lx = wx + dx - baseX, lz = wz + dz - baseZ;
                    if (lx < 0 || lx >= Chunk.SIZE_X || lz < 0 || lz >= Chunk.SIZE_Z) break;
                    int id = chunk.get(lx, y, lz);
                    if (id == BlockType.SPRUCE_LEAVES.id || id == BlockType.LEAVES.id
                            || id == BlockType.JUNGLE_LEAVES.id) {
                        if (chunk.get(lx, y + 1, lz) == 0) {
                            chunk.set(lx, y + 1, lz, (byte) BlockType.SNOW.id);
                        }
                        break;
                    }
                }
            }
        }
    }

    private void jungle(Chunk chunk, int baseX, int baseZ, int wx, int y, int wz, int height, int leaf) {
        for (int i = 0; i < height; i++) put(chunk, baseX, baseZ, wx, y + i, wz, BlockType.LOG.id, false);
        int top = y + height;
        for (int dy = -1; dy <= 1; dy++) {
            int r = (dy == 1) ? 2 : 3;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + 2) continue;
                    if (dx == 0 && dz == 0 && dy < 1) continue;
                    put(chunk, baseX, baseZ, wx + dx, top + dy, wz + dz, leaf, true);
                }
            }
        }
        // Hanging vines as leaf columns for a proper jungle silhouette.
        for (int i = 0; i < 3; i++) {
            int dx = (i * 5 % 7) - 3;
            int dz = (i * 3 % 5) - 2;
            for (int k = 1; k <= 2 + (i & 1); k++) {
                put(chunk, baseX, baseZ, wx + dx, top - k, wz + dz, leaf, true);
            }
        }
    }

    private void acacia(Chunk chunk, int baseX, int baseZ, int wx, int y, int wz, int height, int leaf) {
        for (int i = 0; i < height; i++) put(chunk, baseX, baseZ, wx, y + i, wz, BlockType.LOG.id, false);
        int branchY = y + height;
        int dir = (Math.abs(wx * 31 + wz) & 1) == 0 ? 1 : -1;
        for (int i = 1; i <= 2; i++) put(chunk, baseX, baseZ, wx + dir * i, branchY, wz, BlockType.LOG.id, false);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                put(chunk, baseX, baseZ, wx + dir * 2 + dx, branchY + 1, wz + dz, leaf, true);
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 2) continue;
                put(chunk, baseX, baseZ, wx + dx, branchY + 2, wz + dz, leaf, true);
            }
        }
    }

    private void palm(Chunk chunk, int baseX, int baseZ, int wx, int y, int wz, int height, int leaf) {
        int lean = ((wx + wz) & 1) == 0 ? 1 : -1;
        for (int i = 0; i < height; i++) {
            int lx = wx + (i >= height - 2 ? lean : 0);
            put(chunk, baseX, baseZ, lx, y + i, wz, BlockType.LOG.id, false);
        }
        int top = y + height;
        int tx = wx + lean;
        for (int d = 0; d < 4; d++) {
            int dx = d == 0 ? 1 : d == 1 ? -1 : 0;
            int dz = d == 2 ? 1 : d == 3 ? -1 : 0;
            for (int k = 1; k <= 3; k++) {
                put(chunk, baseX, baseZ, tx + dx * k, top + 1 - (k > 2 ? 1 : 0), wz + dz * k, leaf, true);
            }
        }
        put(chunk, baseX, baseZ, tx, top, wz, leaf, true);
    }

    private void swampTree(Chunk chunk, int baseX, int baseZ, int wx, int y, int wz, int height, int leaf) {
        for (int i = 0; i < height; i++) put(chunk, baseX, baseZ, wx, y + i, wz, BlockType.LOG.id, false);
        int top = y + height;
        for (int dy = -1; dy <= 1; dy++) {
            int r = dy == 1 ? 2 : 3;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + 1) continue;
                    put(chunk, baseX, baseZ, wx + dx, top + dy, wz + dz, leaf, true);
                }
            }
        }
    }

    private void ruins(Chunk chunk, int baseX, int baseZ, int[][] heights) {
        Random r = new Random(seed * 911 + (long) chunk.cx * 7919L + (long) chunk.cz * 104729L);
        if (r.nextInt(26) != 0) return;
        int x = 4 + r.nextInt(Chunk.SIZE_X - 8);
        int z = 4 + r.nextInt(Chunk.SIZE_Z - 8);
        int h = heights[x][z];
        Biome biome = terrain.biomeAt(baseX + x, baseZ + z);
        if (biome.id == Biome.OCEAN || biome.id == Biome.BEACH) return;
        if (chunk.get(x, h, z) == 0) return;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 4) continue;
                if (r.nextFloat() < 0.72f) chunk.set(x + dx, h, z + dz, (byte) BlockType.COBBLE.id);
                if (r.nextFloat() < 0.18f) {
                    int wallH = 1 + r.nextInt(2);
                    for (int i = 1; i <= wallH; i++) {
                        chunk.set(x + dx, h + i, z + dz,
                                (byte) (r.nextBoolean() ? BlockType.MOSSY_BRICK.id : BlockType.STONE_BRICK.id));
                    }
                }
            }
        }
        if (r.nextBoolean()) chunk.set(x, h + 1, z, (byte) BlockType.GLOWSTONE.id);
    }

    /** Places a block if the target is inside this chunk and replaceable. */
    private void put(Chunk chunk, int baseX, int baseZ, int wx, int wy, int wz, int id, boolean soft) {
        int lx = wx - baseX, lz = wz - baseZ;
        if (lx < 0 || lx >= Chunk.SIZE_X || lz < 0 || lz >= Chunk.SIZE_Z) return;
        if (wy < 0 || wy >= Chunk.SIZE_Y) return;
        int existing = chunk.get(lx, wy, lz);
        if (soft) {
            if (existing != 0 && !BlockType.isCross(existing)
                    && existing != BlockType.LEAVES.id
                    && existing != BlockType.SPRUCE_LEAVES.id
                    && existing != BlockType.JUNGLE_LEAVES.id) return;
        } else {
            if (BlockType.isSolid(existing) && existing != BlockType.LEAVES.id
                    && existing != BlockType.SPRUCE_LEAVES.id
                    && existing != BlockType.JUNGLE_LEAVES.id
                    && existing != BlockType.CACTUS.id) return;
        }
        chunk.set(lx, wy, lz, (byte) id);
    }
}
