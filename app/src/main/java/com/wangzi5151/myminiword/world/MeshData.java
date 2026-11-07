package com.wangzi5151.myminiword.world;

public final class MeshData {
    public static final int MAX_VERTICES = 65536;

    public static final class Part {
        public final float[] vertices;
        public final short[] indices;

        public Part(float[] vertices, short[] indices) {
            this.vertices = vertices;
            this.indices = indices;
        }
    }

    public final Part[] parts;

    public MeshData(float[] vertices, short[] indices) {
        this(new Part[]{new Part(vertices, indices)});
    }

    public MeshData(Part[] parts) {
        this.parts = parts;
    }

    public int indexCount() {
        int n = 0;
        for (Part p : parts) n += p.indices.length;
        return n;
    }
}
