package com.wangzi5151.myminiword.world;

import java.util.ArrayList;
import java.util.Random;

/**
 * Procedural city builder.
 *
 * Layout is a real street grid: 5-wide asphalt avenues every 20 blocks with
 * concrete sidewalks, 15-wide plots in between, a ring of suburbs, themed
 * landmarks, parks, a sunken plaza and an elevated metro station on the two
 * crossing rail lines. Every structure is authored in world space and clipped to
 * the chunk being generated, so the city appears progressively while streaming.
 */
public class CityGenerator {
    public static final int THEME_CHINESE = 0;
    public static final int THEME_EUROPEAN = 1;
    public static final int THEME_JAPANESE = 2;
    public static final int THEME_ARABIC = 3;
    public static final int THEME_MODERN = 4;
    public static final int THEME_NORDIC = 5;

    public static final int RADIUS = 64;     // paved city radius
    public static final int GRADE = 16;      // terrain blend ring
    public static final int SUBURB = 56;     // rural ring outside the city
    public static final int PITCH = 20;      // street pitch
    public static final int ROAD_HALF = 2;   // asphalt half width
    public static final int WALK = 3;        // sidewalk offset
    public static final int STAIR_X = 5;

    /** {x, z, theme}; positions are nudged onto flat land by {@link #configure}. */
    public static volatile int[][] CITIES = {
            {0, 0, THEME_CHINESE},
            {520, 80, THEME_EUROPEAN},
            {180, -520, THEME_JAPANESE},
            {-560, -160, THEME_ARABIC},
            {860, 480, THEME_MODERN},
            {-240, 760, THEME_NORDIC}
    };

    /** rail segments: {axis(0=alongX/z-fixed, 1=alongZ/x-fixed), fixed, min, max} */
    public static volatile int[][] SEGMENTS = buildSegments();

    private static volatile long configuredSeed = Long.MIN_VALUE;

    private final TerrainGenerator terrain;

    public CityGenerator(TerrainGenerator terrain) {
        this.terrain = terrain;
        configure(terrain);
    }

    // ------------------------------------------------------------------
    // City placement: anchors are moved onto the nearest buildable flat land.
    // ------------------------------------------------------------------

    public static synchronized void configure(TerrainGenerator t) {
        if (configuredSeed == t.getSeed()) return;
        int[][] anchors = {
                {0, 0, THEME_CHINESE},
                {520, 80, THEME_EUROPEAN},
                {180, -520, THEME_JAPANESE},
                {-560, -160, THEME_ARABIC},
                {860, 480, THEME_MODERN},
                {-240, 760, THEME_NORDIC}
        };
        ArrayList<int[]> out = new ArrayList<>();
        for (int[] anchor : anchors) {
            int[] site = findSite(t, anchor[0], anchor[1], out);
            out.add(new int[]{site[0], site[1], anchor[2]});
        }
        CITIES = out.toArray(new int[0][]);
        SEGMENTS = buildSegments();
        configuredSeed = t.getSeed();
    }

    private static int[] findSite(TerrainGenerator t, int ax, int az, ArrayList<int[]> taken) {
        int bestX = ax, bestZ = az;
        double bestScore = Double.MAX_VALUE;
        int sea = TerrainGenerator.SEA_LEVEL;
        for (int ring = 0; ring <= 20; ring++) {
            int r = ring * 8;
            int samples = ring == 0 ? 1 : 8 + ring * 2;
            for (int s = 0; s < samples; s++) {
                double ang = s * Math.PI * 2 / samples;
                int x = ax + (int) Math.round(Math.cos(ang) * r);
                int z = az + (int) Math.round(Math.sin(ang) * r);
                int h = t.surfaceHeight(x, z);
                if (h < sea + 3 || h > 44) continue;
                int rough = Math.abs(t.surfaceHeight(x + 8, z) - h)
                        + Math.abs(t.surfaceHeight(x - 8, z) - h)
                        + Math.abs(t.surfaceHeight(x, z + 8) - h)
                        + Math.abs(t.surfaceHeight(x, z - 8) - h);
                boolean tooClose = false;
                for (int[] other : taken) {
                    int dx = other[0] - x, dz = other[1] - z;
                    if (dx * dx + dz * dz < 240 * 240) tooClose = true;
                }
                double score = rough * 3.0 + h * 0.05 + Math.sqrt(r * r);
                if (!tooClose && score < bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestZ = z;
                }
            }
            if (bestScore < Double.MAX_VALUE) break;
        }
        return new int[]{bestX, bestZ};
    }

    private static int[][] buildSegments() {
        int[][] c = CITIES;
        ArrayList<int[]> list = new ArrayList<>();
        for (int i = 0; i < c.length - 1; i++) {
            int x0 = c[i][0], z0 = c[i][1];
            int x1 = c[i + 1][0], z1 = c[i + 1][1];
            list.add(new int[]{0, z0, Math.min(x0, x1), Math.max(x0, x1)});
            list.add(new int[]{1, x1, Math.min(z0, z1), Math.max(z0, z1)});
        }
        return list.toArray(new int[0][]);
    }

    // ------------------------------------------------------------------
    // Public entry points
    // ------------------------------------------------------------------

    public void populate(Chunk chunk, int baseX, int baseZ) {
        int[][] cities = CITIES;
        for (int i = 0; i < cities.length; i++) {
            int[] city = cities[i];
            int reach = RADIUS + GRADE + SUBURB;
            if (city[0] + reach < baseX || city[0] - reach > baseX + Chunk.SIZE_X - 1) continue;
            if (city[1] + reach < baseZ || city[1] - reach > baseZ + Chunk.SIZE_Z - 1) continue;
            buildCity(chunk, baseX, baseZ, city[0], city[1], city[2]);
        }
    }

    public int groundHeight(int index) {
        int[] city = CITIES[index];
        return targetHeight(city[0], city[1]);
    }

