package com.wangzi5151.myminiword.world;

import java.util.ArrayList;
import java.util.Random;

public class CityGenerator {
    private static final int RADIUS = 48;

    public static final int THEME_CHINESE = 0;
    public static final int THEME_EUROPEAN = 1;
    public static final int THEME_JAPANESE = 2;
    public static final int THEME_ARABIC = 3;
    public static final int THEME_MODERN = 4;
    public static final int THEME_NORDIC = 5;

    // {x, z, theme}
    public static final int[][] CITIES = {
            {0, 0, THEME_CHINESE},
            {520, 80, THEME_EUROPEAN},
            {180, -520, THEME_JAPANESE},
            {-560, -160, THEME_ARABIC},
            {860, 480, THEME_MODERN},
            {-240, 760, THEME_NORDIC}
    };

    // rail segments: {axis(0=alongX/z-fixed, 1=alongZ/x-fixed), fixed, min, max}
    public static final int[][] SEGMENTS = buildSegments();

    private static int[][] buildSegments() {
        ArrayList<int[]> list = new ArrayList<>();
        for (int c = 0; c < CITIES.length - 1; c++) {
            int x0 = CITIES[c][0], z0 = CITIES[c][1];
            int x1 = CITIES[c + 1][0], z1 = CITIES[c + 1][1];
            list.add(new int[]{0, z0, Math.min(x0, x1), Math.max(x0, x1)});
            list.add(new int[]{1, x1, Math.min(z0, z1), Math.max(z0, z1)});
        }
        return list.toArray(new int[0][]);
    }

    private final TerrainGenerator terrain;

    public CityGenerator(TerrainGenerator terrain) {
        this.terrain = terrain;
    }

    public void populate(Chunk chunk, int baseX, int baseZ) {
        for (int[] city : CITIES) {
            int cx = city[0], cz = city[1];
            if (cx + RADIUS < baseX || cx - RADIUS > baseX + Chunk.SIZE_X - 1) continue;
            if (cz + RADIUS < baseZ || cz - RADIUS > baseZ + Chunk.SIZE_Z - 1) continue;
            buildCity(chunk, baseX, baseZ, cx, cz, city[2]);
        }
    }

    private void buildCity(Chunk chunk, int baseX, int baseZ, int wx0, int wz0, int theme) {
        int cx0 = Math.max(wx0 - RADIUS, baseX);
        int cx1 = Math.min(wx0 + RADIUS, baseX + Chunk.SIZE_X - 1);
        int cz0 = Math.max(wz0 - RADIUS, baseZ);
        int cz1 = Math.min(wz0 + RADIUS, baseZ + Chunk.SIZE_Z - 1);
        if (cx0 > cx1 || cz0 > cz1) return;

        int target = terrain.surfaceHeight(wx0, wz0);
        if (target < TerrainGenerator.SEA_LEVEL + 2) target = TerrainGenerator.SEA_LEVEL + 2;
        if (target > 38) target = 38;

        int[] pal = palette(theme);
        int wall = pal[0], roof = pal[1], accent = pal[2], ground = pal[3], glass = BlockType.GLASS.id;
        int maxH = pal[4];

        int r2 = RADIUS * RADIUS;
        for (int wx = cx0; wx <= cx1; wx++) {
            for (int wz = cz0; wz <= cz1; wz++) {
                int dx = wx - wx0, dz = wz - wz0;
                if (dx * dx + dz * dz > r2) continue;
                int lx = wx - baseX, lz = wz - baseZ;
                for (int y = 0; y < target - 1; y++) {
                    int id = chunk.get(lx, y, lz);
                    if (id == 0 || id == BlockType.WATER.id) chunk.set(lx, y, lz, (byte) BlockType.STONE.id);
                }
                boolean plaza = dx * dx + dz * dz <= 16;
                boolean street = Math.abs(dx) <= 2 || Math.abs(dz) <= 2;
                int g;
                if (plaza) g = accent;
                else if (street) g = ground;
                else g = (theme == THEME_ARABIC) ? BlockType.SAND.id : BlockType.GRASS.id;
                chunk.set(lx, target - 1, lz, (byte) g);
                for (int y = target; y < World.TRACK_Y - 1; y++) chunk.set(lx, y, lz, (byte) 0);

                // street lamps
                if (street && !plaza && Math.floorMod(wx, 12) == 0 && Math.floorMod(wz, 12) == 0) {
                    put(chunk, baseX, baseZ, wx, target, wz, accent);
                    put(chunk, baseX, baseZ, wx, target + 1, wz, accent);
                    put(chunk, baseX, baseZ, wx, target + 2, wz, BlockType.GLOWSTONE.id);
                }
            }
        }

        // plaza fountain
        buildFountain(chunk, baseX, baseZ, wx0, wz0, target, accent);

        Random r = new Random((long) wx0 * 918273645L + (long) wz0 * 192837465L + 7L);

        buildLandmark(chunk, baseX, baseZ, wx0 + 4, target, wz0 + 4, theme, wall, roof, accent, glass);

        int count = 18 + r.nextInt(10);
        for (int b = 0; b < count; b++) {
            int w = 5 + r.nextInt(6);
            int d = 5 + r.nextInt(6);
            int h;
            if (theme == THEME_MODERN) h = 10 + r.nextInt(12);
            else h = 4 + r.nextInt(Math.max(2, maxH - 3));
            int bx = wx0 - 42 + r.nextInt(84 - w);
            int bz = wz0 - 42 + r.nextInt(84 - d);
            if ((bx - wx0) * (bx - wx0) + (bz - wz0) * (bz - wz0) < 169) continue;

            int roll = r.nextInt(10);
            if (theme == THEME_MODERN || roll < 3) {
                // Sleek stepped glass tower in every city, for a modern skyline.
                int tw = 5 + r.nextInt(4);
                int td = 5 + r.nextInt(4);
                int th = (theme == THEME_MODERN ? h : h + 4) + r.nextInt(6);
                buildTower(chunk, baseX, baseZ, bx, target, bz, tw, th, td, wall, roof, accent, glass);
            } else if ((theme == THEME_CHINESE || theme == THEME_JAPANESE) && roll < 6) {
                buildPagoda(chunk, baseX, baseZ, bx, target, bz, w, h, d, wall, roof, accent, glass);
            } else if (theme == THEME_ARABIC && roll < 7) {
                buildDome(chunk, baseX, baseZ, bx, target, bz, w, Math.max(3, h - 1), d, wall, roof, accent, glass);
            } else {
                buildHouse(chunk, baseX, baseZ, bx, target, bz, w, h, d, wall, roof, accent, glass);
            }
        }

        buildStation(chunk, baseX, baseZ, wx0, wz0, target);
    }

