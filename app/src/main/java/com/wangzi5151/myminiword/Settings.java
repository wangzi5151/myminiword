package com.wangzi5151.myminiword;

public class Settings {
    public volatile float sensitivity = 1.0f;
    public volatile int renderDistance = 116;   // blocks (56..176)
    public volatile boolean clouds = true;
    public volatile boolean shadows = true;
    public volatile boolean postFx = true;
    public volatile boolean showStats = true;
    public volatile boolean timeFlowing = true;
    public volatile int timePreset = -1;         // -1 = none, else dayTime 0..1
    public volatile boolean resetRequested = false;

    public static final int MIN_RD = 56;
    public static final int MAX_RD = 176;
}