    public static double distanceToCity(int index, float x, float z) {
        int[] c = CITIES[index];
        float dx = c[0] - x, dz = c[1] - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private int targetHeight(int wx, int wz) {
        int h = terrain.surfaceHeight(wx, wz);
        h += terrain.surfaceHeight(wx + 8, wz);
        h += terrain.surfaceHeight(wx - 8, wz);
        h += terrain.surfaceHeight(wx, wz + 8);
        h += terrain.surfaceHeight(wx, wz - 8);
        h /= 5;
        if (h < TerrainGenerator.SEA_LEVEL + 4) h = TerrainGenerator.SEA_LEVEL + 4;
        if (h > 44) h = 44;
        return h;
    }

    // ------------------------------------------------------------------
    // City construction
    // ------------------------------------------------------------------

    private void buildCity(Chunk chunk, int baseX, int baseZ, int cityX, int cityZ, int theme) {
        final int target = targetHeight(cityX, cityZ);
        final int[] pal = palette(theme);
        final int wall = pal[0], wall2 = pal[1], roof = pal[2], accent = pal[3], ground = pal[4];

        shapeTerrain(chunk, baseX, baseZ, cityX, cityZ, target, theme, wall, roof, accent, ground);

        // Street furniture first so plazas, parks and landmarks can replace it.
        furniture(chunk, baseX, baseZ, cityX, cityZ, target, theme, accent);

        // --- reserved plots -------------------------------------------------
        buildStation(chunk, baseX, baseZ, cityX, cityZ, target);
        if (overlaps(baseX, baseZ, cityX - 22, cityZ + 18, cityX + 22, cityZ + 42)) {
            buildPlaza(chunk, baseX, baseZ, cityX + 10, cityZ + 30, target, accent, theme);
        }
        if (overlaps(baseX, baseZ, cityX - 22, cityZ - 22, cityX + 2, cityZ + 2)) {
            buildGardenSquare(chunk, baseX, baseZ, cityX - 10, cityZ - 10, target, theme);
        }
        if (overlaps(baseX, baseZ, cityX + 18, cityZ + 18, cityX + 42, cityZ + 42)) {
            buildPark(chunk, baseX, baseZ, cityX + 30, cityZ + 30, target, theme);
        }
        if (overlaps(baseX, baseZ, cityX - 42, cityZ - 42, cityX - 18, cityZ - 18)) {
            buildLandmark(chunk, baseX, baseZ, cityX - 30, cityZ - 30, target, theme,
                    wall, wall2, roof, accent);
        }

        parkedCars(chunk, baseX, baseZ, cityX, cityZ, target);

        // --- rooftop skyline / housing in the remaining plots --------------
        int cellR = (RADIUS / PITCH) + 2;
        for (int cx = -cellR; cx <= cellR; cx++) {
            for (int cz = -cellR; cz <= cellR; cz++) {
                if (cx == 0 || cz == 0) continue;             // avenue + rail corridors
                if ((cx == 0 && cz == 1) || (cx == 1 && cz == 1)
                        || (cx == -1 && cz == -1) || (cx == -2 && cz == -2)) continue;
                int ccx = cityX + cx * PITCH + PITCH / 2;
                int ccz = cityZ + cz * PITCH + PITCH / 2;
                double dist = Math.hypot(ccx - cityX, ccz - cityZ);
                if (dist > RADIUS - 2) continue;
                Random r = new Random(cityX * 92837111L + cityZ * 689287499L
                        + cx * 7919L + cz * 104729L + theme * 31L);
                if (dist < 26) {
                    buildTowerPlot(chunk, baseX, baseZ, ccx, ccz, target, theme, pal, r, 20 + r.nextInt(12));
                } else if (dist < 46) {
                    if (r.nextBoolean()) {
                        buildTowerPlot(chunk, baseX, baseZ, ccx, ccz, target, theme, pal, r, 10 + r.nextInt(10));
                    } else {
                        buildShopPlot(chunk, baseX, baseZ, ccx, ccz, target, theme, pal, r);
                    }
                } else {
                    buildHousePlot(chunk, baseX, baseZ, ccx, ccz, target, theme, pal, r);
                }
            }
        }

        // --- suburbs ------------------------------------------------------
        suburbs(chunk, baseX, baseZ, cityX, cityZ, target, theme, pal);

        // --- ring wall for the walled themes --------------------------------
        if (theme == THEME_CHINESE || theme == THEME_EUROPEAN) {
            buildWall(chunk, baseX, baseZ, cityX, cityZ, target, theme, wall2, accent);
        }
    }

    /** Flattens and paves the city footprint, blending into the natural terrain. */
    private void shapeTerrain(Chunk chunk, int baseX, int baseZ, int cityX, int cityZ,
                              int target, int theme, int wall, int roof, int accent, int ground) {
        int reach = RADIUS + GRADE;
        int x0 = Math.max(baseX, cityX - reach);
        int x1 = Math.min(baseX + Chunk.SIZE_X - 1, cityX + reach);
        int z0 = Math.max(baseZ, cityZ - reach);
        int z1 = Math.min(baseZ + Chunk.SIZE_Z - 1, cityZ + reach);
        if (x0 > x1 || z0 > z1) return;

        int sea = TerrainGenerator.SEA_LEVEL;
        for (int wx = x0; wx <= x1; wx++) {
            for (int wz = z0; wz <= z1; wz++) {
                double dist = Math.hypot(wx - cityX, wz - cityZ);
                if (dist > reach) continue;
                float t = TerrainGenerator.smoothstep(reach, RADIUS - 8f, (float) dist);
                int natural = terrain.surfaceHeight(wx, wz);
                int y = Math.round(natural + (target - natural) * t);
                if (y < sea + 1) y = sea + 1;

                int lx = wx - baseX, lz = wz - baseZ;

                // Clear sky (trees, snow overhangs, previous terrain detail).
                for (int yy = y + 1; yy < Chunk.SIZE_Y; yy++) chunk.set(lx, yy, lz, (byte) 0);

                // Body.
                for (int yy = 0; yy < y - 3; yy++) {
                    int id = chunk.get(lx, yy, lz);
                    if (id == 0 || BlockType.isLiquid(id)) chunk.set(lx, yy, lz, (byte) BlockType.STONE.id);
                }
                for (int yy = Math.max(1, y - 3); yy < y; yy++) {
                    int id = chunk.get(lx, yy, lz);
                    if (id == 0 || BlockType.isLiquid(id)) {
                        chunk.set(lx, yy, lz, (byte) (theme == THEME_ARABIC
                                ? BlockType.SANDSTONE.id : BlockType.DIRT.id));
                    }
                }

                int relX = wx - cityX, relZ = wz - cityZ;
                int roadDist = Math.min(roadDist(relX), roadDist(relZ));
                boolean inCity = dist <= RADIUS - 1;
                int surface;
                if (inCity && roadDist <= ROAD_HALF) {
                    surface = BlockType.ASPHALT.id;
                    boolean both = roadDist(relX) <= ROAD_HALF && roadDist(relZ) <= ROAD_HALF;
                    if (both) {
                        if (((wx + wz) & 3) == 0) surface = BlockType.CONCRETE.id;   // crosswalk
                    } else if (roadDist(relX) == 0 || roadDist(relZ) == 0) {
                        if (Math.floorMod(wx + wz, 7) < 3) surface = BlockType.CONCRETE.id; // dashes
                    }
                } else if (inCity && roadDist == WALK) {
                    surface = BlockType.CONCRETE.id;
                } else if (inCity) {
                    surface = (theme == THEME_ARABIC) ? BlockType.SAND.id
                            : (theme == THEME_MODERN ? BlockType.GRASS.id : ground);
                } else {
                    surface = (natural > TerrainGenerator.SEA_LEVEL + 2)
                            ? (theme == THEME_ARABIC ? BlockType.SAND.id : BlockType.GRASS.id)
                            : BlockType.SAND.id;
                }
                chunk.set(lx, y, lz, (byte) surface);
            }
        }
    }

    private static int roadDist(int rel) {
        int m = Math.floorMod(rel, PITCH);
        return Math.min(m, PITCH - m);
    }

    private void furniture(Chunk chunk, int baseX, int baseZ, int cityX, int cityZ,
                           int target, int theme, int accent) {
        int reach = RADIUS - 2;
        int x0 = Math.max(baseX, cityX - reach);
        int x1 = Math.min(baseX + Chunk.SIZE_X - 1, cityX + reach);
        int z0 = Math.max(baseZ, cityZ - reach);
        int z1 = Math.min(baseZ + Chunk.SIZE_Z - 1, cityZ + reach);
        for (int wx = x0; wx <= x1; wx++) {
            for (int wz = z0; wz <= z1; wz++) {
                int relX = wx - cityX, relZ = wz - cityZ;
                int dxr = roadDist(relX), dzr = roadDist(relZ);
                boolean onWalkX = dxr == WALK;
                boolean onWalkZ = dzr == WALK;
                if (!onWalkX && !onWalkZ) continue;
                if (onWalkX && onWalkZ) continue;
                int along = onWalkX ? relZ : relX;
                int side = onWalkX ? Integer.signum(relX) : Integer.signum(relZ);

                // Street lamps every 12 blocks.
                if (Math.floorMod(along, 12) == 0) {
                    int lx = wx, lz = wz;
                    for (int i = 1; i <= 4; i++) put(chunk, baseX, baseZ, lx, target + i, lz, BlockType.STEEL.id);
                    put(chunk, baseX, baseZ, lx, target + 5, lz, BlockType.LAMP.id);
                    continue;
                }
                // Planter trees every 10 blocks (offset from lamps).
                if (Math.floorMod(along + 5, 10) == 0) {
                    put(chunk, baseX, baseZ, wx, target, wz, BlockType.DIRT.id);
                    put(chunk, baseX, baseZ, wx, target + 1, wz, BlockType.LOG.id);
                    for (int i = 2; i <= 4; i++) put(chunk, baseX, baseZ, wx, target + i, wz, BlockType.LOG.id);
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            if (dx * dx + dz * dz > 5) continue;
                            putSoft(chunk, baseX, baseZ, wx + dx, target + 5, wz + dz,
                                    theme == THEME_JAPANESE ? BlockType.JUNGLE_LEAVES.id : BlockType.LEAVES.id);
                        }
                    }
                    continue;
                }
                // Benches and bins.
                if (Math.floorMod(along + 2, 16) == 0) {
                    put(chunk, baseX, baseZ, wx, target + 1, wz, BlockType.PLANKS.id);
                    put(chunk, baseX, baseZ, wx + (onWalkX ? 0 : 1), target + 2, wz + (onWalkX ? 1 : 0),
                            BlockType.PLANKS.id);
                }
                if (Math.floorMod(along + 9, 24) == 0) {
                    put(chunk, baseX, baseZ, wx, target + 1, wz, BlockType.STEEL.id);
                }
                // Hanging flower boxes on the shop side.
                if (theme != THEME_MODERN && Math.floorMod(along + 3, 11) == 0) {
                    put(chunk, baseX, baseZ, wx, target + 1, wz, BlockType.HEDGE.id);
                    put(chunk, baseX, baseZ, wx, target + 2, wz, BlockType.FLOWER_RED.id);
                }
                // Occasional vending kiosk.
                if (Math.floorMod(along, 31) == 0 && side != 0) {
                    put(chunk, baseX, baseZ, wx, target + 1, wz, BlockType.BLUE_PLASTER.id);
                    put(chunk, baseX, baseZ, wx, target + 2, wz, BlockType.DARK_GLASS.id);
                    put(chunk, baseX, baseZ, wx, target + 3, wz, accent);
                }
            }
        }
    }

    /** Parked cars along the kerb lanes: small, cheap, and they sell the scale. */
    private void parkedCars(Chunk chunk, int baseX, int baseZ, int cityX, int cityZ, int target) {
        int[] bodyColors = {BlockType.RED_PLASTER.id, BlockType.BLUE_PLASTER.id,
                BlockType.YELLOW_PLASTER.id, BlockType.WHITE_PLASTER.id, BlockType.DARK_PLASTER.id};
        for (int axis = 0; axis < 2; axis++) {
            for (int k = -3; k <= 3; k++) {
                int line = (axis == 0 ? cityZ : cityX) + k * PITCH;
                for (int along = -RADIUS + 8; along < RADIUS - 8; along += 17) {
                    Random r = new Random(line * 7919L + along * 104729L + axis * 31L);
                    if (r.nextInt(3) != 0) continue;
                    int ax = axis == 0 ? cityX + along : line;
                    int az = axis == 0 ? line : cityZ + along;
                    int lane = r.nextBoolean() ? 1 : -1;
                    if (axis == 0) az += lane * 1;
                    else ax += lane * 1;
                    if (Math.hypot(ax - cityX, az - cityZ) > RADIUS - 5) continue;
                    if (!overlaps(baseX, baseZ, Math.min(ax, ax + (axis == 0 ? 3 : 0)) - 1,
                            Math.min(az, az + (axis == 0 ? 0 : 3)) - 1,
                            Math.max(ax, ax + (axis == 0 ? 3 : 0)) + 1,
                            Math.max(az, az + (axis == 0 ? 0 : 3)) + 1)) continue;
                    if (chunk.get(ax - baseX, target, az - baseZ) != BlockType.ASPHALT.id) continue;
                    int body = bodyColors[r.nextInt(bodyColors.length)];
                    int len = 3;
                    for (int i = 0; i < len; i++) {
                        for (int w = 0; w < 2; w++) {
                            int cx = axis == 0 ? ax + i : ax + w * lane;
                            int cz = axis == 0 ? az + w * lane : az + i;
                            put(chunk, baseX, baseZ, cx, target + 1, cz, body);
                            boolean cabin = (i == 1);
                            put(chunk, baseX, baseZ, cx, target + 2, cz,
                                    cabin ? BlockType.DARK_GLASS.id : body);
                        }
                    }
                    // Bumpers.
                    int bx0 = axis == 0 ? ax - 1 : ax;
                    int bz0 = axis == 0 ? az : az - 1;
                    put(chunk, baseX, baseZ, bx0, target + 1, bz0, BlockType.STEEL.id);
                }
            }
        }
    }

    private void buildPlaza(Chunk chunk, int baseX, int baseZ, int cx, int cz, int target,
                            int accent, int theme) {
        int radius = 10;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > radius * radius) continue;
                int y = target;
                int id = ((Math.abs(dx) + Math.abs(dz)) % 4 == 0)
                        ? BlockType.CONCRETE.id : BlockType.STONE_BRICK.id;
                if (d2 <= 4) continue;
                chunk.set(cx + dx - baseX, y, cz + dz - baseZ, (byte) id);
                if (d2 > (radius - 2) * (radius - 2)) {
                    put(chunk, baseX, baseZ, cx + dx, y + 1, cz + dz,
                            (Math.max(Math.abs(dx), Math.abs(dz)) % 3 == 0) ? BlockType.HEDGE.id
                                    : BlockType.FLOWER_YELLOW.id);
                }
            }
        }
        // Fountain: stone rim, water basin, centre column + lamp.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > 9) continue;
                if (d2 >= 7) {
                    put(chunk, baseX, baseZ, cx + dx, target + 1, cz + dz, BlockType.STONE_BRICK.id);
                } else {
                    put(chunk, baseX, baseZ, cx + dx, target, cz + dz, BlockType.WATER.id);
                }
            }
        }
        put(chunk, baseX, baseZ, cx, target, cz, BlockType.STONE_BRICK.id);
        put(chunk, baseX, baseZ, cx, target + 1, cz, BlockType.STONE_BRICK.id);
        put(chunk, baseX, baseZ, cx, target + 2, cz, BlockType.WATER.id);
        put(chunk, baseX, baseZ, cx, target + 3, cz, BlockType.LAMP.id);

        // Benches, lamps and trees around the plaza rim.
        for (int a = 0; a < 8; a++) {
            double ang = a * Math.PI / 4;
            int bx = cx + (int) Math.round(Math.cos(ang) * (radius - 3));
            int bz = cz + (int) Math.round(Math.sin(ang) * (radius - 3));
            if ((a & 1) == 0) {
                put(chunk, baseX, baseZ, bx, target + 1, bz, BlockType.PLANKS.id);
                put(chunk, baseX, baseZ, bx + 1, target + 1, bz, BlockType.PLANKS.id);
                put(chunk, baseX, baseZ, bx, target + 2, bz, BlockType.PLANKS.id);
            } else {
                put(chunk, baseX, baseZ, bx, target + 1, bz, BlockType.STEEL.id);
                put(chunk, baseX, baseZ, bx, target + 2, bz, BlockType.STEEL.id);
                put(chunk, baseX, baseZ, bx, target + 3, bz, BlockType.STEEL.id);
                put(chunk, baseX, baseZ, bx, target + 4, bz, BlockType.LAMP.id);
            }
        }
        for (int a = 0; a < 4; a++) {
            int tx = cx + (int) Math.round(Math.cos(a * Math.PI / 2 + 0.4) * (radius - 5));
            int tz = cz + (int) Math.round(Math.sin(a * Math.PI / 2 + 0.4) * (radius - 5));
            streetTree(chunk, baseX, baseZ, tx, target + 1, tz, 4);
        }
        // Market stalls facing the fountain.
        for (int a = 0; a < 4; a++) {
            double ang = a * Math.PI / 2 + 0.8;
            int sx = cx + (int) Math.round(Math.cos(ang) * 8);
            int sz = cz + (int) Math.round(Math.sin(ang) * 8);
            marketStall(chunk, baseX, baseZ, sx, target + 1, sz, accent, a);
        }
    }

    private void marketStall(Chunk chunk, int baseX, int baseZ, int x, int y, int z,
                             int accent, int variant) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
                    put(chunk, baseX, baseZ, x + dx, y, z + dz, BlockType.BIRCH_LOG.id);
                    put(chunk, baseX, baseZ, x + dx, y + 1, z + dz, BlockType.BIRCH_LOG.id);
                    put(chunk, baseX, baseZ, x + dx, y + 2, z + dz, BlockType.BIRCH_LOG.id);
                } else if (dx == 0 && dz == 0) {
                    put(chunk, baseX, baseZ, x, y, z, BlockType.PLANKS.id);
                    put(chunk, baseX, baseZ, x, y + 1, z, BlockType.HEDGE.id);
                } else {
                    put(chunk, baseX, baseZ, x + dx, y, z + dz, BlockType.PLANKS.id);
                }
                put(chunk, baseX, baseZ, x + dx, y + 3, z + dz,
                        ((dx + dz + variant) & 1) == 0 ? accent : BlockType.WHITE_PLASTER.id);
            }
        }
        put(chunk, baseX, baseZ, x, y + 1, z + 1, BlockType.FLOWER_YELLOW.id);
        put(chunk, baseX, baseZ, x - 1, y + 1, z, BlockType.FLOWER_RED.id);
    }

    private void buildGardenSquare(Chunk chunk, int baseX, int baseZ, int cx, int cz,
                                   int target, int theme) {
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (dx * dx + dz * dz > 36) continue;
                chunk.set(cx + dx - baseX, target, cz + dz - baseZ, (byte) BlockType.GRASS.id);
                if ((Math.abs(dx) <= 1 || Math.abs(dz) <= 1) && (Math.abs(dx) + Math.abs(dz)) < 9) {
                    chunk.set(cx + dx - baseX, target, cz + dz - baseZ, (byte) BlockType.CONCRETE.id);
                }
            }
        }
        for (int a = 0; a < 6; a++) {
            double ang = a * Math.PI / 3 + 0.2;
            int tx = cx + (int) Math.round(Math.cos(ang) * 5);
            int tz = cz + (int) Math.round(Math.sin(ang) * 5);
            streetTree(chunk, baseX, baseZ, tx, target + 1, tz, 3);
        }
        // Small pavilion in the middle.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                put(chunk, baseX, baseZ, cx + dx, target + 3, cz + dz,
                        (Math.abs(dx) == 2 || Math.abs(dz) == 2) ? BlockType.ROOF_SLATE.id
                                : BlockType.PLANKS.id);
            }
        }
        put(chunk, baseX, baseZ, cx - 2, target + 1, cz - 2, BlockType.LOG.id);
        put(chunk, baseX, baseZ, cx + 2, target + 1, cz - 2, BlockType.LOG.id);
        put(chunk, baseX, baseZ, cx - 2, target + 1, cz + 2, BlockType.LOG.id);
        put(chunk, baseX, baseZ, cx + 2, target + 1, cz + 2, BlockType.LOG.id);
        put(chunk, baseX, baseZ, cx - 2, target + 2, cz - 2, BlockType.LOG.id);
        put(chunk, baseX, baseZ, cx + 2, target + 2, cz - 2, BlockType.LOG.id);
        put(chunk, baseX, baseZ, cx - 2, target + 2, cz + 2, BlockType.LOG.id);
        put(chunk, baseX, baseZ, cx + 2, target + 2, cz + 2, BlockType.LOG.id);
    }

    private void buildPark(Chunk chunk, int baseX, int baseZ, int cx, int cz, int target, int theme) {
        int radius = 11;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int x = cx + dx, z = cz + dz;
                boolean path = Math.abs(dx) <= 1 || Math.abs(dz) <= 1
                        || (dx * dx + dz * dz >= (radius - 2) * (radius - 2) && (dx + dz) % 2 == 0);
                if (path) {
                    put(chunk, baseX, baseZ, x, target, z, BlockType.GRAVEL.id);
                    put(chunk, baseX, baseZ, x, target + 1, z, 0);
                } else {
                    put(chunk, baseX, baseZ, x, target, z, BlockType.GRASS.id);
                }
            }
        }
        // Pond with a stone edge and a little bridge.
        int px = cx - 4, pz = cz - 3;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double d = dx * dx / 16.0 + dz * dz / 9.0;
                if (d <= 1.0) {
                    put(chunk, baseX, baseZ, px + dx, target, pz + dz, BlockType.WATER.id);
                    put(chunk, baseX, baseZ, px + dx, target - 1, pz + dz, BlockType.CLAY.id);
                } else if (d <= 1.5) {
                    put(chunk, baseX, baseZ, px + dx, target, pz + dz, BlockType.GRAVEL.id);
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            put(chunk, baseX, baseZ, px + dx, target, pz - 1, BlockType.PLANKS.id);
            put(chunk, baseX, baseZ, px + dx, target + 1, pz - 1, 0);
        }
        // Gazebo.
        int gx = cx + 5, gz = cz + 5;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                put(chunk, baseX, baseZ, gx + dx, target + 3, gz + dz,
                        (Math.abs(dx) == 2 || Math.abs(dz) == 2) ? BlockType.ROOF_TILE.id : BlockType.PLANKS.id);
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                    for (int y = 1; y <= 2; y++) put(chunk, baseX, baseZ, gx + dx, target + y, gz + dz, BlockType.BIRCH_LOG.id);
                }
            }
        }
        put(chunk, baseX, baseZ, gx, target + 4, gz, BlockType.LAMP.id);
        for (int a = 0; a < 7; a++) {
            double ang = a * Math.PI * 2 / 7;
            int tx = cx + (int) Math.round(Math.cos(ang) * 7);
            int tz = cz + (int) Math.round(Math.sin(ang) * 7);
            if (chunk.get(tx - baseX, target + 1, tz - baseZ) == 0) {
                streetTree(chunk, baseX, baseZ, tx, target + 1, tz, 3);
            }
        }
        for (int a = 0; a < 10; a++) {
            double ang = a * Math.PI * 2 / 10;
            int fx = cx + (int) Math.round(Math.cos(ang) * 9);
            int fz = cz + (int) Math.round(Math.sin(ang) * 9);
            put(chunk, baseX, baseZ, fx, target + 1, fz,
                    (a % 2 == 0) ? BlockType.FLOWER_RED.id : BlockType.FLOWER_YELLOW.id);
        }
    }

    private void streetTree(Chunk chunk, int baseX, int baseZ, int x, int y, int z, int height) {
        for (int i = 0; i < height; i++) put(chunk, baseX, baseZ, x, y + i, z, BlockType.LOG.id);
        for (int dy = -1; dy <= 1; dy++) {
            int r = dy == 1 ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + 1) continue;
                    putSoft(chunk, baseX, baseZ, x + dx, y + height + dy, z + dz, BlockType.LEAVES.id);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Buildings
    // ------------------------------------------------------------------

    private void buildTowerPlot(Chunk chunk, int baseX, int baseZ, int cx, int cz, int target,
                                int theme, int[] pal, Random r, int height) {
        int w = 9 + r.nextInt(5);
        int d = 9 + r.nextInt(5);
        if (!overlaps(baseX, baseZ, cx - w / 2 - 1, cz - d / 2 - 1, cx + w / 2 + 1, cz + d / 2 + 1)) return;
        buildTower(chunk, baseX, baseZ, cx - w / 2, target, cz - d / 2, w, height, d,
                pal[0], pal[1], pal[2], pal[3], theme);
    }

    private void buildShopPlot(Chunk chunk, int baseX, int baseZ, int cx, int cz, int target,
                               int theme, int[] pal, Random r) {
        int w = 10 + r.nextInt(4);
        int d = 8 + r.nextInt(4);
        int h = 5 + r.nextInt(4);
        if (!overlaps(baseX, baseZ, cx - w / 2 - 2, cz - d / 2 - 2, cx + w / 2 + 2, cz + d / 2 + 2)) return;
        int x0 = cx - w / 2, z0 = cz - d / 2;
        buildShop(chunk, baseX, baseZ, x0, target, z0, w, h, d, pal[0], pal[1], pal[2], pal[3], theme, r);
        // Terrace / roof clutter.
        roofClutter(chunk, baseX, baseZ, x0, target + h, z0, w, d, r);
    }

    private void buildHousePlot(Chunk chunk, int baseX, int baseZ, int cx, int cz, int target,
                                int theme, int[] pal, Random r) {
        int count = r.nextBoolean() ? 1 : 2;
        for (int i = 0; i < count; i++) {
            int offX = count == 1 ? 0 : (i == 0 ? -4 : 4);
            int offZ = count == 1 ? 0 : (r.nextBoolean() ? -3 : 3);
            int hx = cx + offX, hz = cz + offZ;
            int w = 5 + r.nextInt(3);
            int d = 5 + r.nextInt(3);
            int h = 4 + r.nextInt(2);
            if (!overlaps(baseX, baseZ, hx - w / 2 - 4, hz - d / 2 - 4, hx + w / 2 + 4, hz + d / 2 + 4)) continue;
            buildHouse(chunk, baseX, baseZ, hx - w / 2, target, hz - d / 2, w, h, d,
                    pal[0], pal[1], pal[2], pal[3], theme, r);
            yard(chunk, baseX, baseZ, hx, hz, w, d, target, theme, r);
        }
    }

    private void buildTower(Chunk chunk, int baseX, int baseZ, int bx, int y0, int bz,
                            int w, int h, int d, int wall, int wall2, int roof, int accent, int theme) {
        int maxH = Chunk.SIZE_Y - 10 - y0;
        if (h > maxH) h = maxH;
        if (h < 4) return;
        int tiers = h >= 26 ? 3 : (h >= 16 ? 2 : 1);
        int curW = w, curD = d, curX = bx, curZ = bz, y = y0;
        int remaining = h;
        for (int t = 0; t < tiers && remaining > 0; t++) {
            int seg = (t == tiers - 1) ? remaining : Math.max(5, remaining / (tiers - t));
            towerSegment(chunk, baseX, baseZ, curX, y, curZ, curW, seg, curD,
                    wall, wall2, roof, accent, t == 0, theme);
            y += seg;
            remaining -= seg;
            if (remaining <= 0) break;
            // Setback with a planted terrace.
            curX += 1;
            curZ += 1;
            curW = Math.max(4, curW - 2);
            curD = Math.max(4, curD - 2);
            for (int x = curX - 1; x <= curX + curW; x++) {
                for (int z = curZ - 1; z <= curZ + curD; z++) {
                    put(chunk, baseX, baseZ, x, y, z, (x == curX - 1 || x == curX + curW
                            || z == curZ - 1 || z == curZ + curD) ? BlockType.HEDGE.id : BlockType.CONCRETE.id);
                }
            }
            y += 1;
        }
        roofClutter(chunk, baseX, baseZ, curX, y, curZ, curW, curD, new Random(bx * 31L + bz * 17L));
    }

    private void towerSegment(Chunk chunk, int baseX, int baseZ, int bx, int y0, int bz,
                              int w, int h, int d, int wall, int wall2, int roof, int accent,
                              boolean ground, int theme) {
        boolean glassy = theme == THEME_MODERN || theme == THEME_ARABIC;
        int glass = (theme == THEME_MODERN) ? BlockType.DARK_GLASS.id : BlockType.GLASS.id;
        for (int x = bx; x < bx + w; x++) {
            for (int z = bz; z < bz + d; z++) {
                boolean perim = (x == bx || x == bx + w - 1 || z == bz || z == bz + d - 1);
                boolean corner = (x == bx || x == bx + w - 1) && (z == bz || z == bz + d - 1);
                for (int y = y0; y < y0 + h; y++) {
                    int rel = y - y0;
                    if (!perim) {
                        boolean lift = Math.abs(x - (bx + w / 2)) <= 0 && Math.abs(z - (bz + d / 2)) <= 0;
                        if (rel % 4 == 3) put(chunk, baseX, baseZ, x, y, z, BlockType.CONCRETE.id);
                        else if (lift) put(chunk, baseX, baseZ, x, y, z, BlockType.STEEL.id);
                        else put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean slab = (rel % 4 == 3);
                    boolean mullion = ((x + z) % 3 == 0);
                    boolean store = ground && rel < 3;
                    int id;
                    if (corner) id = accent;
                    else if (slab) id = accent;
                    else if (store) {
                        id = (rel == 2) ? accent : glass;
                    } else if (glassy) {
                        id = mullion ? wall2 : glass;
                    } else {
                        id = mullion ? wall : glass;
                    }
                    put(chunk, baseX, baseZ, x, y, z, id);
                }
            }
        }
        // Entrance + awning.
        int doorX = bx + w / 2;
        for (int y = y0; y < y0 + 2; y++) {
            put(chunk, baseX, baseZ, doorX, y, bz, 0);
            put(chunk, baseX, baseZ, doorX + 1, y, bz, 0);
        }
        for (int x = doorX - 1; x <= doorX + 2; x++) {
            put(chunk, baseX, baseZ, x, y0 + 2, bz - 1, accent);
            put(chunk, baseX, baseZ, x, y0 + 3, bz - 1, (x & 1) == 0 ? accent : wall2);
        }
        for (int x = bx; x < bx + w; x++) put(chunk, baseX, baseZ, x, y0 + h, bz + d - 1, roof);
    }

    private void roofClutter(Chunk chunk, int baseX, int baseZ, int bx, int y, int bz, int w, int d, Random r) {
        // Roof deck + parapet.
        for (int x = bx - 1; x <= bx + w; x++) {
            for (int z = bz - 1; z <= bz + d; z++) {
                put(chunk, baseX, baseZ, x, y, z, BlockType.CONCRETE.id);
            }
        }
        int cx = bx + w / 2, cz = bz + d / 2;
        // AC units.
        for (int i = 0; i < 2 + r.nextInt(3); i++) {
            int ux = bx + 1 + r.nextInt(Math.max(1, w - 2));
            int uz = bz + 1 + r.nextInt(Math.max(1, d - 2));
            put(chunk, baseX, baseZ, ux, y + 1, uz, BlockType.STEEL.id);
            put(chunk, baseX, baseZ, ux, y + 2, uz, BlockType.STEEL.id);
        }
        // Water tank.
        if (r.nextBoolean()) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = 1; dy <= 2; dy++) {
                        put(chunk, baseX, baseZ, cx + dx, y + dy, cz + dz, BlockType.STEEL.id);
                    }
                }
            }
            put(chunk, baseX, baseZ, cx, y + 3, cz, BlockType.STEEL.id);
        }
        // Antenna + beacon.
        for (int i = 1; i <= 2 + r.nextInt(3); i++) put(chunk, baseX, baseZ, cx, y + i, cz, BlockType.STEEL.id);
        put(chunk, baseX, baseZ, cx, y + 4, cz, BlockType.LAMP.id);
        // Roof garden.
        if (r.nextInt(3) == 0) {
            for (int x = bx + 1; x < bx + w - 1; x++) {
                for (int z = bz + 1; z < bz + d - 1; z++) {
                    if (r.nextFloat() < 0.25f) put(chunk, baseX, baseZ, x, y + 1, z, BlockType.HEDGE.id);
                }
            }
        }
    }

    private void buildShop(Chunk chunk, int baseX, int baseZ, int bx, int y0, int bz,
                           int w, int h, int d, int wall, int wall2, int roof, int accent,
                           int theme, Random r) {
        for (int x = bx; x < bx + w; x++) {
            for (int z = bz; z < bz + d; z++) {
                boolean perim = (x == bx || x == bx + w - 1 || z == bz || z == bz + d - 1);
                for (int y = y0; y < y0 + h; y++) {
                    int rel = y - y0;
                    if (!perim) {
                        boolean lift = Math.abs(x - (bx + w / 2)) <= 0 && Math.abs(z - (bz + d / 2)) <= 0;
                        if (rel % 4 == 3) put(chunk, baseX, baseZ, x, y, z, BlockType.CONCRETE.id);
                        else if (lift) put(chunk, baseX, baseZ, x, y, z, BlockType.STEEL.id);
                        else put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean frame = (x == bx || x == bx + w - 1) && (z == bz || z == bz + d - 1);
                    int id;
                    if (frame) id = wall2;
                    else if (rel == 0) id = BlockType.STONE_BRICK.id;
                    else if (rel == 1) id = BlockType.GLASS.id;
                    else if (rel % 3 == 0) id = accent;
                    else id = ((x + z) % 4 == 0) ? BlockType.GLASS.id : wall;
                    put(chunk, baseX, baseZ, x, y, z, id);
                }
                put(chunk, baseX, baseZ, x, y0 + h, z, roof);
            }
        }
        // Shop front: big window, door and striped awning.
        int doorX = bx + w / 2;
        for (int x = bx + 1; x < bx + w - 1; x++) {
            for (int y = y0 + 1; y <= y0 + 2; y++) put(chunk, baseX, baseZ, x, y, bz, BlockType.GLASS.id);
        }
        put(chunk, baseX, baseZ, doorX, y0, bz, 0);
        put(chunk, baseX, baseZ, doorX, y0 + 1, bz, 0);
        for (int x = bx; x < bx + w; x++) {
            put(chunk, baseX, baseZ, x, y0 + 3, bz - 1,
                    ((x - bx) & 1) == 0 ? BlockType.RED_PLASTER.id : BlockType.WHITE_PLASTER.id);
            put(chunk, baseX, baseZ, x, y0 + 3, bz - 2,
                    ((x - bx) & 1) == 0 ? BlockType.RED_PLASTER.id : BlockType.WHITE_PLASTER.id);
        }
        // Sign above the shop.
        put(chunk, baseX, baseZ, bx + 1, y0 + 4, bz - 1, BlockType.GLOWSTONE.id);
        put(chunk, baseX, baseZ, bx + 2, y0 + 4, bz - 1, BlockType.GLOWSTONE.id);
        put(chunk, baseX, baseZ, bx + w - 2, y0 + 4, bz - 1, accent);
        // Interior: counter + shelves.
        for (int x = bx + 1; x < bx + w - 1; x += 2) put(chunk, baseX, baseZ, x, y0, bz + 2, BlockType.PLANKS.id);
        put(chunk, baseX, baseZ, bx + 1, y0, bz + 1, BlockType.BOOKSHELF.id);
        put(chunk, baseX, baseZ, bx + w - 2, y0, bz + 1, BlockType.BOOKSHELF.id);
        put(chunk, baseX, baseZ, bx + w / 2, y0 + h - 1, bz + d / 2, BlockType.LAMP.id);
    }

    private void buildHouse(Chunk chunk, int baseX, int baseZ, int bx, int y0, int bz,
                            int w, int h, int d, int wall, int wall2, int roof, int accent,
                            int theme, Random r) {
        int doorX = bx + w / 2;
        for (int x = bx; x < bx + w; x++) {
            for (int z = bz; z < bz + d; z++) {
                boolean perim = (x == bx || x == bx + w - 1 || z == bz || z == bz + d - 1);
                boolean corner = (x == bx || x == bx + w - 1) && (z == bz || z == bz + d - 1);
                for (int y = y0; y < y0 + h; y++) {
                    int rel = y - y0;
                    if (!perim) {
                        boolean lift = Math.abs(x - (bx + w / 2)) <= 0 && Math.abs(z - (bz + d / 2)) <= 0;
                        if (rel % 4 == 3) put(chunk, baseX, baseZ, x, y, z, BlockType.CONCRETE.id);
                        else if (lift) put(chunk, baseX, baseZ, x, y, z, BlockType.STEEL.id);
                        else put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean base = (rel == 0);
                    boolean band = (h >= 5) && (rel == h - 1);
                    boolean door = (z == bz) && (x == doorX) && (rel < 2);
                    if (door) {
                        put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean win = !corner && !band && rel > 0 && ((x + z) % 3 == 0);
                    int id;
                    if (base) id = BlockType.STONE_BRICK.id;
                    else if (corner || band) id = wall2;
                    else if (win) id = BlockType.GLASS.id;
                    else id = wall;
                    put(chunk, baseX, baseZ, x, y, z, id);
                }
            }
        }
        // Gable roof with overhang, ridge and chimney.
        int x0 = bx - 1, z0 = bz - 1, rw = w + 2, rd = d + 2;
        int baseY = y0 + h;
        int ridgeX = x0 + rw / 2;
        for (int x = x0; x < x0 + rw; x++) {
            int rise = Math.max(0, rw / 2 - Math.abs(x - ridgeX));
            for (int z = z0; z < z0 + rd; z++) {
                for (int y = baseY; y <= baseY + rise; y++) put(chunk, baseX, baseZ, x, y, z, roof);
            }
        }
        for (int z = z0; z < z0 + rd; z++) put(chunk, baseX, baseZ, ridgeX, baseY + rw / 2, z, accent);
        int chimneyX = bx + (w > 5 ? 1 : 0);
        for (int y = baseY; y <= baseY + rw / 2 + 1; y++) {
            put(chunk, baseX, baseZ, chimneyX, y, bz + d / 2, BlockType.BRICK.id);
        }
        // Porch.
        for (int x = doorX - 1; x <= doorX + 1; x++) {
            put(chunk, baseX, baseZ, x, y0 + 3, bz - 1, roof);
            put(chunk, baseX, baseZ, x, y0 + 3, bz - 2, roof);
        }
        put(chunk, baseX, baseZ, doorX - 1, y0 + 1, bz - 1, BlockType.BIRCH_LOG.id);
        put(chunk, baseX, baseZ, doorX + 1, y0 + 1, bz - 1, BlockType.BIRCH_LOG.id);
        put(chunk, baseX, baseZ, doorX, y0, bz - 1, BlockType.CONCRETE.id);
        // Balcony across the back facade on the upper floor.
        if (h >= 5) {
            for (int x = bx + 1; x < bx + w - 1; x++) {
                put(chunk, baseX, baseZ, x, y0 + 3, bz + d, BlockType.PLANKS.id);
                put(chunk, baseX, baseZ, x, y0 + 4, bz + d, BlockType.HEDGE.id);
            }
            for (int x = bx + 2; x < bx + w - 2; x += 2) {
                put(chunk, baseX, baseZ, x, y0 + 4, bz + d - 1, BlockType.FLOWER_RED.id);
            }
        }

        // Interior.
        int fx0 = bx + 1, fz0 = bz + 1, fx1 = bx + w - 2, fz1 = bz + d - 2;
        if (fx0 <= fx1 && fz0 <= fz1) {
            int cx = (fx0 + fx1) / 2, cz = (fz0 + fz1) / 2;
            put(chunk, baseX, baseZ, cx, y0, cz, BlockType.PLANKS.id);
            put(chunk, baseX, baseZ, cx, y0 + 1, cz, BlockType.PLANKS.id);
            put(chunk, baseX, baseZ, fx0, y0, fz0, BlockType.RED_PLASTER.id);
            put(chunk, baseX, baseZ, fx0, y0, fz0 + 1, BlockType.WHITE_PLASTER.id);
            put(chunk, baseX, baseZ, fx1, y0, fz1, BlockType.BOOKSHELF.id);
            put(chunk, baseX, baseZ, cx, Math.min(y0 + h - 1, y0 + 3), cz, BlockType.GLOWSTONE.id);
            put(chunk, baseX, baseZ, fx1, y0 + 1, cz, BlockType.FLOWER_YELLOW.id);
        }
    }

    private void yard(Chunk chunk, int baseX, int baseZ, int cx, int cz, int w, int d,
                      int target, int theme, Random r) {
        int x0 = cx - w / 2 - 3, x1 = cx + w / 2 + 3;
        int z0 = cz - d / 2 - 3, z1 = cz + d / 2 + 3;
        int fence = theme == THEME_NORDIC ? BlockType.PLANKS.id : BlockType.HEDGE.id;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = (x == x0 || x == x1) && z >= z0 && z <= z1
                        || (z == z0 || z == z1) && x >= x0 && x <= x1;
                if (!edge) {
                    if (r.nextFloat() < 0.12f) {
                        putSoft(chunk, baseX, baseZ, x, target + 1, z, BlockType.TALL_GRASS.id);
                    }
                    continue;
                }
                // Leave a gate in front of the door.
                if (z == z0 && Math.abs(x - cx) <= 1) continue;
                putSoft(chunk, baseX, baseZ, x, target + 1, z, fence);
            }
        }
        // Driveway + garden bed.
        for (int z = z0; z < z0 + 3; z++) {
            put(chunk, baseX, baseZ, cx - w / 2 + 1, target, z, BlockType.CONCRETE.id);
        }
        for (int i = 0; i < 4; i++) {
            int fx = x1 - 1 - r.nextInt(2);
            int fz = z1 - 1 - r.nextInt(2);
            put(chunk, baseX, baseZ, fx, target, fz, BlockType.DIRT.id);
            put(chunk, baseX, baseZ, fx, target + 1, fz,
                    (i % 2 == 0) ? BlockType.FLOWER_RED.id : BlockType.FLOWER_YELLOW.id);
        }
        streetTree(chunk, baseX, baseZ, x0 + 1, target + 1, z1 - 1, 3);
    }

    // ------------------------------------------------------------------
    // Landmarks
    // ------------------------------------------------------------------

    private void buildLandmark(Chunk chunk, int baseX, int baseZ, int cx, int cz, int target,
                               int theme, int wall, int wall2, int roof, int accent) {
        // Stone terrace shared by every landmark.
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 13) continue;
                put(chunk, baseX, baseZ, cx + dx, target, cz + dz, BlockType.STONE_BRICK.id);
                if (Math.abs(dx) + Math.abs(dz) == 13) {
                    put(chunk, baseX, baseZ, cx + dx, target + 1, cz + dz, BlockType.CONCRETE.id);
                }
            }
        }
        switch (theme) {
            case THEME_CHINESE:
                greatHall(chunk, baseX, baseZ, cx, target + 1, cz, wall, wall2, roof, accent);
                break;
            case THEME_EUROPEAN:
                cathedral(chunk, baseX, baseZ, cx, target + 1, cz, wall, wall2, roof, accent);
                break;
            case THEME_JAPANESE:
                pagoda(chunk, baseX, baseZ, cx, target + 1, cz, 5, wall, wall2, roof, accent);
                buildTorii(chunk, baseX, baseZ, cx, target + 1, cz + 11);
                break;
            case THEME_ARABIC:
                mosque(chunk, baseX, baseZ, cx, target + 1, cz, wall, wall2, roof, accent);
                break;
            case THEME_NORDIC:
                townHall(chunk, baseX, baseZ, cx, target + 1, cz, wall, wall2, roof, accent);
                break;
            default:
                spireTower(chunk, baseX, baseZ, cx, target + 1, cz, wall2, roof, accent);
                break;
        }
    }

    private void greatHall(Chunk chunk, int baseX, int baseZ, int cx, int y0, int cz,
                           int wall, int wall2, int roof, int accent) {
        int w = 13, d = 10, h = 6;
        int bx = cx - w / 2, bz = cz - d / 2;
        for (int floor = 0; floor < 2; floor++) {
            int fw = w - floor * 3;
            int fd = d - floor * 2;
            int fx = cx - fw / 2, fz = cz - fd / 2;
            int fy = y0 + floor * 5;
            for (int x = fx; x < fx + fw; x++) {
                for (int z = fz; z < fz + fd; z++) {
                    boolean perim = x == fx || x == fx + fw - 1 || z == fz || z == fz + fd - 1;
                    for (int y = fy; y < fy + 4; y++) {
                        if (!perim) {
                            put(chunk, baseX, baseZ, x, y, z, 0);
                            continue;
                        }
                        boolean pillar = (x - fx) % 3 == 0 && (z == fz || z == fz + fd - 1);
                        int id = pillar ? wall : (y == fy + 1 ? BlockType.GLASS.id : wall2);
                        put(chunk, baseX, baseZ, x, y, z, id);
                    }
                }
            }
            // Sweeping eaves.
            for (int x = fx - 2; x <= fx + fw + 1; x++) {
                for (int z = fz - 2; z <= fz + fd + 1; z++) {
                    boolean edge = x <= fx - 1 || x >= fx + fw || z <= fz - 1 || z >= fz + fd;
                    put(chunk, baseX, baseZ, x, fy + 4, z, edge ? roof : BlockType.PLANKS.id);
                    if (edge && ((x + z) & 1) == 0) put(chunk, baseX, baseZ, x, fy + 5, z, accent);
                }
            }
            for (int x = fx - 1; x <= fx + fw; x++) {
                for (int z = fz - 1; z <= fz + fd; z++) {
                    put(chunk, baseX, baseZ, x, fy + 5, z, roof);
                }
            }
            // Corner lanterns.
            put(chunk, baseX, baseZ, fx, fy + 2, fz, BlockType.LAMP.id);
            put(chunk, baseX, baseZ, fx + fw - 1, fy + 2, fz, BlockType.LAMP.id);
        }
        // Gold finial.
        put(chunk, baseX, baseZ, cx, y0 + 12, cz, accent);
        put(chunk, baseX, baseZ, cx, y0 + 13, cz, BlockType.GOLD_BLOCK.id);
        // Entrance steps.
        for (int x = cx - 2; x <= cx + 2; x++) {
            put(chunk, baseX, baseZ, x, y0, bz - 1, BlockType.STONE_BRICK.id);
            put(chunk, baseX, baseZ, x, y0 - 1, bz - 2, BlockType.STONE_BRICK.id);
        }
        put(chunk, baseX, baseZ, cx, y0 + 1, bz, 0);
        put(chunk, baseX, baseZ, cx + 1, y0 + 1, bz, 0);
        put(chunk, baseX, baseZ, cx, y0 + 2, bz, 0);
        put(chunk, baseX, baseZ, cx + 1, y0 + 2, bz, 0);
        buildPillars(chunk, baseX, baseZ, cx, y0, cz + 9, wall, accent);
    }

    private void buildPillars(Chunk chunk, int baseX, int baseZ, int cx, int y0, int cz,
                              int wall, int accent) {
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                for (int y = 1; y <= 3; y++) put(chunk, baseX, baseZ, cx + side * 4, y0 + y, cz + i * 2, wall);
                put(chunk, baseX, baseZ, cx + side * 4, y0 + 4, cz + i * 2, accent);
            }
        }
    }

    private void cathedral(Chunk chunk, int baseX, int baseZ, int cx, int y0, int cz,
                           int wall, int wall2, int roof, int accent) {
        int w = 9, d = 16, h = 9;
        int bx = cx - w / 2, bz = cz - d / 2;
        for (int x = bx; x < bx + w; x++) {
            for (int z = bz; z < bz + d; z++) {
                boolean perim = x == bx || x == bx + w - 1 || z == bz || z == bz + d - 1;
                for (int y = y0; y < y0 + h; y++) {
                    if (!perim) {
                        put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean buttress = (z - bz) % 4 == 0 && (x == bx || x == bx + w - 1);
                    boolean win = !buttress && (y - y0) > 2 && (y - y0) < h - 2 && ((x + z) % 4 == 0);
                    int id = buttress ? accent : (win ? BlockType.GLASS.id : wall);
                    put(chunk, baseX, baseZ, x, y, z, id);
                }
            }
        }
        // Nave roof.
        int ridgeZ0 = bz + 3, ridgeZ1 = bz + d - 4;
        for (int z = bz; z < bz + d; z++) {
            for (int k = 0; k <= w / 2; k++) {
                put(chunk, baseX, baseZ, bx + k, y0 + h + k, z, roof);
                put(chunk, baseX, baseZ, bx + w - 1 - k, y0 + h + k, z, roof);
            }
        }
        for (int z = ridgeZ0; z <= ridgeZ1; z++) put(chunk, baseX, baseZ, bx + w / 2, y0 + h + w / 2, z, accent);
        // Rose window.
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (dx * dx + dy * dy > 5) continue;
                put(chunk, baseX, baseZ, cx + dx, y0 + 5 + dy, bz, BlockType.GLASS.id);
            }
        }
        // Twin towers with spires.
        for (int side = -1; side <= 1; side += 2) {
            int tx = cx + side * (w / 2 + 2);
            int tz = bz + 1;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = y0; y < y0 + 17; y++) {
                        boolean perim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                        if (!perim) continue;
                        boolean win = (y - y0) % 4 == 0 && y > y0 + 3;
                        put(chunk, baseX, baseZ, tx + dx, y, tz + dz,
                                win ? BlockType.GLASS.id : wall2);
                    }
                }
            }
            for (int y = y0 + 17; y <= y0 + 21; y++) {
                int r = Math.max(0, 3 - (y - y0 - 17));
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) put(chunk, baseX, baseZ, tx + dx, y, tz + dz, roof);
                }
            }
            put(chunk, baseX, baseZ, tx, y0 + 22, tz, accent);
            put(chunk, baseX, baseZ, tx, y0 + 23, tz, BlockType.GOLD_BLOCK.id);
        }
        // Porch and doors.
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int y = y0; y < y0 + 4; y++) put(chunk, baseX, baseZ, x, y, bz, wall2);
        }
        for (int y = y0; y < y0 + 3; y++) {
            put(chunk, baseX, baseZ, cx - 1, y, bz, 0);
            put(chunk, baseX, baseZ, cx, y, bz, 0);
            put(chunk, baseX, baseZ, cx + 1, y, bz, 0);
        }
    }

    private void pagoda(Chunk chunk, int baseX, int baseZ, int cx, int y0, int cz, int tiers,
                        int wall, int wall2, int roof, int accent) {
        int w = 9, d = 9, y = y0;
        for (int t = 0; t < tiers; t++) {
            int hw = w / 2, hd = d / 2;
            for (int x = cx - hw; x <= cx + hw; x++) {
                for (int z = cz - hd; z <= cz + hd; z++) {
                    boolean perim = Math.abs(x - cx) == hw || Math.abs(z - cz) == hd;
                    if (perim) {
                        boolean corner = Math.abs(x - cx) == hw && Math.abs(z - cz) == hd;
                        put(chunk, baseX, baseZ, x, y, z, corner ? accent : wall);
                        put(chunk, baseX, baseZ, x, y + 1, z,
                                corner ? accent : ((x + z) % 3 == 0 ? BlockType.GLASS.id : wall2));
                    } else {
                        put(chunk, baseX, baseZ, x, y, z, 0);
                        put(chunk, baseX, baseZ, x, y + 1, z, 0);
                    }
                }
            }
            // Eaves.
            for (int x = cx - hw - 2; x <= cx + hw + 2; x++) {
                for (int z = cz - hd - 2; z <= cz + hd + 2; z++) {
                    boolean edge = Math.abs(x - cx) >= hw || Math.abs(z - cz) >= hd;
                    if (!edge) continue;
                    put(chunk, baseX, baseZ, x, y + 3, z, roof);
                    if ((Math.abs(x - cx) == hw + 2 && Math.abs(z - cz) == hd + 2)) {
                        put(chunk, baseX, baseZ, x, y + 4, z, accent);
                    }
                }
            }
            y += 4;
            w -= 2;
            d -= 2;
        }
        put(chunk, baseX, baseZ, cx, y, cz, BlockType.LAMP.id);
        put(chunk, baseX, baseZ, cx, y + 1, cz, BlockType.GOLD_BLOCK.id);
        // Stone lanterns flanking the stairs.
        for (int side = -1; side <= 1; side += 2) {
            put(chunk, baseX, baseZ, cx + side * 5, y0, cz + 6, BlockType.STONE_BRICK.id);
            put(chunk, baseX, baseZ, cx + side * 5, y0 + 1, cz + 6, BlockType.STONE_BRICK.id);
            put(chunk, baseX, baseZ, cx + side * 5, y0 + 2, cz + 6, BlockType.LAMP.id);
        }
    }

    private void buildTorii(Chunk chunk, int baseX, int baseZ, int cx, int y, int cz) {
        for (int side = -1; side <= 1; side += 2) {
            for (int dy = 0; dy < 4; dy++) {
                put(chunk, baseX, baseZ, cx + side * 3, y + dy, cz, BlockType.RED_PLASTER.id);
            }
            put(chunk, baseX, baseZ, cx + side * 3, y, cz - 1, BlockType.STONE_BRICK.id);
        }
        for (int dx = -4; dx <= 4; dx++) {
            put(chunk, baseX, baseZ, cx + dx, y + 4, cz, BlockType.RED_PLASTER.id);
            put(chunk, baseX, baseZ, cx + dx, y + 5, cz, BlockType.DARK_PLASTER.id);
        }
        for (int dx = -5; dx <= 5; dx++) put(chunk, baseX, baseZ, cx + dx, y + 6, cz, BlockType.DARK_PLASTER.id);
    }

    private void mosque(Chunk chunk, int baseX, int baseZ, int cx, int y0, int cz,
                        int wall, int wall2, int roof, int accent) {
        int w = 13, d = 11, h = 7;
        int bx = cx - w / 2, bz = cz - d / 2;
        for (int x = bx; x < bx + w; x++) {
            for (int z = bz; z < bz + d; z++) {
                boolean perim = x == bx || x == bx + w - 1 || z == bz || z == bz + d - 1;
                for (int y = y0; y < y0 + h; y++) {
                    if (!perim) {
                        put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean arch = (y - y0) < 3 && ((x + z) % 4 == 0);
                    int id = arch ? BlockType.DARK_GLASS.id : wall;
                    put(chunk, baseX, baseZ, x, y, z, id);
                }
                put(chunk, baseX, baseZ, x, y0 + h, z, wall2);
            }
        }
        // Dome.
        int dr = 5;
        for (int dy = 0; dy <= dr; dy++) {
            int r = (int) Math.round(Math.sqrt(Math.max(0, dr * dr - dy * dy)));
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + 1) continue;
                    put(chunk, baseX, baseZ, cx + dx, y0 + h + 1 + dy, cz + dz, roof);
                }
            }
        }
        put(chunk, baseX, baseZ, cx, y0 + h + dr + 2, cz, accent);
        // Minarets.
        for (int side = -1; side <= 1; side += 2) {
            int tx = cx + side * (w / 2 + 2), tz = cz - d / 2 + 1;
            for (int y = y0; y < y0 + 18; y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (Math.abs(dx) == 1 && Math.abs(dz) == 1) continue;
                        boolean band = (y - y0) % 5 == 0;
                        put(chunk, baseX, baseZ, tx + dx, y, tz + dz,
                                band ? accent : wall2);
                    }
                }
            }
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                    put(chunk, baseX, baseZ, tx + dx, y0 + 18, tz + dz, roof);
                }
            }
            put(chunk, baseX, baseZ, tx, y0 + 19, tz, BlockType.GOLD_BLOCK.id);
        }
        // Arcade courtyard.
        for (int x = bx - 2; x <= bx + w + 1; x++) {
            for (int z = bz + d; z <= bz + d + 5; z++) {
                boolean perim = x == bx - 2 || x == bx + w + 1 || z == bz + d + 5;
                if (perim) {
                    for (int y = y0; y <= y0 + 3; y++) {
                        put(chunk, baseX, baseZ, x, y, z, (y == y0 + 3) ? roof : wall);
                    }
                } else {
                    put(chunk, baseX, baseZ, x, y0, z, BlockType.STONE_BRICK.id);
                }
            }
        }
    }

    private void townHall(Chunk chunk, int baseX, int baseZ, int cx, int y0, int cz,
                          int wall, int wall2, int roof, int accent) {
        int w = 11, d = 9, h = 6;
        int bx = cx - w / 2, bz = cz - d / 2;
        for (int x = bx; x < bx + w; x++) {
            for (int z = bz; z < bz + d; z++) {
                boolean perim = x == bx || x == bx + w - 1 || z == bz || z == bz + d - 1;
                for (int y = y0; y < y0 + h; y++) {
                    if (!perim) {
                        put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean win = (y - y0) % 3 == 1 && (x + z) % 3 == 0;
                    put(chunk, baseX, baseZ, x, y, z, win ? BlockType.GLASS.id : wall);
                }
            }
        }
        // Steep gable roof.
        for (int x = bx - 1; x <= bx + w; x++) {
            int rise = Math.max(0, (w + 2) / 2 - Math.abs(x - cx));
            for (int z = bz - 1; z <= bz + d; z++) {
                for (int y = y0 + h; y <= y0 + h + rise; y++) put(chunk, baseX, baseZ, x, y, z, roof);
            }
        }
        // Clock tower.
        int tx = cx, tz = cz;
        for (int y = y0 + h; y < y0 + h + 12; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
                    put(chunk, baseX, baseZ, tx + dx, y, tz + dz, wall2);
                }
            }
        }
        put(chunk, baseX, baseZ, tx, y0 + h + 8, tz + 2, BlockType.LAMP.id);
        put(chunk, baseX, baseZ, tx, y0 + h + 8, tz - 2, BlockType.LAMP.id);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                put(chunk, baseX, baseZ, tx + dx, y0 + h + 12, tz + dz, roof);
            }
        }
        put(chunk, baseX, baseZ, tx, y0 + h + 13, tz, accent);
        put(chunk, baseX, baseZ, tx, y0 + h + 14, tz, BlockType.GOLD_BLOCK.id);
        // Colourful gabled houses around the square.
        for (int i = 0; i < 4; i++) {
            int hx = cx + (i < 2 ? -7 : 7);
            int hz = cz + (i % 2 == 0 ? -7 : 7);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = y0; y < y0 + 4; y++) {
                        boolean perim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                        put(chunk, baseX, baseZ, hx + dx, y, hz + dz,
                                perim ? (y == y0 ? BlockType.STONE_BRICK.id : wall2) : 0);
                    }
                    put(chunk, baseX, baseZ, hx + dx, y0 + 4, hz + dz, roof);
                }
            }
        }
    }

    private void spireTower(Chunk chunk, int baseX, int baseZ, int cx, int y0, int cz,
                            int wall, int roof, int accent) {
        int w = 9, d = 9;
        int h = Math.min(30, Chunk.SIZE_Y - 16 - y0);
        if (h < 12) h = 12;
        for (int tier = 0; tier < 3; tier++) {
            int seg = 10;
            for (int x = cx - w / 2; x <= cx + w / 2; x++) {
                for (int z = cz - d / 2; z <= cz + d / 2; z++) {
                    boolean perim = Math.abs(x - cx) == w / 2 || Math.abs(z - cz) == d / 2;
                    for (int y = y0 + tier * seg; y < y0 + (tier + 1) * seg; y++) {
                        if (!perim) {
                            put(chunk, baseX, baseZ, x, y, z, 0);
                        } else {
                            put(chunk, baseX, baseZ, x, y, z,
                                    (y % 6 == 0) ? accent : BlockType.DARK_GLASS.id);
                        }
                    }
                }
            }
            w -= 2;
            d -= 2;
        }
        for (int y = y0 + h; y < y0 + h + 8; y++) put(chunk, baseX, baseZ, cx, y, cz, wall);
        put(chunk, baseX, baseZ, cx, y0 + h + 8, cz, BlockType.LAMP.id);
        // Observation deck.
        for (int x = cx - 5; x <= cx + 5; x++) {
            for (int z = cz - 5; z <= cz + 5; z++) {
                if (Math.abs(x - cx) == 5 || Math.abs(z - cz) == 5) {
                    put(chunk, baseX, baseZ, x, y0 + h - 2, z, roof);
                }
            }
        }
    }

    private void buildWall(Chunk chunk, int baseX, int baseZ, int cityX, int cityZ,
                           int target, int theme, int wall, int accent) {
        int r = RADIUS + 1;
        int reach = r + 3;
        int x0 = Math.max(baseX, cityX - reach), x1 = Math.min(baseX + Chunk.SIZE_X - 1, cityX + reach);
        int z0 = Math.max(baseZ, cityZ - reach), z1 = Math.min(baseZ + Chunk.SIZE_Z - 1, cityZ + reach);
        for (int wx = x0; wx <= x1; wx++) {
            for (int wz = z0; wz <= z1; wz++) {
                double dist = Math.hypot(wx - cityX, wz - cityZ);
                if (dist < r - 1 || dist > r + 1) continue;
                int relX = wx - cityX, relZ = wz - cityZ;
                // Gates at the four avenues.
                if (Math.abs(relX) <= 3 || Math.abs(relZ) <= 3) continue;
                for (int y = 1; y <= 5; y++) {
                    int id = (y == 5) ? accent : wall;
                    put(chunk, baseX, baseZ, wx, target + y, wz, id);
                }
                if (Math.floorMod((int) (Math.atan2(relZ, relX) * 8), 2) == 0) {
                    put(chunk, baseX, baseZ, wx, target + 6, wz, accent);
                }
            }
        }
        // Corner towers.
        for (int a = 0; a < 4; a++) {
            double ang = Math.PI / 4 + a * Math.PI / 2;
            int tx = cityX + (int) Math.round(Math.cos(ang) * r);
            int tz = cityZ + (int) Math.round(Math.sin(ang) * r);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int y = 1; y <= 8; y++) {
                        boolean perim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                        if (!perim && y < 7) continue;
                        put(chunk, baseX, baseZ, tx + dx, target + y, tz + dz,
                                y == 8 ? accent : wall);
                    }
                }
            }
        }
    }

    private void suburbs(Chunk chunk, int baseX, int baseZ, int cityX, int cityZ,
                         int target, int theme, int[] pal) {
        int outer = RADIUS + SUBURB;
        int x0 = Math.max(baseX, cityX - outer), x1 = Math.min(baseX + Chunk.SIZE_X - 1, cityX + outer);
        int z0 = Math.max(baseZ, cityZ - outer), z1 = Math.min(baseZ + Chunk.SIZE_Z - 1, cityZ + outer);
        if (x0 > x1 || z0 > z1) return;
        for (int wx = x0; wx <= x1; wx += 4) {
            for (int wz = z0; wz <= z1; wz += 4) {
                double dist = Math.hypot(wx - cityX, wz - cityZ);
                if (dist < RADIUS + GRADE + 6 || dist > outer) continue;
                Random r = new Random(wx * 9176L + wz * 3881L + 71L);
                if (r.nextInt(4) != 0) continue;
                int h = terrain.surfaceHeight(wx, wz);
                if (h <= TerrainGenerator.SEA_LEVEL) continue;
                if (r.nextBoolean()) {
                    field(chunk, baseX, baseZ, wx, h, wz, r);
                } else {
                    cottage(chunk, baseX, baseZ, wx, h + 1, wz, theme, pal[0], pal[2], r);
                }
            }
        }
    }

    private void field(Chunk chunk, int baseX, int baseZ, int x, int y, int z, Random r) {
        int w = 5 + r.nextInt(4), d = 5 + r.nextInt(4);
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < d; dz++) {
                put(chunk, baseX, baseZ, x + dx, y, z + dz, BlockType.DIRT.id);
                if ((dx % 2 == 0) && (dz % 2 == 0)) {
                    putSoft(chunk, baseX, baseZ, x + dx, y + 1, z + dz,
                            r.nextBoolean() ? BlockType.TALL_GRASS.id : BlockType.HEDGE.id);
                }
            }
        }
        for (int dx = -1; dx <= w; dx++) {
            putSoft(chunk, baseX, baseZ, x + dx, y + 1, z - 1, BlockType.HEDGE.id);
        }
    }

    private void cottage(Chunk chunk, int baseX, int baseZ, int x, int y, int z,
                         int theme, int wall, int roof, Random r) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy < 3; dy++) {
                    boolean perim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    put(chunk, baseX, baseZ, x + dx, y + dy, z + dz,
                            perim ? (dy == 0 ? BlockType.STONE_BRICK.id : wall) : 0);
                }
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            int rise = Math.max(0, 3 - Math.abs(dx));
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 0; dy <= rise; dy++) put(chunk, baseX, baseZ, x + dx, y + 3 + dy, z + dz, roof);
            }
        }
        put(chunk, baseX, baseZ, x, y, z - 2, 0);
        put(chunk, baseX, baseZ, x, y + 1, z - 2, 0);
        put(chunk, baseX, baseZ, x + 1, y + 4, z + 1, BlockType.BRICK.id);
        put(chunk, baseX, baseZ, x + 1, y + 5, z + 1, BlockType.BRICK.id);
        // Fence.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean edge = Math.abs(dx) == 3 && Math.abs(dz) <= 3 || Math.abs(dz) == 3 && Math.abs(dx) <= 3;
                if (edge && !(dz == -3 && Math.abs(dx) <= 1)) {
                    putSoft(chunk, baseX, baseZ, x + dx, y + 1, z + dz, BlockType.HEDGE.id);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Elevated metro station
    // ------------------------------------------------------------------

    private void buildStation(Chunk chunk, int baseX, int baseZ, int wx0, int wz0, int target) {
        int ty = World.TRACK_Y;
        int hx = 11, hz = 7;
        int roofY = ty + 9;
        int frame = BlockType.STONE_BRICK.id;
        int wall = BlockType.WHITE_PLASTER.id;
        int roof = BlockType.ROOF_SLATE.id;
        int glass = BlockType.GLASS.id;
        int glow = BlockType.LAMP.id;
        int accent = BlockType.RED_PLASTER.id;

        // Platform slab.
        for (int dx = -hx; dx <= hx; dx++)
            for (int dz = -hz; dz <= hz; dz++)
                put(chunk, baseX, baseZ, wx0 + dx, ty - 1, wz0 + dz, frame);

        // Supports.
        for (int dx = -hx + 1; dx <= hx - 1; dx += 5)
            for (int dz = -hz + 1; dz <= hz - 1; dz += 5)
                for (int y = target; y < ty - 1; y++)
                    put(chunk, baseX, baseZ, wx0 + dx, y, wz0 + dz, frame);

        // Concourse volume.
        for (int dx = -hx; dx <= hx; dx++)
            for (int dz = -hz; dz <= hz; dz++)
                for (int y = ty; y <= roofY; y++)
                    put(chunk, baseX, baseZ, wx0 + dx, y, wz0 + dz, 0);

        // Pillars clear of the tracks.
        int[] pxs = {-8, -3, 3, 8};
        int[] pzs = {-4, 4};
        for (int px : pxs) {
            for (int pz : pzs) {
                for (int ax = -1; ax <= 1; ax++)
                    for (int az = -1; az <= 1; az++)
                        for (int y = ty; y < roofY; y++)
                            put(chunk, baseX, baseZ, wx0 + px + ax, y, wz0 + pz + az,
                                    (ax == 0 && az == 0) ? frame : wall);
            }
        }

        // Glass curtain walls, open at the tracks and the stair landing.
        for (int dx = -hx; dx <= hx; dx++) {
            if (Math.abs(dx) <= 2) continue;
            boolean stairDoor = dx >= STAIR_X - 1 && dx <= STAIR_X + 1;
            for (int y = ty + 1; y <= ty + 7; y++) {
                put(chunk, baseX, baseZ, wx0 + dx, y, wz0 - hz, glass);
                if (!stairDoor) put(chunk, baseX, baseZ, wx0 + dx, y, wz0 + hz, glass);
            }
        }
        for (int dz = -hz; dz <= hz; dz++) {
            if (Math.abs(dz) <= 2) continue;
            for (int y = ty + 1; y <= ty + 7; y++) {
                put(chunk, baseX, baseZ, wx0 - hx, y, wz0 + dz, glass);
                put(chunk, baseX, baseZ, wx0 + hx, y, wz0 + dz, glass);
            }
        }

        // Roof with a cross skylight and a clock tower.
        for (int dx = -hx - 1; dx <= hx + 1; dx++) {
            for (int dz = -hz - 1; dz <= hz + 1; dz++) {
                boolean skylight = (Math.abs(dz) <= 1 && Math.abs(dx) <= hx - 2)
                        || (Math.abs(dx) <= 1 && Math.abs(dz) <= hz - 2);
                put(chunk, baseX, baseZ, wx0 + dx, roofY, wz0 + dz, skylight ? glass : roof);
            }
        }
        for (int dx = -hx; dx <= hx; dx += 4) {
            put(chunk, baseX, baseZ, wx0 + dx, roofY - 1, wz0 - hz + 1, glow);
            put(chunk, baseX, baseZ, wx0 + dx, roofY - 1, wz0 + hz - 1, glow);
        }

        // Corner towers + beacons.
        for (int tx : new int[]{-hx, hx}) {
            for (int tz : new int[]{-hz, hz}) {
                for (int ax = -1; ax <= 1; ax++)
                    for (int az = -1; az <= 1; az++)
                        for (int y = ty; y <= roofY + 5; y++)
                            put(chunk, baseX, baseZ, wx0 + tx + ax, y, wz0 + tz + az,
                                    (ax != 0 || az != 0) ? glass : wall);
                put(chunk, baseX, baseZ, wx0 + tx, roofY + 6, wz0 + tz, glow);
            }
        }

        // Sign above the west entrance.
        for (int y = roofY + 1; y <= roofY + 5; y++)
            put(chunk, baseX, baseZ, wx0 - hx, y, wz0, accent);
        put(chunk, baseX, baseZ, wx0 - hx, roofY + 6, wz0, glow);

        // Benches and platform edge lines.
        for (int dx = -hx + 3; dx <= hx - 3; dx += 6) {
            for (int side = -1; side <= 1; side += 2) {
                int z = wz0 + side * (hz - 2);
                put(chunk, baseX, baseZ, wx0 + dx, ty, z, BlockType.PLANKS.id);
                put(chunk, baseX, baseZ, wx0 + dx + 1, ty, z, BlockType.PLANKS.id);
                put(chunk, baseX, baseZ, wx0 + dx, ty + 1, z, BlockType.PLANKS.id);
            }
        }
        for (int dx = -hx; dx <= hx; dx++) {
            put(chunk, baseX, baseZ, wx0 + dx, ty, wz0 - 3, BlockType.YELLOW_PLASTER.id);
            put(chunk, baseX, baseZ, wx0 + dx, ty, wz0 + 3, BlockType.YELLOW_PLASTER.id);
        }

        // Grand staircase from the plaza up to the platform (south side).
        int steps = ty - target;
        if (steps > 0) {
            for (int i = 1; i <= steps; i++) {
                int z = wz0 + hz + (steps - i + 1);
                int topY = target + i - 1;
                for (int ax = -1; ax <= 1; ax++) {
                    for (int y = target - 1; y <= topY; y++) {
                        put(chunk, baseX, baseZ, wx0 + STAIR_X + ax, y, z,
                                (y == topY) ? frame : wall);
                    }
                }
                put(chunk, baseX, baseZ, wx0 + STAIR_X - 2, topY + 1, z, accent);
                put(chunk, baseX, baseZ, wx0 + STAIR_X + 2, topY + 1, z, accent);
            }
        }
    }

    // ------------------------------------------------------------------
    // Rails
    // ------------------------------------------------------------------

    public void rails(Chunk chunk, int baseX, int baseZ) {
        for (int[] s : SEGMENTS) {
            if (s[0] == 0) {
                int z = s[1];
                if (z < baseZ || z > baseZ + Chunk.SIZE_Z - 1) continue;
                int lz = z - baseZ;
                int xMin = Math.max(s[2], baseX);
                int xMax = Math.min(s[3], baseX + Chunk.SIZE_X - 1);
                for (int x = xMin; x <= xMax; x++) trackColumn(chunk, x - baseX, lz, x, true);
            } else {
                int x = s[1];
                if (x < baseX || x > baseX + Chunk.SIZE_X - 1) continue;
                int lx = x - baseX;
                int zMin = Math.max(s[2], baseZ);
                int zMax = Math.min(s[3], baseZ + Chunk.SIZE_Z - 1);
                for (int z = zMin; z <= zMax; z++) trackColumn(chunk, lx, z - baseZ, z, false);
            }
        }
    }

    private void trackColumn(Chunk chunk, int lx, int lz, int alongCoord, boolean alongX) {
        int ty = World.TRACK_Y;
        for (int y = ty; y <= ty + 6; y++) chunk.set(lx, y, lz, (byte) 0);
        chunk.set(lx, ty - 1, lz, (byte) BlockType.STONE_BRICK.id);
        chunk.set(lx, ty, lz, (byte) (alongX ? BlockType.RAIL_X.id : BlockType.RAIL_Z.id));

        int surf = 1;
        for (int y = ty - 2; y >= 1; y--) {
            if (BlockType.isSolid(chunk.get(lx, y, lz))) {
                surf = y;
                break;
            }
        }
        if (Math.floorMod(alongCoord, 8) == 0) {
            for (int y = surf + 1; y <= ty - 2; y++) {
                chunk.set(lx, y, lz, (byte) BlockType.STEEL.id);
            }
            chunk.set(lx, ty - 2, lz, (byte) BlockType.STONE_BRICK.id);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static int[] palette(int theme) {
        switch (theme) {
            case THEME_CHINESE:
                return new int[]{BlockType.RED_PLASTER.id, BlockType.WHITE_PLASTER.id,
                        BlockType.ROOF_SLATE.id, BlockType.GOLD_BLOCK.id,
                        BlockType.GRASS.id, BlockType.GLASS.id};
            case THEME_EUROPEAN:
                return new int[]{BlockType.BRICK.id, BlockType.WHITE_PLASTER.id,
                        BlockType.ROOF_TILE.id, BlockType.STONE_BRICK.id,
                        BlockType.GRASS.id, BlockType.GLASS.id};
            case THEME_JAPANESE:
                return new int[]{BlockType.BIRCH_PLANKS.id, BlockType.WHITE_PLASTER.id,
                        BlockType.ROOF_SLATE.id, BlockType.DARK_PLASTER.id,
                        BlockType.GRASS.id, BlockType.GLASS.id};
            case THEME_ARABIC:
                return new int[]{BlockType.SANDSTONE.id, BlockType.YELLOW_PLASTER.id,
                        BlockType.TERRACOTTA.id, BlockType.WHITE_PLASTER.id,
                        BlockType.SAND.id, BlockType.GLASS.id};
            case THEME_MODERN:
                return new int[]{BlockType.CONCRETE.id, BlockType.WHITE_PLASTER.id,
                        BlockType.STEEL.id, BlockType.DARK_GLASS.id,
                        BlockType.GRASS.id, BlockType.GLASS.id};
            default:
                return new int[]{BlockType.RED_PLASTER.id, BlockType.YELLOW_PLASTER.id,
                        BlockType.ROOF_TILE.id, BlockType.WHITE_PLASTER.id,
                        BlockType.GRASS.id, BlockType.GLASS.id};
        }
    }

    private boolean overlaps(int baseX, int baseZ, int x0, int z0, int x1, int z1) {
        return x1 >= baseX && x0 <= baseX + Chunk.SIZE_X - 1
                && z1 >= baseZ && z0 <= baseZ + Chunk.SIZE_Z - 1;
    }

    private void put(Chunk chunk, int baseX, int baseZ, int wx, int wy, int wz, int id) {
        int lx = wx - baseX, lz = wz - baseZ;
        if (lx < 0 || lx >= Chunk.SIZE_X || lz < 0 || lz >= Chunk.SIZE_Z) return;
        if (wy < 0 || wy >= Chunk.SIZE_Y) return;
        chunk.set(lx, wy, lz, (byte) id);
    }

    /** Writes only into air, used for foliage so buildings are never overwritten. */
    private void putSoft(Chunk chunk, int baseX, int baseZ, int wx, int wy, int wz, int id) {
        int lx = wx - baseX, lz = wz - baseZ;
        if (lx < 0 || lx >= Chunk.SIZE_X || lz < 0 || lz >= Chunk.SIZE_Z) return;
        if (wy < 0 || wy >= Chunk.SIZE_Y) return;
        if (chunk.get(lx, wy, lz) != 0) return;
        chunk.set(lx, wy, lz, (byte) id);
    }
}
