package com.wangzi5151.shahe.world;

import java.util.concurrent.atomic.AtomicReference;

public class Chunk {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 64;
    public static final int SIZE_Z = 16;

    public final int cx;
    public final int cz;
    public final byte[] blocks = new byte[SIZE_X * SIZE_Y * SIZE_Z];
    public volatile boolean generated = false;

    // pending CPU mesh data (atomically published by worker, consumed by GL thread)
    public final AtomicReference<MeshData> opaqueMesh = new AtomicReference<>();
    public final AtomicReference<MeshData> transparentMesh = new AtomicReference<>();
    public final AtomicReference<MeshData> liquidMesh = new AtomicReference<>();

    // GL objects
    public int vboOpaque = -1;
    public int iboOpaque = -1;
    public int countOpaque = 0;
    public int vboTransparent = -1;
    public int iboTransparent = -1;
    public int countTransparent = 0;
    public int vboLiquid = -1;
    public int iboLiquid = -1;
    public int countLiquid = 0;

    // culling bounds (world space)
    public float minX, maxX, minZ, maxZ;

    public Chunk(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
        minX = cx * SIZE_X;
        maxX = minX + SIZE_X;
        minZ = cz * SIZE_Z;
        maxZ = minZ + SIZE_Z;
    }

    public static int index(int x, int y, int z) {
        return (y * SIZE_Z + z) * SIZE_X + x;
    }

    public byte get(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= SIZE_X || y >= SIZE_Y || z >= SIZE_Z) return 0;
        return blocks[index(x, y, z)];
    }

    public void set(int x, int y, int z, byte id) {
        if (x < 0 || y < 0 || z < 0 || x >= SIZE_X || y >= SIZE_Y || z >= SIZE_Z) return;
        blocks[index(x, y, z)] = id;
    }
}
