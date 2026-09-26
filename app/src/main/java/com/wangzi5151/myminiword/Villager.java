package com.wangzi5151.myminiword;

public class Villager {
    public float x, y, z;
    public float yaw;
    public float tx, tz;
    public float walkPhase;
    public float retarget;
    public int cityIndex;

    public Villager(float x, float y, float z, int cityIndex) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.cityIndex = cityIndex;
        this.tx = x;
        this.tz = z;
        this.retarget = 0f;
    }
}
