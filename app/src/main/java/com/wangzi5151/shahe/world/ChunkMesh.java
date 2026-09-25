package com.wangzi5151.shahe.world;

import com.wangzi5151.shahe.engine.TextureAtlas;

import java.util.Arrays;

public class ChunkMesh {

    private static final float[] FACE_SHADE = {0.80f, 0.80f, 1.00f, 0.55f, 0.68f, 0.68f};
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
        return id != 0 && BlockType.get(id).opaque;
    }

    public static void build(World world, Chunk chunk) {
        byte[] pad = new byte[PAD_SIZE];
        int baseX = chunk.cx * SX;
        int baseZ = chunk.cz * SZ;
        for (int y = -1; y <= SY; y++) {
            for (int z = -1; z <= SZ; z++) {
                for (int x = -1; x <= SX; x++) {
                    pad[pidx(x, y, z)] = (byte) world.getBlock(baseX + x, y, baseZ + z);
                }
            }
        }

        FloatList ov = new FloatList(8192);
        ShortList oi = new ShortList(6144);
        FloatList tv = new FloatList(2048);
        ShortList ti = new ShortList(1536);
        FloatList lv = new FloatList(2048);
        ShortList li = new ShortList(1536);

        greedyOpaque(pad, ov, oi);
        perFaceNonOpaque(pad, tv, ti, lv, li);
        emitRails(pad, tv, ti);

        chunk.opaqueMesh.set(new MeshData(ov.toArray(), oi.toArray()));
        chunk.transparentMesh.set(new MeshData(tv.toArray(), ti.toArray()));
        chunk.liquidMesh.set(new MeshData(lv.toArray(), li.toArray()));
    }

    // ---- Greedy meshing for opaque blocks ----
    private static void greedyOpaque(byte[] pad, FloatList verts, ShortList indices) {
        int[] dims = {SX, SY, SZ};
        int[] x = new int[3];
        int[] q = new int[3];
        int[] pos = new int[3];

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
            int[] mask = new int[mw * mh];

            for (x[d] = -1; x[d] < dims[d]; ) {
                int n = 0;
                for (x[v] = 0; x[v] < mh; x[v]++) {
                    for (x[u] = 0; x[u] < mw; x[u]++) {
                        int a = padGet(pad, x[0] + q[0], x[1] + q[1], x[2] + q[2]);
                        int b = padGet(pad, x[0], x[1], x[2]);
                        boolean af = opaqueId(a);
                        boolean bf = opaqueId(b);
                        int m = 0;
                        if (af && !bf) {
                            if (x[d] + 1 >= 0 && x[d] + 1 < dims[d]) m = a;      // +d, owned by us
                        } else if (bf && !af) {
                            if (x[d] >= 0 && x[d] < dims[d]) m = -b;             // -d, owned by us
                        }
                        mask[n++] = m;
                    }
                }
                x[d]++;

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
                        pos[0] = pos[1] = pos[2] = 0;
                        pos[d] = x[d];
                        emitGreedy(verts, indices, pad, d, u, v, pos, i, j, w, h, c);
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

    private static void emitGreedy(FloatList out, ShortList indices, byte[] pad,
                                   int d, int u, int v, int[] pos, int i, int j, int w, int h, int c) {
        int sign = c > 0 ? 1 : -1;
        int block = c > 0 ? c : -c;
        int face = faceOf(d, sign);
        int tile = BlockType.get(block).getTile(face);
        float baseShade = FACE_SHADE[face];
        boolean vFlip = (d != 1);

        int[] cu = {i, i + w, i + w, i};
        int[] cv = {j, j, j + h, j + h};
        int[] su = {-1, 1, 1, -1};
        int[] sv = {-1, -1, 1, 1};

        int base = out.size() / FLOATS_PER_VERTEX;
        for (int k = 0; k < 4; k++) {
            pos[u] = cu[k];
            pos[v] = cv[k];

            int iu = su[k] < 0 ? i : i + w - 1;
            int jv = sv[k] < 0 ? j : j + h - 1;
            int bd = sign > 0 ? pos[d] : pos[d] - 1;

            int s1d = bd + sign, s1u = iu + su[k], s1v = jv;
            int s2d = bd + sign, s2u = iu, s2v = jv + sv[k];
            int cRd = bd + sign, cRu = iu + su[k], cRv = jv + sv[k];

            int[] a = {0, 0, 0};
            a[d] = s1d; a[u] = s1u; a[v] = s1v;
            int occ1 = opaqueId(padGet(pad, a[0], a[1], a[2])) ? 1 : 0;
            a[d] = s2d; a[u] = s2u; a[v] = s2v;
            int occ2 = opaqueId(padGet(pad, a[0], a[1], a[2])) ? 1 : 0;
            a[d] = cRd; a[u] = cRu; a[v] = cRv;
            int occC = opaqueId(padGet(pad, a[0], a[1], a[2])) ? 1 : 0;

            int ao = (occ1 == 1 && occ2 == 1) ? 0 : 3 - (occ1 + occ2 + occC);
            float aoFactor = 0.55f + (ao / 3f) * 0.45f;

            out.add(pos[0]);
            out.add(pos[1]);
            out.add(pos[2]);
            out.add(tile);
            out.add(cu[k] - i);
            out.add(vFlip ? (j + h) - cv[k] : cv[k] - j);
            out.add(baseShade * aoFactor);
        }
        indices.add((short) base);
        indices.add((short) (base + 1));
        indices.add((short) (base + 2));
        indices.add((short) base);
        indices.add((short) (base + 2));
        indices.add((short) (base + 3));
    }

    // ---- Per-face meshing for glass / water / other non-opaque blocks ----
    private static void perFaceNonOpaque(byte[] pad, FloatList tv, ShortList ti, FloatList lv, ShortList li) {
        for (int y = 0; y < SY; y++) {
            for (int z = 0; z < SZ; z++) {
                for (int x = 0; x < SX; x++) {
                    int id = padGet(pad, x, y, z);
                    if (id == 0) continue;
                    if (id == BlockType.RAIL_X.id || id == BlockType.RAIL_Z.id) continue;
                    BlockType bt = BlockType.get(id);
                    if (bt.opaque) continue;
                    FloatList verts = bt.liquid ? lv : tv;
                    ShortList indices = bt.liquid ? li : ti;
                    for (int face = 0; face < 6; face++) {
                        int nid = padGet(pad, x + FACE_NORMALS[face][0],
                                y + FACE_NORMALS[face][1],
                                z + FACE_NORMALS[face][2]);
                        if (!shouldRenderFace(id, nid)) continue;
                        addFace(verts, indices, x, y, z, face, bt.getTile(face));
                    }
                }
            }
        }
    }

    private static void emitRails(byte[] pad, FloatList out, ShortList indices) {
        int railX = BlockType.RAIL_X.id;
        int railZ = BlockType.RAIL_Z.id;
        for (int y = 0; y < SY; y++) {
            for (int z = 0; z < SZ; z++) {
                for (int x = 0; x < SX; x++) {
                    int id = padGet(pad, x, y, z);
                    if (id != railX && id != railZ) continue;
                    int tile = id == railX ? TextureAtlas.RAIL_X : TextureAtlas.RAIL_Z;
                    float yy = y + 0.0625f;
                    int base = out.size() / FLOATS_PER_VERTEX;
                    addVert(out, x, yy, z, tile, 0, 0, 0.95f);
                    addVert(out, x + 1, yy, z, tile, 1, 0, 0.95f);
                    addVert(out, x + 1, yy, z + 1, tile, 1, 1, 0.95f);
                    addVert(out, x, yy, z + 1, tile, 0, 1, 0.95f);
                    indices.add((short) base);
                    indices.add((short) (base + 1));
                    indices.add((short) (base + 2));
                    indices.add((short) base);
                    indices.add((short) (base + 2));
                    indices.add((short) (base + 3));
                }
            }
        }
    }

    private static void addVert(FloatList out, float x, float y, float z, int tile, float u, float v, float shade) {
        out.add(x);
        out.add(y);
        out.add(z);
        out.add(tile);
        out.add(u);
        out.add(v);
        out.add(shade);
    }

    private static boolean shouldRenderFace(int current, int neighbor) {
        if (neighbor == 0) return true;
        BlockType nb = BlockType.get(neighbor);
        if (nb.opaque) return false;
        if (current == neighbor) return false;
        return true;
    }

    private static void addFace(FloatList out, ShortList indices, int x, int y, int z, int face, int tile) {
        float[] verts = FACE_VERTS[face];
        float shade = FACE_SHADE[face];
        int base = out.size() / FLOATS_PER_VERTEX;
        for (int c = 0; c < 4; c++) {
            int o = c * 5;
            out.add(x + verts[o]);
            out.add(y + verts[o + 1]);
            out.add(z + verts[o + 2]);
            out.add(tile);
            out.add(verts[o + 3]);
            out.add(verts[o + 4]);
            out.add(shade);
        }
        indices.add((short) base);
        indices.add((short) (base + 1));
        indices.add((short) (base + 2));
        indices.add((short) base);
        indices.add((short) (base + 2));
        indices.add((short) (base + 3));
    }

    public static MeshData buildBlockMesh(int blockId) {
        BlockType bt = BlockType.get(blockId);
        FloatList out = new FloatList(6 * 4 * FLOATS_PER_VERTEX);
        ShortList idx = new ShortList(6 * 6);
        for (int face = 0; face < 6; face++) {
            float[] verts = FACE_VERTS[face];
            float shade = FACE_SHADE[face];
            int tile = bt.getTile(face);
            int base = out.size() / FLOATS_PER_VERTEX;
            for (int c = 0; c < 4; c++) {
                int o = c * 5;
                out.add(verts[o]);
                out.add(verts[o + 1]);
                out.add(verts[o + 2]);
                out.add(tile);
                out.add(verts[o + 3]);
                out.add(verts[o + 4]);
                out.add(shade);
            }
            idx.add((short) base);
            idx.add((short) (base + 1));
            idx.add((short) (base + 2));
            idx.add((short) base);
            idx.add((short) (base + 2));
            idx.add((short) (base + 3));
        }
        return new MeshData(out.toArray(), idx.toArray());
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

        short[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }
}
