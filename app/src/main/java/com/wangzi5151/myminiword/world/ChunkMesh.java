package com.wangzi5151.myminiword.world;

import com.wangzi5151.myminiword.engine.TextureAtlas;

import java.util.ArrayList;
import java.util.Arrays;

public class ChunkMesh {

    private static final float[] FACE_SHADE = {0.86f, 0.86f, 1.00f, 0.62f, 0.72f, 0.72f};
    private static final int FLOATS_PER_VERTEX = 7;
    private static final int SX = Chunk.SIZE_X;
    private static final int SY = Chunk.SIZE_Y;
    private static final int SZ = Chunk.SIZE_Z;

    // 4 corners {x,y,z,u,v} per face (20 floats), used for non-opaque blocks and held items
    private static final float[][] FACE_VERTS = {
            {1, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0, 0, 0},
            {0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 0},
            {0, 1, 1, 0, 0, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 0, 0, 1},
            {0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 1, 1, 1, 0, 0, 1, 0, 1},
            {1, 0, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 0},
            {0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 0, 0}
    };

    private static final int[][] FACE_NORMALS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    private static final int PW = SX + 2;
    private static final int PH = SY + 2;
    private static final int PD = SZ + 2;
    private static final int PAD_SIZE = PW * PH * PD;

    private static int pidx(int x, int y, int z) {
        return ((y + 1) * PD + (z + 1)) * PW + (x + 1);
    }

    private static int padGet(byte[] pad, int x, int y, int z) {
        if (x < -1 || x > SX || y < -1 || y > SY || z < -1 || z > SZ) return 0;
        return pad[pidx(x, y, z)] & 0xff;
    }

    private static boolean opaqueId(int id) {
        return id > 0 && id < BlockType.OPAQUE.length && BlockType.OPAQUE[id];
    }

    // ------------------------------------------------------------------
    // Reusable per-thread scratch state: meshing runs on 2-4 worker threads and
    // allocating a 20k pad plus several mesh buffers per chunk was the single
    // biggest source of GC churn.
    // ------------------------------------------------------------------

    private static final class Ctx {
        final byte[] pad = new byte[PAD_SIZE];
        final int[] mask = new int[SX * SY];
        final int[] p = new int[3];
        final Chunk[] neighbors = new Chunk[9];
        final MeshBuffer opaque = new MeshBuffer(8192, 6144);
        final MeshBuffer transparent = new MeshBuffer(2048, 1536);
        final MeshBuffer liquid = new MeshBuffer(1536, 1024);
    }

    private static final ThreadLocal<Ctx> CTX = new ThreadLocal<Ctx>() {
        @Override
        protected Ctx initialValue() {
            return new Ctx();
        }
    };

    public static void build(World world, Chunk chunk) {
        Ctx ctx = CTX.get();
        fillPad(world, chunk, ctx);
        ctx.opaque.reset();
        ctx.transparent.reset();
        ctx.liquid.reset();

        greedyOpaque(ctx.pad, ctx.opaque, ctx.mask, ctx.p);
        perFaceNonOpaque(ctx.pad, ctx.transparent, ctx.liquid);
        emitRails(ctx.pad, ctx.transparent);

        chunk.opaqueMesh.set(ctx.opaque.finish());
        chunk.transparentMesh.set(ctx.transparent.finish());
        chunk.liquidMesh.set(ctx.liquid.finish());
    }

    /**
     * Builds the 18x66x18 padded neighbourhood from direct chunk array reads.
     * Previously every one of the ~19k lookups went through a concurrent hash map.
     */
    private static void fillPad(World world, Chunk chunk, Ctx ctx) {
        byte[] pad = ctx.pad;
        Arrays.fill(pad, (byte) 0);
        world.getNeighborChunks(chunk.cx, chunk.cz, ctx.neighbors);
        for (int y = -1; y <= SY; y++) {
            if (y < 0 || y >= SY) continue;
            for (int z = -1; z <= SZ; z++) {
                int ncz = z < 0 ? 0 : (z >= SZ ? 2 : 1);
                int bz = z & 15;
                for (int x = -1; x <= SX; x++) {
                    int ncx = x < 0 ? 0 : (x >= SX ? 2 : 1);
                    Chunk c = ctx.neighbors[ncz * 3 + ncx];
                    if (c == null) continue;
                    int bx = x & 15;
                    pad[pidx(x, y, z)] = c.blocks[(y * SZ + bz) * SX + bx];
                }
            }
        }
    }

