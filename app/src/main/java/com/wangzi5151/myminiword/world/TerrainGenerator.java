package com.wangzi5151.myminiword.world;

import java.util.Random;

/**
 * Layered world generator: continental shelf -> rolling hills -> ridged mountains,
 * domain-warped coastlines, carved rivers, 3D noise caves, ore veins and
 * biome-aware vegetation. All of it is pure math on (seed, x, z), so chunks can be
 * generated independently and still line up across chunk borders.
 */
public class TerrainGenerator {
    public static final int SEA_LEVEL = 22;
    public static final int SNOW_LINE = 44;
    public static final int MAX_SURFACE = Chunk.SIZE_Y - 4;

    private final long seed;
    private final Noise continentNoise;
    private final Noise hillNoise;
    private final Noise mountainNoise;
    private final Noise riverNoise;
    private final Noise warpNoise;
    private final Noise detailNoise;
    private final Noise tempNoise;
    private final Noise humidNoise;
    private final Noise caveNoiseA;
    private final Noise caveNoiseB;
    private final Noise caveNoiseC;
    private final Noise oreNoise;
    private final Noise floraNoise;
    private final Decoration decoration;

    public TerrainGenerator(long seed) {
        this.seed = seed;
        continentNoise = new Noise(seed);
        hillNoise = new Noise(seed * 31 + 7);
        mountainNoise = new Noise(seed * 977 + 53);
        riverNoise = new Noise(seed * 613 + 29);
        warpNoise = new Noise(seed * 137 + 11);
        detailNoise = new Noise(seed * 17 + 5);
        tempNoise = new Noise(seed * 2711 + 3);
        humidNoise = new Noise(seed * 3571 + 19);
        caveNoiseA = new Noise(seed * 8191 + 101);
        caveNoiseB = new Noise(seed * 8191 + 211);
        caveNoiseC = new Noise(seed * 6553 + 307);
        oreNoise = new Noise(seed * 4099 + 37);
        floraNoise = new Noise(seed * 5231 + 9);
        decoration = new Decoration(this, seed);
    }

    public long getSeed() {
        return seed;
    }

    public void generate(Chunk chunk) {
        final int baseX = chunk.cx * Chunk.SIZE_X;
        final int baseZ = chunk.cz * Chunk.SIZE_Z;
        final int sx = Chunk.SIZE_X;
        final int sz = Chunk.SIZE_Z;
        int[][] heights = new int[sx][sz];

        for (int x = 0; x < sx; x++) {
            for (int z = 0; z < sz; z++) {
                heights[x][z] = surfaceHeight(baseX + x, baseZ + z);
            }
        }

        for (int x = 0; x < sx; x++) {
            for (int z = 0; z < sz; z++) {
                int wx = baseX + x;
                int wz = baseZ + z;
                int h = heights[x][z];
                float temp = temperature(wx, wz);
                float humid = humidity(wx, wz);
                int biomeId = Biome.select(temp, humid, h, SEA_LEVEL);
                Biome biome = Biome.get(biomeId);

                int slope = slopeAt(baseX, baseZ, x, z, h, heights);
                boolean underwater = h < SEA_LEVEL;
                boolean cliff = slope >= 3 && h > SEA_LEVEL + 2;

                // Stone body with strata.
                for (int y = 1; y < h - 3; y++) {
                    int id = BlockType.STONE.id;
                    if (y < 22) {
                        float band = detailNoise.fbm2(wx * 0.03f + y * 0.11f, wz * 0.03f, 2, 2f, 0.5f);
                        if (band > 0.42f) id = BlockType.GRANITE.id;
                        else if (band < -0.46f) id = BlockType.BASALT.id;
                    }
                    chunk.set(x, y, z, (byte) id);
                }

                int filler = biome.filler;
                if (underwater) {
                    filler = h < SEA_LEVEL - 6 ? BlockType.STONE.id
                            : (humid < -0.1f ? BlockType.SANDSTONE.id : BlockType.SAND.id);
                } else if (biomeId == Biome.SNOWY_PEAKS || biomeId == Biome.MOUNTAIN) {
                    filler = BlockType.STONE.id;
                }
                for (int y = Math.max(1, h - 3); y < h; y++) {
                    chunk.set(x, y, z, (byte) filler);
                }

                int top;
                if (underwater) {
                    if (h < SEA_LEVEL - 8) top = BlockType.GRAVEL.id;
                    else if (biomeId == Biome.SWAMP) top = BlockType.MUD.id;
                    else if (biomeId == Biome.OCEAN) top = (humid > 0.1f ? BlockType.CLAY.id : BlockType.SAND.id);
                    else top = BlockType.SAND.id;
                } else if (cliff) {
                    top = (biomeId == Biome.SNOWY_PEAKS) ? BlockType.SNOW.id : BlockType.STONE.id;
                } else {
                    switch (biomeId) {
                        case Biome.SNOWY_PLAINS:
                        case Biome.SNOWY_PEAKS:
                            top = BlockType.SNOW.id;
                            break;
                        case Biome.DESERT:
                            top = BlockType.SAND.id;
                            break;
                        case Biome.BEACH:
                            top = BlockType.SAND.id;
                            break;
                        case Biome.MOUNTAIN:
                            top = (h > 40 ? BlockType.SNOW.id : BlockType.STONE.id);
                            break;
                        case Biome.SWAMP:
                            top = (detailNoise.perlin2(wx * 0.14f, wz * 0.14f) > 0.25f)
                                    ? BlockType.MUD.id : BlockType.GRASS.id;
                            break;
                        default:
                            top = h >= SNOW_LINE ? BlockType.SNOW.id : biome.top;
                            if (h >= SNOW_LINE) filler = BlockType.DIRT.id;
                    }
                }
                chunk.set(x, h, z, (byte) top);
                chunk.set(x, 0, z, (byte) BlockType.BEDROCK.id);

                // Water / ice sheet.
                if (h < SEA_LEVEL) {
                    for (int y = h + 1; y <= SEA_LEVEL; y++) {
                        chunk.set(x, y, z, (byte) BlockType.WATER.id);
                    }
                    if (temp < -0.42f) chunk.set(x, SEA_LEVEL, z, (byte) BlockType.ICE.id);
                }
            }
        }

        carveCaves(chunk, heights);
        generateOres(chunk);
        decoration.populate(chunk, baseX, baseZ, heights);
    }

