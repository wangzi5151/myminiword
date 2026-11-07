package com.wangzi5151.myminiword.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

import org.junit.Test;

public class WorldTest {
    @Test
    public void raycastHitsSolidBlockAndReportsFace() {
        World world = new World(123L, null, false);
        Chunk chunk = new Chunk(0, 0);
        chunk.set(5, 10, 5, (byte) BlockType.STONE.id);
        chunk.generated = true;
        world.putChunkForTest(chunk);

        assertTrue(world.raycast(5.5f, 15f, 5.5f, 0f, -1f, 0f, 10f));
        assertEquals(5, world.hitX);
        assertEquals(10, world.hitY);
        assertEquals(5, world.hitZ);
        assertEquals(BlockType.FACE_PY, world.hitFace);

        assertFalse(world.raycast(5.5f, 15f, 5.5f, 1f, 0f, 0f, 3f));
        assertFalse(world.raycast(5.5f, 15f, 5.5f, 0f, -1f, 0f, 2f));
        world.shutdown();
    }

    @Test
    public void raycastIgnoresNonSolidBlocks() {
        World world = new World(1L, null, false);
        Chunk chunk = new Chunk(0, 0);
        chunk.set(2, 4, 2, (byte) BlockType.WATER.id);
        chunk.set(2, 2, 2, (byte) BlockType.STONE.id);
        chunk.generated = true;
        world.putChunkForTest(chunk);

        assertTrue(world.raycast(2.5f, 10f, 2.5f, 0f, -1f, 0f, 10f));
        assertEquals(2, world.hitY);
        assertEquals(BlockType.WATER.id, world.getBlock(2, 4, 2));
        assertTrue(BlockType.isSolid(world.getBlock(2, world.hitY, 2)));
        world.shutdown();
    }

    @Test
    public void saveWaitsAndRoundTripsEdits() throws Exception {
        File file = File.createTempFile("myminiword-test", ".dat");
        assertTrue(file.delete());

        World world = new World(7L, file, false);
        Chunk chunk = new Chunk(0, 0);
        chunk.generated = true;
        world.putChunkForTest(chunk);
        world.setBlock(3, 5, 4, BlockType.GLOWSTONE.id);
        assertTrue(world.saveAndWait(5000L));
        assertTrue(file.isFile());
        assertTrue(file.length() > 0);

        DataInputStream in = new DataInputStream(new FileInputStream(file));
        try {
            assertEquals(0x53484145, in.readInt());
            assertEquals(1, in.readInt());
            in.readLong();
            int count = in.readInt();
            assertTrue(count >= 1);
        } finally {
            in.close();
        }
        world.shutdown();

        World loaded = new World(7L, file, false);
        Chunk loadedChunk = new Chunk(0, 0);
        loadedChunk.generated = true;
        loaded.putChunkForTest(loadedChunk);
        assertEquals(BlockType.GLOWSTONE.id, loaded.getBlock(3, 5, 4));
        assertEquals(0, loaded.getBlock(4, 5, 4));
        loaded.shutdown();
        assertTrue(file.delete());
    }

    @Test
    public void saveWithoutEditsCompletesImmediately() {
        File file = new File(System.getProperty("java.io.tmpdir"),
                "myminiword-empty-" + System.nanoTime() + ".dat");
        World world = new World(9L, file, false);
        assertTrue(world.saveAndWait(1000L));
        assertFalse(file.exists());
        world.shutdown();
    }

    @Test
    public void editIsAppliedWhenChunkIsInjected() {
        World world = new World(11L, null, false);
        Chunk chunk = new Chunk(0, 0);
        chunk.generated = true;
        world.putChunkForTest(chunk);
        world.setBlock(1, 2, 3, BlockType.BRICK.id);
        Chunk again = new Chunk(0, 0);
        again.generated = true;
        world.putChunkForTest(again);
        assertEquals(BlockType.BRICK.id, world.getBlock(1, 2, 3));
        assertNotNull(again.blocks);
        world.shutdown();
    }
}