    // ---- Greedy meshing for opaque blocks ----
    private static void greedyOpaque(byte[] pad, MeshBuffer out, int[] mask, int[] xyz) {
        int[] dims = {SX, SY, SZ};
        int[] q = new int[3];

        for (int d = 0; d < 3; d++) {
            int u, v;
            if (d == 0) {
                u = 2; v = 1;      // X faces: U along Z, V along Y (vertical)
            } else if (d == 1) {
                u = 0; v = 2;      // Y faces: U along X, V along Z
            } else {
                u = 0; v = 1;      // Z faces: U along X, V along Y (vertical)
            }
            q[0] = q[1] = q[2] = 0;
            q[d] = 1;
            int mw = dims[u];
            int mh = dims[v];

            for (xyz[d] = -1; xyz[d] < dims[d]; ) {
                int n = 0;
                for (xyz[v] = 0; xyz[v] < mh; xyz[v]++) {
                    for (xyz[u] = 0; xyz[u] < mw; xyz[u]++) {
                        int a = padGet(pad, xyz[0] + q[0], xyz[1] + q[1], xyz[2] + q[2]);
                        int b = padGet(pad, xyz[0], xyz[1], xyz[2]);
                        boolean af = opaqueId(a);
                        boolean bf = opaqueId(b);
                        int m = 0;
                        if (af && !bf) {
                            if (xyz[d] + 1 >= 0 && xyz[d] + 1 < dims[d]) m = a;
                        } else if (bf && !af) {
                            if (xyz[d] >= 0 && xyz[d] < dims[d]) m = -b;
                        }
                        mask[n++] = m;
                    }
                }
                xyz[d]++;

                n = 0;
                for (int j = 0; j < mh; j++) {
                    for (int i = 0; i < mw; ) {
                        int c = mask[j * mw + i];
                        if (c == 0) {
                            i++;
                            continue;
                        }
                        int w = 1;
                        while (i + w < mw && mask[j * mw + i + w] == c) w++;
                        int h = 1;
                        boolean done = false;
                        while (j + h < mh) {
                            for (int k = 0; k < w; k++) {
                                if (mask[(j + h) * mw + i + k] != c) {
                                    done = true;
                                    break;
                                }
                            }
                            if (done) break;
                            h++;
                        }
                        int[] pos = new int[3];
                        pos[d] = xyz[d];
                        emitGreedy(out, pad, d, u, v, pos, i, j, w, h, c);
                        for (int l = 0; l < h; l++)
                            for (int k = 0; k < w; k++)
                                mask[(j + l) * mw + i + k] = 0;
                        i += w;
                    }
                }
            }
        }
    }

    private static int faceOf(int d, int sign) {
        switch (d) {
            case 0: return sign > 0 ? BlockType.FACE_PX : BlockType.FACE_NX;
            case 1: return sign > 0 ? BlockType.FACE_PY : BlockType.FACE_NY;
            default: return sign > 0 ? BlockType.FACE_PZ : BlockType.FACE_NZ;
        }
    }

    private static void emitGreedy(MeshBuffer out, byte[] pad,
                                   int d, int u, int v, int[] pos, int i, int j, int w, int h, int c) {
        int sign = c > 0 ? 1 : -1;
        int block = c > 0 ? c : -c;
        int face = faceOf(d, sign);
        int tile = BlockType.get(block).getTile(face);
        float baseShade = FACE_SHADE[face];
        if (BlockType.GLOW[block]) baseShade *= 1.75f;
        boolean vFlip = (d != 1);

        int[] cu = {i, i + w, i + w, i};
        int[] cv = {j, j, j + h, j + h};
        int[] su = {-1, 1, 1, -1};
        int[] sv = {-1, -1, 1, 1};
        int[] a = new int[3];

        out.ensureRoom(4, 6);
        int base = out.vertexBase();
        int iu = i + w - 1;
        int jv = j + h - 1;
        for (int k = 0; k < 4; k++) {
            pos[u] = cu[k];
            pos[v] = cv[k];

            int iu2 = su[k] < 0 ? i : iu;
            int jv2 = sv[k] < 0 ? j : jv;
            int bd = sign > 0 ? pos[d] : pos[d] - 1;

            a[d] = bd + sign; a[u] = iu2 + su[k]; a[v] = jv2;
            int occ1 = opaqueId(padGet(pad, a[0], a[1], a[2])) ? 1 : 0;
            a[d] = bd + sign; a[u] = iu2; a[v] = jv2 + sv[k];
            int occ2 = opaqueId(padGet(pad, a[0], a[1], a[2])) ? 1 : 0;
            a[d] = bd + sign; a[u] = iu2 + su[k]; a[v] = jv2 + sv[k];
            int occC = opaqueId(padGet(pad, a[0], a[1], a[2])) ? 1 : 0;

            int ao = (occ1 == 1 && occ2 == 1) ? 0 : 3 - (occ1 + occ2 + occC);
            float aoFactor = 0.58f + (ao / 3f) * 0.42f;

            out.addVertex(pos[0], pos[1], pos[2], tile,
                    cu[k] - i, vFlip ? (j + h) - cv[k] : cv[k] - j, baseShade * aoFactor);
        }
        out.addTriangle(base, base + 1, base + 2);
        out.addTriangle(base, base + 2, base + 3);
    }

