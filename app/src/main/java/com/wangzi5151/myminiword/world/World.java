package com.wangzi5151.myminiword.world;

import android.opengl.GLES20;

import com.wangzi5151.myminiword.Diag;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class World {
    public static final int DEFAULT_VIEW_RADIUS = 7;
    public static final int TRACK_Y = 46;
    private static final int MIN_RADIUS = 4;
    private static final int MAX_RADIUS = 12;
    private static final int WORKERS = 3;
    private static final int UPLOAD_BUDGET = 3;

    private final TerrainGenerator generator;
    private final CityGenerator cityGenerator;
    private final ConcurrentHashMap<Long, Chunk> chunks = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<long[]> genQueue = new ConcurrentLinkedQueue<>();
    private final java.util.Set<Long> genSet = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<long[]> meshQueue = new ConcurrentLinkedQueue<>();
    private final java.util.Set<Long> meshSet = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Chunk> deleteQueue = new ConcurrentLinkedQueue<>();

    private volatile Chunk[] snapshot = new Chunk[0];
    private volatile boolean snapshotDirty = true;

    private volatile int viewRadius = DEFAULT_VIEW_RADIUS;
    private volatile int centerCX = Integer.MIN_VALUE;
    private volatile int centerCZ = Integer.MIN_VALUE;

    private final AtomicInteger generatedCount = new AtomicInteger(0);
    private final AtomicInteger initialGoal = new AtomicInteger(1);
    private volatile boolean spawnReady = false;
    private volatile boolean running = true;

    private final java.io.File saveFile;
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Integer, Byte>> chunkEdits = new ConcurrentHashMap<>();
    private static final int SAVE_MAGIC = 0x5348_4145; // "SHAE"

    public World(final long seed) {
        this(seed, null);
    }

    public World(final long seed, java.io.File saveFile) {
        this.saveFile = saveFile;
        generator = new TerrainGenerator(seed);
        cityGenerator = new CityGenerator(generator);
        loadEdits();
        initialGoal.set(countInRadius(0, 0, DEFAULT_VIEW_RADIUS));
        setCenter(0, 0);
        for (int w = 0; w < WORKERS; w++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    workerLoop();
                }
            }, "chunk-worker-" + w);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            t.start();
        }
    }

    private void workerLoop() {
        long[] t;
        while (running) {
            try {
                if ((t = genQueue.poll()) != null) {
                    genSet.remove(key((int) t[0], (int) t[1]));
                    generateChunk((int) t[0], (int) t[1]);
                    continue;
                }
                if ((t = meshQueue.poll()) != null) {
                    meshSet.remove(key((int) t[0], (int) t[1]));
                    Chunk c = chunks.get(key((int) t[0], (int) t[1]));
                    if (c != null && c.generated) {
                        ChunkMesh.build(World.this, c);
                    }
                    continue;
                }
                Thread.sleep(4);
            } catch (Throwable e) {
                Diag.setError(e);
            }
        }
    }

    private void generateChunk(int cx, int cz) {
        int cdx = cx - centerCX, cdz = cz - centerCZ;
        int limit = viewRadius + 2;
        if (cdx * cdx + cdz * cdz > limit * limit) return;
        long k = key(cx, cz);
        Chunk c = chunks.get(k);
        if (c == null) {
            c = new Chunk(cx, cz);
            Chunk prev = chunks.putIfAbsent(k, c);
            if (prev != null) c = prev;
        }
        if (c.generated) return;
        generator.generate(c);
        cityGenerator.populate(c, cx * Chunk.SIZE_X, cz * Chunk.SIZE_Z);
        cityGenerator.rails(c, cx * Chunk.SIZE_X, cz * Chunk.SIZE_Z);
        applyEdits(cx, cz, c);
        c.generated = true;
        generatedCount.incrementAndGet();
        snapshotDirty = true;
        if (cx == 0 && cz == 0) spawnReady = true;
        enqueueMesh(cx, cz);
        enqueueMesh(cx - 1, cz);
        enqueueMesh(cx + 1, cz);
        enqueueMesh(cx, cz - 1);
        enqueueMesh(cx, cz + 1);
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xffffffffL);
    }

    private static int countInRadius(int ccx, int ccz, int r) {
        int n = 0;
        int r2 = r * r;
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                if (dx * dx + dz * dz <= r2) n++;
        return n;
    }

    public void setViewRadius(int r) {
        if (r < MIN_RADIUS) r = MIN_RADIUS;
        if (r > MAX_RADIUS) r = MAX_RADIUS;
        if (r == viewRadius) return;
        viewRadius = r;
        int oldCX = centerCX, oldCZ = centerCZ;
        centerCX = Integer.MIN_VALUE;
        setCenter(oldCX, oldCZ);
    }

    public void setCenter(float worldX, float worldZ) {
        setCenter((int) Math.floor(worldX) >> 4, (int) Math.floor(worldZ) >> 4);
    }

    private void setCenter(int ccx, int ccz) {
        if (ccx == centerCX && ccz == centerCZ) return;
        centerCX = ccx;
        centerCZ = ccz;
        int r = viewRadius;
        int r2 = r * r;
        int unloadR = r + 3;
        int unloadR2 = unloadR * unloadR;

        for (int ring = 0; ring <= r; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    if (dx * dx + dz * dz > r2) continue;
                    int cx = ccx + dx, cz = ccz + dz;
                    long k = key(cx, cz);
                    if (!chunks.containsKey(k) && genSet.add(k)) {
                        genQueue.add(new long[]{cx, cz});
                    }
                }
            }
        }

        java.util.Iterator<java.util.Map.Entry<Long, Chunk>> it = chunks.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<Long, Chunk> e = it.next();
            Chunk c = e.getValue();
            int dx = c.cx - ccx, dz = c.cz - ccz;
            if (dx * dx + dz * dz > unloadR2) {
                it.remove();
                snapshotDirty = true;
                deleteQueue.add(c);
            }
        }
        snapshotDirty = true;
    }

    private void enqueueMesh(int cx, int cz) {
        long k = key(cx, cz);
        if (chunks.containsKey(k) && meshSet.add(k)) {
            meshQueue.add(new long[]{cx, cz});
        }
    }

    public boolean isSpawnReady() {
        return spawnReady;
    }

    public boolean isChunkGenerated(int cx, int cz) {
        Chunk c = chunks.get(key(cx, cz));
        return c != null && c.generated;
    }

    public void resetWorld() {
        chunkEdits.clear();
        if (saveFile != null && saveFile.exists()) saveFile.delete();
        for (Chunk c : chunks.values()) {
            c.generated = false;
            if (genSet.add(key(c.cx, c.cz))) {
                genQueue.add(new long[]{c.cx, c.cz});
            }
        }
        generatedCount.set(0);
        spawnReady = false;
        snapshotDirty = true;
    }

    public float getProgress() {
        int goal = initialGoal.get();
        if (goal <= 0) return 1f;
        return Math.min(1f, (float) generatedCount.get() / goal);
    }

    public int getLoadedCount() {
        return chunks.size();
    }

    public Chunk[] getRenderChunks() {
        if (snapshotDirty) {
            List<Chunk> list = new ArrayList<>(chunks.values());
            snapshot = list.toArray(new Chunk[0]);
            snapshotDirty = false;
        }
        return snapshot;
    }

    public int getBlock(int x, int y, int z) {
        if (y < 0 || y >= Chunk.SIZE_Y) return 0;
        Chunk c = chunks.get(key(x >> 4, z >> 4));
        if (c == null || !c.generated) return 0;
        return c.get(x & 15, y, z & 15);
    }

    public void setBlock(int x, int y, int z, int id) {
        if (y < 0 || y >= Chunk.SIZE_Y) return;
        int cx = x >> 4, cz = z >> 4;
        Chunk c = chunks.get(key(cx, cz));
        if (c == null || !c.generated) return;
        c.set(x & 15, y, z & 15, (byte) id);
        recordEdit(cx, cz, Chunk.index(x & 15, y, z & 15), (byte) id);
        enqueueMesh(cx, cz);
        enqueueMesh(cx - 1, cz);
        enqueueMesh(cx + 1, cz);
        enqueueMesh(cx, cz - 1);
        enqueueMesh(cx, cz + 1);
    }

    // ---- GL thread operations ----

    public void processDeletions() {
        Chunk c;
        while ((c = deleteQueue.poll()) != null) {
            if (c.vboOpaque != -1) GLES20.glDeleteBuffers(1, new int[]{c.vboOpaque}, 0);
            if (c.iboOpaque != -1) GLES20.glDeleteBuffers(1, new int[]{c.iboOpaque}, 0);
            if (c.vboTransparent != -1) GLES20.glDeleteBuffers(1, new int[]{c.vboTransparent}, 0);
            if (c.iboTransparent != -1) GLES20.glDeleteBuffers(1, new int[]{c.iboTransparent}, 0);
            if (c.vboLiquid != -1) GLES20.glDeleteBuffers(1, new int[]{c.vboLiquid}, 0);
            if (c.iboLiquid != -1) GLES20.glDeleteBuffers(1, new int[]{c.iboLiquid}, 0);
        }
    }

    public void uploadPending() {
        int budget = UPLOAD_BUDGET * 2;
        Chunk[] list = getRenderChunks();
        for (int i = 0; i < list.length && budget > 0; i++) {
            Chunk c = list[i];
            MeshData om = c.opaqueMesh.get();
            if (om != null) {
                c.vboOpaque = uploadVertices(c.vboOpaque, om.vertices);
                c.iboOpaque = uploadIndices(c.iboOpaque, om.indices);
                c.countOpaque = om.indices.length;
                c.opaqueMesh.compareAndSet(om, null);
                budget--;
            }
            MeshData tm = c.transparentMesh.get();
            if (tm != null && budget > 0) {
                c.vboTransparent = uploadVertices(c.vboTransparent, tm.vertices);
                c.iboTransparent = uploadIndices(c.iboTransparent, tm.indices);
                c.countTransparent = tm.indices.length;
                c.transparentMesh.compareAndSet(tm, null);
                budget--;
            }
            MeshData lm = c.liquidMesh.get();
            if (lm != null && budget > 0) {
                c.vboLiquid = uploadVertices(c.vboLiquid, lm.vertices);
                c.iboLiquid = uploadIndices(c.iboLiquid, lm.indices);
                c.countLiquid = lm.indices.length;
                c.liquidMesh.compareAndSet(lm, null);
                budget--;
            }
        }
    }

    private int uploadVertices(int vbo, float[] data) {
        if (vbo == -1) {
            int[] ids = new int[1];
            GLES20.glGenBuffers(1, ids, 0);
            vbo = ids[0];
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buf = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buf.put(data);
        buf.position(0);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.length * 4, buf, GLES20.GL_STATIC_DRAW);
        return vbo;
    }

    private int uploadIndices(int ibo, short[] data) {
        if (ibo == -1) {
            int[] ids = new int[1];
            GLES20.glGenBuffers(1, ids, 0);
            ibo = ids[0];
        }
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo);
        ShortBuffer buf = ByteBuffer.allocateDirect(data.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        buf.put(data);
        buf.position(0);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, data.length * 2, buf, GLES20.GL_STATIC_DRAW);
        return ibo;
    }

    private void recordEdit(int cx, int cz, int localIndex, byte id) {
        ConcurrentHashMap<Integer, Byte> m = chunkEdits.get(key(cx, cz));
        if (m == null) {
            m = new ConcurrentHashMap<>();
            ConcurrentHashMap<Integer, Byte> prev = chunkEdits.putIfAbsent(key(cx, cz), m);
            if (prev != null) m = prev;
        }
        m.put(localIndex, id);
    }

    private void applyEdits(int cx, int cz, Chunk c) {
        ConcurrentHashMap<Integer, Byte> m = chunkEdits.get(key(cx, cz));
        if (m == null || m.isEmpty()) return;
        for (java.util.Map.Entry<Integer, Byte> e : m.entrySet()) {
            int idx = e.getKey();
            if (idx >= 0 && idx < c.blocks.length) c.blocks[idx] = e.getValue();
        }
    }

    private void loadEdits() {
        if (saveFile == null || !saveFile.exists()) return;
        java.io.DataInputStream in = null;
        try {
            in = new java.io.DataInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(saveFile)));
            if (in.readInt() != SAVE_MAGIC) return;
            int nChunks = in.readInt();
            for (int i = 0; i < nChunks; i++) {
                long ck = in.readLong();
                int n = in.readInt();
                ConcurrentHashMap<Integer, Byte> m = new ConcurrentHashMap<>();
                for (int j = 0; j < n; j++) {
                    int idx = in.readInt();
                    byte id = in.readByte();
                    m.put(idx, id);
                }
                chunkEdits.put(ck, m);
            }
        } catch (Throwable e) {
            Diag.setError(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (java.io.IOException ignored) {
                }
            }
        }
    }

    public void save() {
        if (saveFile == null || chunkEdits.isEmpty()) return;
        final java.util.ArrayList<Object[]> snap = new java.util.ArrayList<>(chunkEdits.size());
        for (java.util.Map.Entry<Long, ConcurrentHashMap<Integer, Byte>> e : chunkEdits.entrySet()) {
            ConcurrentHashMap<Integer, Byte> m = e.getValue();
            java.util.ArrayList<int[]> entries = new java.util.ArrayList<>(m.size());
            for (java.util.Map.Entry<Integer, Byte> be : m.entrySet()) {
                entries.add(new int[]{be.getKey(), be.getValue()});
            }
            snap.add(new Object[]{e.getKey(), entries});
        }
        Thread t = new Thread(new Runnable() {
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                java.io.DataOutputStream out = null;
                try {
                    java.io.File tmp = new java.io.File(saveFile.getAbsolutePath() + ".tmp");
                    out = new java.io.DataOutputStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(tmp)));
                    out.writeInt(SAVE_MAGIC);
                    out.writeInt(snap.size());
                    for (Object[] cc : snap) {
                        out.writeLong((Long) cc[0]);
                        java.util.ArrayList<int[]> entries = (java.util.ArrayList<int[]>) cc[1];
                        out.writeInt(entries.size());
                        for (int[] en : entries) {
                            out.writeInt(en[0]);
                            out.writeByte(en[1]);
                        }
                    }
                    out.flush();
                    out.close();
                    out = null;
                    tmp.renameTo(saveFile);
                } catch (Throwable e) {
                    Diag.setError(e);
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }, "world-save");
        t.start();
    }

    public void shutdown() {
        running = false;
    }

    // ---- Raycast ----
    public int hitX, hitY, hitZ, hitFace;
    public boolean hasHit;

    public boolean raycast(float ox, float oy, float oz, float dx, float dy, float dz, float maxDist) {
        hasHit = false;
        int x = (int) Math.floor(ox);
        int y = (int) Math.floor(oy);
        int z = (int) Math.floor(oz);

        int stepX = dx > 0 ? 1 : -1;
        int stepY = dy > 0 ? 1 : -1;
        int stepZ = dz > 0 ? 1 : -1;

        float tDeltaX = dx == 0 ? Float.MAX_VALUE : Math.abs(1f / dx);
        float tDeltaY = dy == 0 ? Float.MAX_VALUE : Math.abs(1f / dy);
        float tDeltaZ = dz == 0 ? Float.MAX_VALUE : Math.abs(1f / dz);

        float tMaxX = dx == 0 ? Float.MAX_VALUE : (dx > 0 ? (x + 1 - ox) : (ox - x)) * tDeltaX;
        float tMaxY = dy == 0 ? Float.MAX_VALUE : (dy > 0 ? (y + 1 - oy) : (oy - y)) * tDeltaY;
        float tMaxZ = dz == 0 ? Float.MAX_VALUE : (dz > 0 ? (z + 1 - oz) : (oz - z)) * tDeltaZ;

        int face = -1;
        float t = 0f;

        while (t <= maxDist) {
            int id = getBlock(x, y, z);
            if (id != 0 && BlockType.isSolid(id)) {
                hitX = x;
                hitY = y;
                hitZ = z;
                hitFace = face;
                hasHit = true;
                return true;
            }
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX;
                t = tMaxX;
                tMaxX += tDeltaX;
                face = stepX > 0 ? BlockType.FACE_NX : BlockType.FACE_PX;
            } else if (tMaxY < tMaxZ) {
                y += stepY;
                t = tMaxY;
                tMaxY += tDeltaY;
                face = stepY > 0 ? BlockType.FACE_NY : BlockType.FACE_PY;
            } else {
                z += stepZ;
                t = tMaxZ;
                tMaxZ += tDeltaZ;
                face = stepZ > 0 ? BlockType.FACE_NZ : BlockType.FACE_PZ;
            }
        }
        return false;
    }

    public int cityGroundHeight(int index) {
        int[] c = CityGenerator.CITIES[index];
        int t = generator.surfaceHeight(c[0], c[1]);
        if (t < TerrainGenerator.SEA_LEVEL + 2) t = TerrainGenerator.SEA_LEVEL + 2;
        if (t > 38) t = 38;
        return t;
    }

    public int getSurfaceHeight(int x, int z) {
        for (int y = Chunk.SIZE_Y - 1; y >= 0; y--) {
            int id = getBlock(x, y, z);
            if (id != 0 && id != BlockType.WATER.id) return y;
        }
        return 1;
    }
}