    private static int[] palette(int theme) {
        switch (theme) {
            case THEME_CHINESE:
                return new int[]{BlockType.RED_PLASTER.id, BlockType.DARK_PLASTER.id, BlockType.GOLD_BLOCK.id, BlockType.STONE_BRICK.id, 8};
            case THEME_EUROPEAN:
                return new int[]{BlockType.STONE_BRICK.id, BlockType.BRICK.id, BlockType.PLANKS.id, BlockType.COBBLE.id, 10};
            case THEME_JAPANESE:
                return new int[]{BlockType.BIRCH_PLANKS.id, BlockType.DARK_PLASTER.id, BlockType.BIRCH_LOG.id, BlockType.STONE_BRICK.id, 6};
            case THEME_ARABIC:
                return new int[]{BlockType.SANDSTONE.id, BlockType.YELLOW_PLASTER.id, BlockType.WHITE_PLASTER.id, BlockType.SAND.id, 9};
            case THEME_MODERN:
                return new int[]{BlockType.WHITE_PLASTER.id, BlockType.DARK_PLASTER.id, BlockType.STONE_BRICK.id, BlockType.STONE_BRICK.id, 20};
            default: // NORDIC
                return new int[]{BlockType.BIRCH_PLANKS.id, BlockType.BRICK.id, BlockType.RED_PLASTER.id, BlockType.COBBLE.id, 8};
        }
    }

    private void plantTree(Chunk chunk, int baseX, int baseZ, int wx, int y, int wz) {
        // unused; kept for future park placement
    }