    // ---- Per-face meshing for glass / water / plants ----
    private static void perFaceNonOpaque(byte[] pad, MeshBuffer tv, MeshBuffer lv) {
        for (int y = 0; y < SY; y++) {
            for (int z = 0; z < SZ; z++) {
                for (int x = 0; x < SX; x++) {
                    int id = padGet(pad, x, y, z);
                    if (id == 0) continue;
                    if (id == BlockType.RAIL_X.id || id == BlockType.RAIL_Z.id) continue;
                    BlockType bt = BlockType.get(id);
                    if (bt.opaque) continue;
                    if (bt.cross) {
                        emitCross(tv, x, y, z, bt.getTile(BlockType.FACE_PX), x, z);
                        continue;
                    }
                    MeshBuffer out = bt.liquid ? lv : tv;
                    float topScale = 1f;
                    if (bt.liquid) {
                        int above = padGet(pad, x, y + 1, z);
                        if (above != id) topScale = 0.875f;
                    }
                    for (int face = 0; face < 6; face++) {
                        int nid = padGet(pad, x + FACE_NORMALS[face][0],
                                y + FACE_NORMALS[face][1],
                                z + FACE_NORMALS[face][2]);
                        if (!shouldRenderFace(id, nid)) continue;
                        addFace(out, x, y, z, face, bt.getTile(face), topScale, BlockType.GLOW[id]);
                    }
                }
            }
        }
    }

    private static void emitRails(byte[] pad, MeshBuffer out) {
        int railX = BlockType.RAIL_X.id;
        int railZ = BlockType.RAIL_Z.id;
        for (int y = 0; y < SY; y++) {
            for (int z = 0; z < SZ; z++) {
                for (int x = 0; x < SX; x++) {
                    int id = padGet(pad, x, y, z);
                    if (id != railX && id != railZ) continue;
                    int tile = id == railX ? TextureAtlas.RAIL_X : TextureAtlas.RAIL_Z;
                    float yy = y + 0.0625f;
                    out.ensureRoom(4, 6);
                    int base = out.vertexBase();
                    addVert(out, x, yy, z, tile, 0, 0, 0.95f);
                    addVert(out, x + 1, yy, z, tile, 1, 0, 0.95f);
                    addVert(out, x + 1, yy, z + 1, tile, 1, 1, 0.95f);
                    addVert(out, x, yy, z + 1, tile, 0, 1, 0.95f);
                    out.addTriangle(base, base + 1, base + 2);
                    out.addTriangle(base, base + 2, base + 3);
                }
            }
        }
    }

    /** Two crossed quads, the classic voxel plant billboard. */
    private static void emitCross(MeshBuffer out, int x, int y, int z, int tile, int wx, int wz) {
        int h = 12 + (Math.floorMod(wx * 73856093 ^ wz * 19349663, 5));
        float top = Math.min(1f, h / 16f);
        out.ensureRoom(8, 12);
        int base = out.vertexBase();
        addVert(out, x + 0.15f, y, z + 0.15f, tile, 0, 1, 1f);
        addVert(out, x + 0.85f, y, z + 0.85f, tile, 1, 1, 1f);
        addVert(out, x + 0.85f, y + top, z + 0.85f, tile, 1, 0, 1f);
        addVert(out, x + 0.15f, y + top, z + 0.15f, tile, 0, 0, 1f);
        addVert(out, x + 0.85f, y, z + 0.15f, tile, 0, 1, 1f);
        addVert(out, x + 0.15f, y, z + 0.85f, tile, 1, 1, 1f);
        addVert(out, x + 0.15f, y + top, z + 0.85f, tile, 1, 0, 1f);
        addVert(out, x + 0.85f, y + top, z + 0.15f, tile, 0, 0, 1f);
        out.addTriangle(base, base + 1, base + 2);
        out.addTriangle(base, base + 2, base + 3);
        out.addTriangle(base + 4, base + 5, base + 6);
        out.addTriangle(base + 4, base + 6, base + 7);
    }

