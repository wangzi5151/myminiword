package com.wangzi5151.myminiword.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CityGeneratorTest {
    private static final long SEED = 20260925L;

    private static Chunk chunkAt(TerrainGenerator g, CityGenerator cg, int ccx, int ccz) {
        Chunk c = new Chunk(ccx, ccz);
        g.generate(c);
        cg.populate(c, ccx * Chunk.SIZE_X, ccz * Chunk.SIZE_Z);
        cg.rails(c, ccx * Chunk.SIZE_X, ccz * Chunk.SIZE_Z);
        return c;
    }

    private static int countAround(Chunk c, int x0, int z0, int x1, int z1, int id, int y0, int y1) {
        int n = 0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    if (c.get(x, y, z) == id) n++;
                }
            }
        }
        return n;
    }

    @Test
    public void citiesAreConfiguredOnLand() {
        TerrainGenerator g = new TerrainGenerator(SEED);
        CityGenerator cg = new CityGenerator(g);
        assertEquals(6, CityGenerator.CITIES.length);
        for (int i = 0; i < CityGenerator.CITIES.length; i++) {
            int[] city = CityGenerator.CITIES[i];
            int ground = cg.groundHeight(i);
            assertTrue("city ground above sea", ground > TerrainGenerator.SEA_LEVEL);
            assertTrue("city ground below the metro", ground < World.TRACK_Y - 2);
            assertTrue("cities stay apart", city[0] * city[0] + city[1] * city[1] > 0);
        }
    }

    @Test
    public void stationPlatformAndRailsExist() {
        TerrainGenerator g = new TerrainGenerator(SEED);
        CityGenerator cg = new CityGenerator(g);
        int[] city = CityGenerator.CITIES[0];
        int ccx = city[0] >> 4;
        int ccz = city[1] >> 4;
        int rails = 0;
        int platform = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk c = chunkAt(g, cg, ccx + dx, ccz + dz);
                rails += countAround(c, 0, 0, 15, 15, BlockType.RAIL_X.id,
                        World.TRACK_Y - 1, World.TRACK_Y + 1);
                rails += countAround(c, 0, 0, 15, 15, BlockType.RAIL_Z.id,
                        World.TRACK_Y - 1, World.TRACK_Y + 1);
                platform += countAround(c, 0, 0, 15, 15, BlockType.STONE_BRICK.id,
                        World.TRACK_Y - 2, World.TRACK_Y - 1);
            }
        }
        assertTrue("metro rails missing", rails > 0);
        assertTrue("station platform missing", platform > 20);
    }

    @Test
    public void cityCoreHasRoadsBuildingsAndPlazaWater() {
        TerrainGenerator g = new TerrainGenerator(SEED);
        CityGenerator cg = new CityGenerator(g);
        int[] city = CityGenerator.CITIES[0];
        int ground = cg.groundHeight(0);
        int ccx = city[0] >> 4;
        int ccz = city[1] >> 4;
        int road = 0;
        int walk = 0;
        int walls = 0;
        int glass = 0;
        int lamps = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                Chunk c = chunkAt(g, cg, ccx + dx, ccz + dz);
                for (byte b : c.blocks) {
                    int id = b & 0xff;
                    if (id == BlockType.ASPHALT.id) road++;
                    else if (id == BlockType.CONCRETE.id) walk++;
                    else if (id == BlockType.GLASS.id || id == BlockType.DARK_GLASS.id) glass++;
                    else if (id == BlockType.LAMP.id) lamps++;
                    else if (id == BlockType.RED_PLASTER.id || id == BlockType.BRICK.id
                            || id == BlockType.SANDSTONE.id) walls++;
                }
            }
        }
        assertTrue("no streets generated", road > 200);
        assertTrue("no sidewalks generated", walk > 200);
        assertTrue("no buildings generated", walls > 200);
        assertTrue("no windows generated", glass > 100);
        assertTrue("no street lamps generated", lamps > 4);
        // Fountain in the plaza south-east of the station.
        World world = new World(SEED, null, false);
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = 0; dz <= 3; dz++) {
                Chunk c = chunkAt(g, cg, ccx + dx, ccz + dz);
                c.generated = true;
                world.putChunkForTest(c);
            }
        }
        int water = 0;
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                int id = world.getBlock(city[0] + 10 + x, ground + 2, city[1] + 30 + z);
                if (id == BlockType.WATER.id) water++;
            }
        }
        assertTrue("plaza fountain missing", water > 0);
        world.shutdown();
    }

    @Test
    public void cityTerrainIsFlattenedAndBlendsOutwards() {
        TerrainGenerator g = new TerrainGenerator(SEED);
        CityGenerator cg = new CityGenerator(g);
        int[] city = CityGenerator.CITIES[0];
        int ground = cg.groundHeight(0);
        Chunk c = chunkAt(g, cg, city[0] >> 4, city[1] >> 4);
        // The centre of the city must sit on a flat plateau.
        int lumps = 0;
        for (int x = 1; x < 15; x++) {
            for (int z = 1; z < 15; z++) {
                for (int y = ground + 1; y < ground + 8; y++) {
                    int id = c.get(x, y, z);
                    if (id == BlockType.GRASS.id || id == BlockType.DIRT.id) lumps++;
                }
            }
        }
        assertTrue("unexpected dirt pile above the plateau", lumps == 0);
        assertTrue("natural terrain around the city still varies",
                g.surfaceHeight(city[0] + 140, city[1]) != g.surfaceHeight(city[0] - 140, city[1])
                        || g.surfaceHeight(city[0], city[1] + 140) != g.surfaceHeight(city[0], city[1] - 140));
    }

    @Test
    public void urbanBlocksAreRegisteredWithTextures() {
        for (int id = BlockType.ASPHALT.id; id <= BlockType.MAX_ID; id++) {
            BlockType b = BlockType.get(id);
            assertEquals("block " + id + " registered", id, b.id);
            assertTrue("block " + id + " has a texture", b.getTile(BlockType.FACE_PX) >= 0);
        }
        assertEquals(BlockType.ASPHALT.id, BlockType.get(BlockType.ASPHALT.id).id);
        assertTrue("plants do not block movement", !BlockType.isSolid(BlockType.TALL_GRASS.id));
        assertTrue("flowers are cross-shaped", BlockType.isCross(BlockType.FLOWER_RED.id));
        assertTrue("cactus blocks movement", BlockType.isSolid(BlockType.CACTUS.id));
    }
}
