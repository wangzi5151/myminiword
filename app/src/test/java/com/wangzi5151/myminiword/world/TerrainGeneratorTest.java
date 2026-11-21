package com.wangzi5151.myminiword.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TerrainGeneratorTest {
    private static final long SEED = 20260925L;

    @Test
    public void heightsStayInsideTheChunk() {
        TerrainGenerator g = new TerrainGenerator(SEED);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int x = -512; x <= 512; x += 17) {
            for (int z = -512; z <= 512; z += 17) {
                int h = g.surfaceHeight(x, z);
                assertTrue("height below world", h >= 2);
                assertTrue("height above world", h <= Chunk.SIZE_Y - 4);
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        }
        assertTrue("terrain should have relief", max - min > 12);
    }

    @Test
    public void generationIsDeterministic() {
        TerrainGenerator a = new TerrainGenerator(SEED);
        TerrainGenerator b = new TerrainGenerator(SEED);
        Chunk ca = new Chunk(3, -2);
        Chunk cb = new Chunk(3, -2);
        a.generate(ca);
        b.generate(cb);
        for (int i = 0; i < ca.blocks.length; i++) {
            assertEquals("block " + i, ca.blocks[i], cb.blocks[i]);
        }
    }

    @Test
    public void worldContainsSeveralBiomes() {
        TerrainGenerator g = new TerrainGenerator(SEED);
        boolean[] seen = new boolean[Biome.ALL.length];
        int kinds = 0;
        for (int x = -700; x <= 700; x += 23) {
            for (int z = -700; z <= 700; z += 23) {
                int id = g.biomeIdAt(x, z);
                if (!seen[id]) {
                    seen[id] = true;
                    kinds++;
                }
            }
        }
        assertTrue("expected a varied world, saw " + kinds + " biomes", kinds >= 8);
        assertTrue(seen[Biome.PLAINS] || seen[Biome.FOREST]);
        assertTrue(seen[Biome.OCEAN]);
    }

    @Test
    public void citiesSitOnBuildableLand() {
        TerrainGenerator g = new TerrainGenerator(SEED);
        new CityGenerator(g);
        assertEquals("all anchors are configured", 6, CityGenerator.CITIES.length);
        for (int[] c : CityGenerator.CITIES) {
            int h = g.surfaceHeight(c[0], c[1]);
            assertTrue("city " + c[0] + "," + c[1] + " under water at " + h,
                    h > TerrainGenerator.SEA_LEVEL + 1);
        }
    }

    @Test
    public void chunksCarryWaterCavesOresAndVegetation() {
        TerrainGenerator g = new TerrainGenerator(SEED);
        int water = 0;
        int caves = 0;
        int ores = 0;
        int logs = 0;
        int plants = 0;
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                Chunk c = new Chunk(cx, cz);
                g.generate(c);
                for (int y = 0; y < Chunk.SIZE_Y; y++) {
                    for (int z = 0; z < Chunk.SIZE_Z; z++) {
                        for (int x = 0; x < Chunk.SIZE_X; x++) {
                            int id = c.get(x, y, z);
                            if (id == BlockType.WATER.id) water++;
                            else if (id == BlockType.COAL_ORE.id || id == BlockType.IRON_ORE.id
                                    || id == BlockType.GOLD_ORE.id || id == BlockType.DIAMOND_ORE.id) ores++;
                            else if (id == BlockType.LOG.id || id == BlockType.BIRCH_LOG.id) logs++;
                            else if (BlockType.isCross(id)) plants++;
                            else if (id == 0 && y > 2 && y < 24) caves++;
                        }
                    }
                }
            }
        }
        assertTrue("no water generated", water > 0);
        assertTrue("no ore veins generated", ores > 0);
        assertTrue("no trees generated", logs > 0);
        assertTrue("no plants generated", plants > 0);
        assertTrue("no cave air pockets", caves > 0);
    }

    @Test
    public void chunkGenerationOrderDoesNotMatter() {
        TerrainGenerator g = new TerrainGenerator(SEED);
        Chunk first = new Chunk(1, 0);
        g.generate(first);
        // Generate a ring of neighbours afterwards, then rebuild: results must match.
        g.generate(new Chunk(0, 0));
        g.generate(new Chunk(2, 0));
        g.generate(new Chunk(1, -1));
        Chunk again = new Chunk(1, 0);
        g.generate(again);
        for (int i = 0; i < again.blocks.length; i++) {
            assertEquals("block " + i, first.blocks[i], again.blocks[i]);
        }
        int logs = 0;
        int leaves = 0;
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                Chunk c = new Chunk(cx, cz);
                g.generate(c);
                logs += countOf(c, BlockType.LOG.id) + countOf(c, BlockType.BIRCH_LOG.id);
                leaves += countOf(c, BlockType.LEAVES.id) + countOf(c, BlockType.SPRUCE_LEAVES.id)
                        + countOf(c, BlockType.JUNGLE_LEAVES.id);
            }
        }
        assertTrue("no trees in the sampled region", logs > 10);
        assertTrue("no canopies in the sampled region", leaves > logs);
    }

    private static int countOf(Chunk c, int id) {
        int n = 0;
        for (byte b : c.blocks) if ((b & 0xff) == id) n++;
        return n;
    }
}