    private static void addVert(MeshBuffer out, float x, float y, float z, int tile, float u, float v, float shade) {
        out.addVertex(x, y, z, tile, u, v, shade);
    }

    private static boolean shouldRenderFace(int current, int neighbor) {
        if (neighbor == 0) return true;
        BlockType nb = BlockType.get(neighbor);
        if (nb.opaque) return false;
        if (current == neighbor) return false;
        return true;
    }

    private static void addFace(MeshBuffer out, int x, int y, int z, int face, int tile,
                                float topScale, boolean glow) {
        float[] verts = FACE_VERTS[face];
        float shade = FACE_SHADE[face] * (glow ? 1.75f : 1f);
        out.ensureRoom(4, 6);
        int base = out.vertexBase();
        for (int c = 0; c < 4; c++) {
            int o = c * 5;
            float vy = verts[o + 1] > 0f ? topScale : 0f;
            out.addVertex(x + verts[o], y + vy, z + verts[o + 2],
                    tile, verts[o + 3], verts[o + 4], shade);
        }
        out.addTriangle(base, base + 1, base + 2);
        out.addTriangle(base, base + 2, base + 3);
    }

    public static MeshData buildCartMesh() {
        MeshBuffer out = new MeshBuffer(7 * 6 * FLOATS_PER_VERTEX, 7 * 6);
        int iron = TextureAtlas.IRON_BLOCK;
        int gold = TextureAtlas.GOLD_BLOCK;
        emitBox(out, iron, -0.45f, 0.00f, -0.50f, 0.45f, 0.10f, 0.50f);
        emitBox(out, iron, -0.45f, 0.10f, -0.50f, -0.37f, 0.55f, 0.50f);
        emitBox(out, iron, 0.37f, 0.10f, -0.50f, 0.45f, 0.55f, 0.50f);
        emitBox(out, iron, -0.45f, 0.10f, -0.50f, 0.45f, 0.55f, -0.42f);
        emitBox(out, iron, -0.45f, 0.10f, 0.42f, 0.45f, 0.55f, 0.50f);
        emitBox(out, gold, -0.47f, 0.55f, -0.52f, 0.47f, 0.60f, -0.40f);
        emitBox(out, gold, -0.47f, 0.55f, 0.40f, 0.47f, 0.60f, 0.52f);
        emitBox(out, gold, -0.47f, 0.55f, -0.52f, -0.35f, 0.60f, 0.52f);
        emitBox(out, gold, 0.35f, 0.55f, -0.52f, 0.47f, 0.60f, 0.52f);
        return out.finish();
    }

    public static MeshData buildVillagerMesh() {
        MeshBuffer out = new MeshBuffer(10 * 6 * FLOATS_PER_VERTEX, 10 * 6);
        int skin = TextureAtlas.SAND;
        int robe = TextureAtlas.BLUE_PLASTER;
        int dark = TextureAtlas.DARK_PLASTER;
        emitBox(out, dark, -0.20f, 0.00f, -0.10f, -0.06f, 0.72f, 0.10f);
        emitBox(out, dark, 0.06f, 0.00f, -0.10f, 0.20f, 0.72f, 0.10f);
        emitBox(out, robe, -0.22f, 0.72f, -0.13f, 0.22f, 1.28f, 0.13f);
        emitBox(out, robe, -0.34f, 0.78f, -0.11f, -0.22f, 1.24f, 0.11f);
        emitBox(out, robe, 0.22f, 0.78f, -0.11f, 0.34f, 1.24f, 0.11f);
        emitBox(out, skin, -0.08f, 1.28f, -0.08f, 0.08f, 1.34f, 0.08f);
        emitBox(out, skin, -0.18f, 1.34f, -0.18f, 0.18f, 1.70f, 0.18f);
        emitBox(out, dark, -0.19f, 1.66f, -0.19f, 0.19f, 1.76f, 0.19f);
        emitBox(out, skin, -0.05f, 1.46f, 0.18f, 0.05f, 1.56f, 0.24f);
        return out.finish();
    }