    private int slopeAt(int baseX, int baseZ, int x, int z, int h, int[][] heights) {
        int slope = 0;
        for (int d = 0; d < 4; d++) {
            int nx = x + (d == 0 ? 1 : d == 1 ? -1 : 0);
            int nz = z + (d == 2 ? 1 : d == 3 ? -1 : 0);
            int nh;
            if (nx >= 0 && nx < Chunk.SIZE_X && nz >= 0 && nz < Chunk.SIZE_Z) {
                nh = heights[nx][nz];
            } else {
                nh = surfaceHeight(baseX + nx, baseZ + nz);
            }
            slope = Math.max(slope, Math.abs(nh - h));
        }
        return slope;
    }

    public int surfaceHeight(int wx, int wz) {
        // Domain warp keeps coastlines and ridges from looking like noise blobs.
        float wxp = wx + warpNoise.fbm2(wx * 0.0021f, wz * 0.0021f, 2, 2f, 0.5f) * 26f;
        float wzp = wz + warpNoise.fbm2(wx * 0.0021f + 31.7f, wz * 0.0021f - 17.3f, 2, 2f, 0.5f) * 26f;

        float continent = continentNoise.fbm2(wxp * 0.0012f, wzp * 0.0012f, 4, 2f, 0.5f);
        float swells = continentNoise.fbm2(wx * 0.0026f + 41.3f, wz * 0.0026f - 27.1f, 3, 2f, 0.5f);
        float hills = hillNoise.fbm2(wx * 0.0062f, wz * 0.0062f, 4, 2f, 0.5f);
        float ridge = mountainNoise.ridged2(wx * 0.0021f, wz * 0.0021f, 5);
        float region = mountainNoise.fbm2(wxp * 0.0011f, wzp * 0.0011f, 3, 2f, 0.5f) * 1.55f;

        // Contrast curve: turns smooth continent noise into real basins + plateaus.
        float land = smoothstep(-0.15f, -0.01f, continent);
        float h = 14f + land * 18f + swells * 11f + hills * 5.5f + continent * 4f;
        float mountainMask = smoothstep(0.06f, 0.52f, region);
        h += mountainMask * mountainMask * ridge * 62f * (0.45f + 0.55f * land);

        // Soft cliff terraces in the high country.
        if (mountainMask > 0.42f && h > SEA_LEVEL + 8) {
            float step = 4f;
            float terraced = Math.round(h / step) * step;
            float t = smoothstep(0.42f, 0.85f, mountainMask) * 0.45f;
            h = h * (1f - t) + terraced * t;
        }

        // Rivers: narrow noise valleys that eventually reach the sea.
        float riverVal = Math.abs(riverNoise.fbm2(wx * 0.0016f, wz * 0.0016f, 2, 2f, 0.5f));
        float river = 1f - smoothstep(0.010f, 0.042f, riverVal);
        if (river > 0f && h < SEA_LEVEL + 26f) {
            float bed = SEA_LEVEL - 3f;
            float t = river * river * river * 0.95f;
            h = h * (1f - t) + bed * t;
        }

        // Fine surface roughness (sub-block scale, rounded away for most columns).
        h += detailNoise.fbm2(wx * 0.09f, wz * 0.09f, 2, 2f, 0.5f) * 1.2f;

        int y = Math.round(h);
        if (y < 2) y = 2;
        if (y > MAX_SURFACE) y = MAX_SURFACE;
        return y;
    }

