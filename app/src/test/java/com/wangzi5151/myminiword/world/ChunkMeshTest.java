package com.wangzi5151.myminiword.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.wangzi5151.myminiword.engine.TextureAtlas;

import org.junit.Test;

public class ChunkMeshTest {
    private static final int FLOATS_PER_VERTEX = 7;

    @Test
    public void largeTransparentMeshIsSplitIntoSafeParts() {
        World world = new World(0L, null, false);
        Chunk chunk = new Chunk(0, 0);
        for (int y = 0; y < Chunk.SIZE_Y; y++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                for (int x = 0; x < Chunk.SIZE_X; x++) {
                    if (((x + y + z) & 1) == 0) {
                        chunk.set(x, y, z, (byte) BlockType.GLASS.id);
                    }
                }
            }
        }
        chunk.generated = true;
        world.putChunkForTest(chunk);

        ChunkMesh.build(world, chunk);
        MeshData md = chunk.transparentMesh.get();
        assertNotNull(md);
        assertTrue(md.parts.length > 1);

        int totalVertices = 0;
        for (MeshData.Part part : md.parts) {
            assertEquals(0, part.vertices.length % FLOATS_PER_VERTEX);
            int vertexCount = part.vertices.length / FLOATS_PER_VERTEX;
            assertTrue(vertexCount <= MeshData.MAX_VERTICES);
            assertEquals(0, part.indices.length % 6);
            assertEquals(vertexCount * 6 / 4, part.indices.length);
            for (short s : part.indices) {
                int index = s & 0xffff;
                assertTrue(index < vertexCount);
            }
            totalVertices += vertexCount;
        }
        assertEquals(196608, totalVertices);
        assertEquals(3, md.parts.length);
        assertEquals(294912, md.indexCount());
        world.shutdown();
    }

    @Test
    public void smallBlockMeshStaysInSinglePart() {
        MeshData md = ChunkMesh.buildBlockMesh(BlockType.STONE.id);
        assertEquals(1, md.parts.length);
        MeshData.Part part = md.parts[0];
        assertEquals(24, part.vertices.length / FLOATS_PER_VERTEX);
        assertEquals(36, part.indices.length);
        for (short s : part.indices) {
            assertTrue((s & 0xffff) < 24);
        }
        int tile = (int) part.vertices[3];
        assertEquals(BlockType.get(BlockType.STONE.id).getTile(0), tile);
    }

    @Test
    public void cartAndVillagerMeshesAreValid() {
        MeshData cart = ChunkMesh.buildCartMesh();
        assertEquals(1, cart.parts.length);
        int cartVerts = cart.parts[0].vertices.length / FLOATS_PER_VERTEX;
        assertEquals(0, cartVerts % 4);
        assertEquals(cartVerts * 6 / 4, cart.parts[0].indices.length);
        assertTrue(cartVerts > 0);

        MeshData villager = ChunkMesh.buildVillagerMesh();
        assertEquals(1, villager.parts.length);
        int villagerVerts = villager.parts[0].vertices.length / FLOATS_PER_VERTEX;
        assertEquals(0, villagerVerts % 4);
        assertEquals(villagerVerts * 6 / 4, villager.parts[0].indices.length);
        assertTrue(villagerVerts > 0);
    }

    @Test
    public void railTileConstantsRemainStable() {
        assertEquals(33, TextureAtlas.RAIL_X);
        assertEquals(34, TextureAtlas.RAIL_Z);
    }
}