    private static void emitBox(MeshBuffer out, int tile,
                                float x0, float y0, float z0, float x1, float y1, float z1) {
        for (int face = 0; face < 6; face++) {
            float[] fv = FACE_VERTS[face];
            float shade = FACE_SHADE[face];
            out.ensureRoom(4, 6);
            int base = out.vertexBase();
            for (int c = 0; c < 4; c++) {
                int o = c * 5;
                out.addVertex(x0 + fv[o] * (x1 - x0),
                        y0 + fv[o + 1] * (y1 - y0),
                        z0 + fv[o + 2] * (z1 - z0),
                        tile, fv[o + 3], fv[o + 4], shade);
            }
            out.addTriangle(base, base + 1, base + 2);
            out.addTriangle(base, base + 2, base + 3);
        }
    }

    public static MeshData buildBlockMesh(int blockId) {
        BlockType bt = BlockType.get(blockId);
        MeshBuffer out = new MeshBuffer(6 * 4 * FLOATS_PER_VERTEX, 6 * 6);
        if (bt.cross) {
            emitCross(out, 0, 0, 0, bt.getTile(BlockType.FACE_PX), 3, 5);
            return out.finish();
        }
        for (int face = 0; face < 6; face++) {
            float[] verts = FACE_VERTS[face];
            float shade = FACE_SHADE[face];
            int tile = bt.getTile(face);
            out.ensureRoom(4, 6);
            int base = out.vertexBase();
            for (int c = 0; c < 4; c++) {
                int o = c * 5;
                out.addVertex(verts[o], verts[o + 1], verts[o + 2],
                        tile, verts[o + 3], verts[o + 4], shade);
            }
            out.addTriangle(base, base + 1, base + 2);
            out.addTriangle(base, base + 2, base + 3);
        }
        return out.finish();
    }

    private static class MeshBuffer {
        private final FloatList verts;
        private final ShortList indices;
        private final ArrayList<MeshData.Part> parts = new ArrayList<>();

        MeshBuffer(int vertexCapacity, int indexCapacity) {
            verts = new FloatList(Math.max(16, vertexCapacity));
            indices = new ShortList(Math.max(16, indexCapacity));
        }

        int vertexBase() {
            return verts.size() / FLOATS_PER_VERTEX;
        }

        void ensureRoom(int vertexCount, int indexCount) {
            if (vertexBase() + vertexCount > MeshData.MAX_VERTICES
                    || indices.size() + indexCount > 0x1fffffff) {
                flush();
            }
        }

        void addVertex(float x, float y, float z, int tile, float u, float v, float shade) {
            verts.add(x);
            verts.add(y);
            verts.add(z);
            verts.add(tile);
            verts.add(u);
            verts.add(v);
            verts.add(shade);
        }

        void addTriangle(int a, int b, int c) {
            indices.add((short) a);
            indices.add((short) b);
            indices.add((short) c);
        }

        void flush() {
            if (indices.size() == 0) return;
            parts.add(new MeshData.Part(verts.toArray(), indices.toArray()));
            verts.clear();
            indices.clear();
        }

        void reset() {
            verts.clear();
            indices.clear();
            parts.clear();
        }

        MeshData finish() {
            flush();
            MeshData md = new MeshData(parts.toArray(new MeshData.Part[0]));
            parts.clear();
            return md;
        }
    }

    private static class FloatList {
        private float[] data;
        private int size;

        FloatList(int cap) {
            data = new float[cap];
        }

        void add(float f) {
            if (size == data.length) data = Arrays.copyOf(data, size * 2);
            data[size++] = f;
        }

        int size() {
            return size;
        }

        void clear() {
            size = 0;
        }

        float[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }

    private static class ShortList {
        private short[] data;
        private int size;

        ShortList(int cap) {
            data = new short[cap];
        }

        void add(short s) {
            if (size == data.length) data = Arrays.copyOf(data, size * 2);
            data[size++] = s;
        }

        int size() {
            return size;
        }

        void clear() {
            size = 0;
        }

        short[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }
}