    private void buildFountain(Chunk chunk, int baseX, int baseZ, int wx0, int wz0, int target, int accent) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    put(chunk, baseX, baseZ, wx0, target, wz0, BlockType.WATER.id);
                    put(chunk, baseX, baseZ, wx0, target + 1, wz0, BlockType.GLOWSTONE.id);
                } else {
                    put(chunk, baseX, baseZ, wx0 + dx, target, wz0 + dz, accent);
                }
            }
        }
    }

    private void buildLandmark(Chunk chunk, int baseX, int baseZ, int bx, int y0, int bz, int theme, int wall, int roof, int accent, int glass) {
        switch (theme) {
            case THEME_CHINESE:
            case THEME_JAPANESE:
                buildPagoda(chunk, baseX, baseZ, bx, y0, bz, 9, 9, 9, wall, roof, accent, glass);
                break;
            case THEME_ARABIC:
                buildDome(chunk, baseX, baseZ, bx, y0, bz, 11, 9, 11, wall, roof, accent, glass);
                break;
            case THEME_MODERN:
                buildTower(chunk, baseX, baseZ, bx, y0, bz, 7, 28, 7, wall, roof, accent, glass);
                break;
            default:
                buildTower(chunk, baseX, baseZ, bx, y0, bz, 7, 7, 14, wall, roof, accent, glass);
                break;
        }
    }

    private void buildHouse(Chunk chunk, int baseX, int baseZ, int bx, int y0, int bz, int w, int h, int d, int wall, int roof, int accent, int glass) {
        int doorX = bx + w / 2;
        for (int x = bx; x < bx + w; x++) {
            for (int z = bz; z < bz + d; z++) {
                boolean perim = (x == bx || x == bx + w - 1 || z == bz || z == bz + d - 1);
                boolean corner = (x == bx || x == bx + w - 1) && (z == bz || z == bz + d - 1);
                for (int y = y0; y < y0 + h; y++) {
                    if (!perim) {
                        put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    int rel = y - y0;
                    boolean base = (rel == 0);
                    boolean band = (h >= 6) && (rel % 3 == 0) && rel > 0;
                    boolean door = (z == bz) && (x == doorX) && (rel < 2);
                    if (door) {
                        put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean win = !corner && !band && rel > 0 && ((x + z) % 2 == 0);
                    int id;
                    if (base || band || corner) id = accent;
                    else if (win) id = glass;
                    else id = wall;
                    put(chunk, baseX, baseZ, x, y, z, id);
                }
                put(chunk, baseX, baseZ, x, y0 + h, z, accent);
            }
        }
        buildGableRoof(chunk, baseX, baseZ, bx - 1, bz - 1, w + 2, d + 2, y0 + h + 1, roof, accent);
        if (((bx + bz) & 3) == 0) {
            put(chunk, baseX, baseZ, bx + 1, y0 + h + 1, bz + 1, roof);
            put(chunk, baseX, baseZ, bx + 1, y0 + h + 2, bz + 1, accent);
        }
    }

    private void buildGableRoof(Chunk chunk, int baseX, int baseZ, int x0, int z0, int w, int d, int baseY, int roof, int accent) {
        int ridgeX = x0 + w / 2;
        for (int x = x0; x < x0 + w; x++) {
            int dist = Math.abs(x - ridgeX);
            int peak = Math.max(0, w / 2 - dist);
            for (int z = z0; z < z0 + d; z++) {
                for (int y = baseY; y <= baseY + peak; y++) put(chunk, baseX, baseZ, x, y, z, roof);
            }
        }
        for (int z = z0; z < z0 + d; z++) put(chunk, baseX, baseZ, ridgeX, baseY + w / 2, z, accent);
        for (int x = x0; x < x0 + w; x++) {
            put(chunk, baseX, baseZ, x, baseY, z0, accent);
            put(chunk, baseX, baseZ, x, baseY, z0 + d - 1, accent);
        }
        for (int z = z0; z < z0 + d; z++) {
            put(chunk, baseX, baseZ, x0, baseY, z, accent);
            put(chunk, baseX, baseZ, x0 + w - 1, baseY, z, accent);
        }
    }

    private void buildTower(Chunk chunk, int baseX, int baseZ, int bx, int y0, int bz, int w, int h, int d, int wall, int roof, int accent, int glass) {
        int tiers = h >= 20 ? 4 : (h >= 13 ? 3 : (h >= 7 ? 2 : 1));
        int curW = w, curD = d, curX = bx, curZ = bz, y = y0;
        int remaining = h;
        for (int t = 0; t < tiers && remaining > 0; t++) {
            int seg = (t == tiers - 1) ? remaining : Math.max(3, remaining / (tiers - t));
            buildTowerSegment(chunk, baseX, baseZ, curX, y, curZ, curW, seg, curD,
                    wall, roof, accent, glass, t == 0);
            y += seg;
            remaining -= seg;
            if (remaining <= 0) break;
            curX += 1;
            curZ += 1;
            curW = Math.max(3, curW - 2);
            curD = Math.max(3, curD - 2);
        }
        // Crown: glass penthouse plus antenna beacon.
        int cx = curX + curW / 2;
        int cz = curZ + curD / 2;
        put(chunk, baseX, baseZ, cx, y, cz, glass);
        put(chunk, baseX, baseZ, cx, y + 1, cz, glass);
        put(chunk, baseX, baseZ, cx, y + 2, cz, accent);
        put(chunk, baseX, baseZ, cx, y + 3, cz, BlockType.GLOWSTONE.id);
    }

    private void buildTowerSegment(Chunk chunk, int baseX, int baseZ, int bx, int y0, int bz,
                                   int w, int h, int d, int wall, int roof, int accent, int glass, boolean ground) {
        for (int x = bx; x < bx + w; x++) {
            for (int z = bz; z < bz + d; z++) {
                boolean perim = (x == bx || x == bx + w - 1 || z == bz || z == bz + d - 1);
                boolean corner = (x == bx || x == bx + w - 1) && (z == bz || z == bz + d - 1);
                for (int y = y0; y < y0 + h; y++) {
                    if (!perim) {
                        put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean door = ground && (z == bz) && (x == bx + w / 2) && (y < y0 + 2);
                    if (door) {
                        put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean slab = ((y - y0) % 3 == 0);
                    boolean win = !corner && !slab && ((x + y + z) % 2 == 0);
                    int id = corner ? accent : (slab ? accent : (win ? glass : wall));
                    put(chunk, baseX, baseZ, x, y, z, id);
                }
                put(chunk, baseX, baseZ, x, y0 + h, z, roof);
            }
        }
    }

    private void buildPagoda(Chunk chunk, int baseX, int baseZ, int bx, int y0, int bz, int w, int h, int d, int wall, int roof, int accent, int glass) {
        int cw = w;
        int cd = d;
        int off = 0;
        int cy = y0;
        int floors = 0;
        while (cw > 2 && cd > 2 && floors < h) {
            buildBoxFloor(chunk, baseX, baseZ, bx + off, cy, bz + off, cw, cd, wall, accent, glass);
            int top = cy + 1;
            for (int x = bx + off - 1; x <= bx + off + cw; x++) {
                for (int z = bz + off - 1; z <= bz + off + cd; z++) {
                    if (x == bx + off - 1 || x == bx + off + cw || z == bz + off - 1 || z == bz + off + cd) {
                        put(chunk, baseX, baseZ, x, top, z, roof);
                    }
                }
            }
            off++;
            cw -= 2;
            cd -= 2;
            cy += 2;
            floors++;
        }
        put(chunk, baseX, baseZ, bx + w / 2, cy, bz + d / 2, accent);
        put(chunk, baseX, baseZ, bx + w / 2, cy + 1, bz + d / 2, BlockType.GLOWSTONE.id);
    }

    private void buildBoxFloor(Chunk chunk, int baseX, int baseZ, int bx, int y0, int bz, int w, int d, int wall, int accent, int glass) {
        for (int x = bx; x < bx + w; x++) {
            for (int z = bz; z < bz + d; z++) {
                boolean perim = (x == bx || x == bx + w - 1 || z == bz || z == bz + d - 1);
                boolean corner = (x == bx || x == bx + w - 1) && (z == bz || z == bz + d - 1);
                for (int y = y0; y < y0 + 2; y++) {
                    if (!perim) {
                        put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean win = (y == y0 + 1) && !corner && ((x + z) % 2 == 0);
                    int id = corner ? accent : (win ? glass : wall);
                    put(chunk, baseX, baseZ, x, y, z, id);
                }
            }
        }
    }

    private void buildDome(Chunk chunk, int baseX, int baseZ, int bx, int y0, int bz, int w, int h, int d, int wall, int roof, int accent, int glass) {
        for (int x = bx; x < bx + w; x++) {
            for (int z = bz; z < bz + d; z++) {
                boolean perim = (x == bx || x == bx + w - 1 || z == bz || z == bz + d - 1);
                boolean corner = (x == bx || x == bx + w - 1) && (z == bz || z == bz + d - 1);
                for (int y = y0; y < y0 + h; y++) {
                    if (!perim) {
                        put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean door = (z == bz) && (x == bx + w / 2) && (y < y0 + 2);
                    if (door) {
                        put(chunk, baseX, baseZ, x, y, z, 0);
                        continue;
                    }
                    boolean win = (y == y0 + 1 || y == y0 + 2) && !corner && ((x + z) % 2 == 0);
                    int id = corner ? accent : (win ? glass : wall);
                    put(chunk, baseX, baseZ, x, y, z, id);
                }
                put(chunk, baseX, baseZ, x, y0 + h, z, accent);
            }
        }
        int py = y0 + h + 1;
        int step = 0;
        while (w - step * 2 > 0) {
            for (int x = bx + step; x < bx + w - step; x++) {
                for (int z = bz + step; z < bz + d - step; z++) {
                    put(chunk, baseX, baseZ, x, py, z, roof);
                }
            }
            step++;
            py++;
        }
        put(chunk, baseX, baseZ, bx + w / 2, py - 1, bz + d / 2, accent);
    }

    private void buildStation(Chunk chunk, int baseX, int baseZ, int wx0, int wz0, int target) {
        int ty = World.TRACK_Y;
        int hx = 11, hz = 7;
        int roofY = ty + 9;
        int frame = BlockType.STONE_BRICK.id;
        int wall = BlockType.WHITE_PLASTER.id;
        int roof = BlockType.DARK_PLASTER.id;
        int glass = BlockType.GLASS.id;
        int glow = BlockType.GLOWSTONE.id;
        int accent = BlockType.RED_PLASTER.id;

        // Platform floor.
        for (int dx = -hx; dx <= hx; dx++)
            for (int dz = -hz; dz <= hz; dz++)
                put(chunk, baseX, baseZ, wx0 + dx, ty - 1, wz0 + dz, frame);

        // Ground supports down from the floating platform.
        for (int dx = -hx + 1; dx <= hx - 1; dx += 5)
            for (int dz = -hz + 1; dz <= hz - 1; dz += 5)
                for (int y = target; y < ty - 1; y++)
                    put(chunk, baseX, baseZ, wx0 + dx, y, wz0 + dz, frame);

        // Hollow concourse volume.
        for (int dx = -hx; dx <= hx; dx++)
            for (int dz = -hz; dz <= hz; dz++)
                for (int y = ty; y <= roofY; y++)
                    put(chunk, baseX, baseZ, wx0 + dx, y, wz0 + dz, 0);

        // Grand pillars (kept clear of the crossing tracks at x==0 and z==0).
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

        // Curtain-wall glass facade, open where the rails run through.
        for (int dx = -hx; dx <= hx; dx++) {
            if (Math.abs(dx) <= 2) continue;
            for (int y = ty + 1; y <= ty + 6; y++) {
                put(chunk, baseX, baseZ, wx0 + dx, y, wz0 - hz, glass);
                put(chunk, baseX, baseZ, wx0 + dx, y, wz0 + hz, glass);
            }
        }
        for (int dz = -hz; dz <= hz; dz++) {
            if (Math.abs(dz) <= 2) continue;
            for (int y = ty + 1; y <= ty + 6; y++) {
                put(chunk, baseX, baseZ, wx0 - hx, y, wz0 + dz, glass);
                put(chunk, baseX, baseZ, wx0 + hx, y, wz0 + dz, glass);
            }
        }

        // Large flat roof with a cross-shaped skylight.
        for (int dx = -hx - 1; dx <= hx + 1; dx++) {
            for (int dz = -hz - 1; dz <= hz + 1; dz++) {
                boolean skylight = (Math.abs(dz) <= 1 && Math.abs(dx) <= hx - 2)
                        || (Math.abs(dx) <= 1 && Math.abs(dz) <= hz - 2);
                put(chunk, baseX, baseZ, wx0 + dx, roofY, wz0 + dz, skylight ? glass : roof);
            }
        }

        // Glass corner towers with beacons.
        int[] cxs = {-hx, hx};
        int[] czs = {-hz, hz};
        for (int tx : cxs) {
            for (int tz : czs) {
                for (int ax = -1; ax <= 1; ax++)
                    for (int az = -1; az <= 1; az++)
                        for (int y = ty; y <= roofY + 4; y++)
                            put(chunk, baseX, baseZ, wx0 + tx + ax, y, wz0 + tz + az,
                                    (ax != 0 || az != 0) ? glass : wall);
                put(chunk, baseX, baseZ, wx0 + tx, roofY + 5, wz0 + tz, glow);
            }
        }

        // Modern station sign above the rail entrance.
        for (int y = roofY + 1; y <= roofY + 7; y++)
            put(chunk, baseX, baseZ, wx0 - hx, y, wz0, accent);
        put(chunk, baseX, baseZ, wx0 - hx, roofY + 8, wz0, glow);
    }

    private void put(Chunk chunk, int baseX, int baseZ, int wx, int wy, int wz, int id) {
        int lx = wx - baseX, lz = wz - baseZ;
        if (lx < 0 || lx >= Chunk.SIZE_X || lz < 0 || lz >= Chunk.SIZE_Z) return;
        if (wy < 0 || wy >= Chunk.SIZE_Y) return;
        chunk.set(lx, wy, lz, (byte) id);
    }

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
        for (int y = ty; y <= ty + 3; y++) chunk.set(lx, y, lz, (byte) 0);
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
                chunk.set(lx, y, lz, (byte) BlockType.STONE.id);
            }
        }
    }
}
