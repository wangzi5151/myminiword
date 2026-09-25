package com.wangzi5151.shahe.player;

import com.wangzi5151.shahe.world.Chunk;
import com.wangzi5151.shahe.world.World;

public class Player {
    public float x, y, z;
    public float vx, vy, vz;
    public float yaw = 0f;
    public float pitch = 0f;
    public boolean onGround = false;
    public boolean flying = false;

    public static final float WIDTH = 0.6f;
    public static final float HEIGHT = 1.8f;
    public static final float EYE = 1.62f;

    private static final float WALK_SPEED = 4.6f;
    private static final float SPRINT_SPEED = 7.2f;
    private static final float FLY_SPEED = 11f;
    private static final float GRAVITY = 26f;
    private static final float JUMP_VELOCITY = 8.6f;
    private static final float HALF = WIDTH / 2f;

    public void update(float dt, float moveForward, float moveStrafe, boolean jump, boolean sprint, World world) {
        float speed = flying ? FLY_SPEED : (sprint ? SPRINT_SPEED : WALK_SPEED);

        float yawRad = (float) Math.toRadians(yaw);
        float fx = (float) Math.sin(yawRad);
        float fz = (float) -Math.cos(yawRad);
        float rx = (float) Math.cos(yawRad);
        float rz = (float) Math.sin(yawRad);

        float len = (float) Math.sqrt(moveForward * moveForward + moveStrafe * moveStrafe);
        if (len > 1f) {
            moveForward /= len;
            moveStrafe /= len;
        }

        vx = (fx * moveForward + rx * moveStrafe) * speed;
        vz = (fz * moveForward + rz * moveStrafe) * speed;

        if (flying) {
            vy = 0f;
            if (jump) vy = FLY_SPEED;
        } else {
            vy -= GRAVITY * dt;
            if (jump && onGround) {
                vy = JUMP_VELOCITY;
                onGround = false;
            }
        }
        if (vy < -50f) vy = -50f;

        moveHorizontal(vx * dt, vz * dt, world);
        moveAxis(0, vy * dt, 0, world);
    }

    private void moveHorizontal(float dx, float dz, World world) {
        boolean steppedOnce = false;
        if (dx != 0f) {
            x += dx;
            if (collides(world)) {
                boolean stepped = false;
                if (onGround && !steppedOnce) {
                    y += 1.0f;
                    if (!collides(world)) {
                        stepped = true;
                        steppedOnce = true;
                    } else {
                        y -= 1.0f;
                    }
                }
                if (!stepped) x -= dx;
            }
        }
        if (dz != 0f) {
            z += dz;
            if (collides(world)) {
                boolean stepped = false;
                if (onGround && !steppedOnce) {
                    y += 1.0f;
                    if (!collides(world)) {
                        stepped = true;
                    } else {
                        y -= 1.0f;
                    }
                }
                if (!stepped) z -= dz;
            }
        }
    }

    private void moveAxis(float dx, float dy, float dz, World world) {
        x += dx;
        y += dy;
        z += dz;
        if (collides(world)) {
            x -= dx;
            y -= dy;
            z -= dz;
            if (dy != 0) {
                if (dy < 0) onGround = true;
                vy = 0;
            }
        } else if (dy < 0) {
            onGround = false;
        }
    }

    public boolean collides(World world) {
        float minX = x - HALF;
        float maxX = x + HALF;
        float minY = y;
        float maxY = y + HEIGHT;
        float minZ = z - HALF;
        float maxZ = z + HALF;

        int x0 = (int) Math.floor(minX);
        int x1 = (int) Math.floor(maxX);
        int y0 = (int) Math.floor(minY);
        int y1 = (int) Math.floor(maxY);
        int z0 = (int) Math.floor(minZ);
        int z1 = (int) Math.floor(maxZ);

        for (int bx = x0; bx <= x1; bx++) {
            for (int by = y0; by <= y1; by++) {
                for (int bz = z0; bz <= z1; bz++) {
                    int id = world.getBlock(bx, by, bz);
                    if (id == 0) continue;
                    if (!com.wangzi5151.shahe.world.BlockType.isSolid(id)) continue;
                    if (maxX > bx && minX < bx + 1 &&
                            maxY > by && minY < by + 1 &&
                            maxZ > bz && minZ < bz + 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean wouldCollideAt(int bx, int by, int bz) {
        float minX = x - HALF, maxX = x + HALF;
        float minY = y, maxY = y + HEIGHT;
        float minZ = z - HALF, maxZ = z + HALF;
        return maxX > bx && minX < bx + 1 &&
                maxY > by && minY < by + 1 &&
                maxZ > bz && minZ < bz + 1;
    }

    public float eyeX() {
        return x;
    }

    public float eyeY() {
        return y + EYE;
    }

    public float eyeZ() {
        return z;
    }
}