    public float temperature(int wx, int wz) {
        float t = tempNoise.fbm2(wx * 0.00085f, wz * 0.00085f, 3, 2f, 0.5f) * 2.1f + 0.14f;
        // Polar caps: colder toward the far north/south of the map.
        t -= Math.abs(wz) * 0.00018f;
        return clamp1(t);
    }

    public float humidity(int wx, int wz) {
        float h = humidNoise.fbm2(wx * 0.0011f + 13.7f, wz * 0.0011f - 8.3f, 3, 2f, 0.5f) * 2.05f;
        return clamp1(h);
    }

    private static float clamp1(float v) {
        return v < -1f ? -1f : (v > 1f ? 1f : v);
    }

    /** Low-frequency plant density field (flower meadows, fern patches). */
    public float flora(int wx, int wz) {
        return floraNoise.fbm2(wx * 0.021f, wz * 0.021f, 2, 2f, 0.5f);
    }

    public int biomeIdAt(int wx, int wz) {
        return Biome.select(temperature(wx, wz), humidity(wx, wz), surfaceHeight(wx, wz), SEA_LEVEL);
    }

    public Biome biomeAt(int wx, int wz) {
        return Biome.get(biomeIdAt(wx, wz));
    }

    private void carveCaves(Chunk chunk, int[][] heights) {
        for (int x = 0; x < Chunk.SIZE_X; x++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                int h = heights[x][z];
                int maxY = Math.min(46, h - 4);
                for (int y = 2; y <= maxY; y++) {
                    int id = chunk.get(x, y, z);
                    if (id == 0 || id == BlockType.BEDROCK.id) continue;
                    float a = caveNoiseA.perlin3(x * 0.030f, y * 0.042f, z * 0.030f);
                    float b = caveNoiseB.perlin3(x * 0.030f, y * 0.042f, z * 0.030f);
                    boolean carve = a * a + b * b < 0.0024f;
                    if (!carve) {
                        float c = caveNoiseC.perlin3(x * 0.015f, y * 0.026f, z * 0.015f);
                        carve = c > 0.638f;
                    }
                    if (carve) chunk.set(x, y, z, (byte) 0);
                }
            }
        }
    }

    private void generateOres(Chunk chunk) {
        Random r = new Random(seed ^ ((long) chunk.cx * 341873128712L) ^ ((long) chunk.cz * 132897987541L));
        // Coal: shallow & plentiful.
        for (int i = 0; i < 14; i++) vein(chunk, r, BlockType.COAL_ORE.id, 6, 6, 52);
        for (int i = 0; i < 9; i++) vein(chunk, r, BlockType.IRON_ORE.id, 5, 4, 40);
        for (int i = 0; i < 5; i++) vein(chunk, r, BlockType.GOLD_ORE.id, 4, 2, 26);
        for (int i = 0; i < 3; i++) vein(chunk, r, BlockType.DIAMOND_ORE.id, 3, 2, 15);
        for (int i = 0; i < 6; i++) vein(chunk, r, BlockType.GRAVEL.id, 7, 4, 40);
        for (int i = 0; i < 4; i++) vein(chunk, r, BlockType.CLAY.id, 6, 4, 30);
    }

    /** Random-walk vein: gives connected pockets instead of isolated specks. */
    private void vein(Chunk chunk, Random r, int block, int size, int minY, int maxY) {
        int x = r.nextInt(Chunk.SIZE_X);
        int z = r.nextInt(Chunk.SIZE_Z);
        int y = minY + r.nextInt(Math.max(1, maxY - minY));
        for (int i = 0; i < size; i++) {
            if (chunk.get(x, y, z) == BlockType.STONE.id) chunk.set(x, y, z, (byte) block);
            int axis = r.nextInt(3);
            if (axis == 0) x += r.nextInt(3) - 1;
            else if (axis == 1) y += r.nextInt(3) - 1;
            else z += r.nextInt(3) - 1;
            if (x < 0) x = 0;
            if (x > Chunk.SIZE_X - 1) x = Chunk.SIZE_X - 1;
            if (z < 0) z = 0;
            if (z > Chunk.SIZE_Z - 1) z = Chunk.SIZE_Z - 1;
            if (y < 2) y = 2;
            if (y > Chunk.SIZE_Y - 2) y = Chunk.SIZE_Y - 2;
        }
    }

    public static float smoothstep(float e0, float e1, float x) {
        float t = (x - e0) / (e1 - e0);
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return t * t * (3f - 2f * t);
    }
}
