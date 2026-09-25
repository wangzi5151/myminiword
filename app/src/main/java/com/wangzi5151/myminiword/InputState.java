package com.wangzi5151.myminiword;

public class InputState {
    public volatile float moveForward = 0f;
    public volatile float moveStrafe = 0f;
    public volatile boolean jump = false;
    public volatile boolean sprint = false;
    public volatile boolean flyToggleRequested = false;

    public volatile float pendingLookDx = 0f;
    public volatile float pendingLookDy = 0f;

    public volatile boolean breakRequested = false;
    public volatile boolean breaking = false;
    public volatile boolean placeRequested = false;
    public volatile boolean cartToggleRequested = false;
    public volatile int cartChoice = 0; // 0 none, 1 stop, 2 continue
    public volatile int selectedBlock = 1;

    public synchronized float consumeLookDx() {
        float v = pendingLookDx;
        pendingLookDx = 0f;
        return v;
    }

    public synchronized float consumeLookDy() {
        float v = pendingLookDy;
        pendingLookDy = 0f;
        return v;
    }

    public synchronized void addLook(float dx, float dy) {
        pendingLookDx += dx;
        pendingLookDy += dy;
    }

    public synchronized boolean consumeBreak() {
        boolean v = breakRequested;
        breakRequested = false;
        return v;
    }

    public synchronized boolean consumePlace() {
        boolean v = placeRequested;
        placeRequested = false;
        return v;
    }

    public synchronized boolean consumeFlyToggle() {
        boolean v = flyToggleRequested;
        flyToggleRequested = false;
        return v;
    }

    public synchronized boolean consumeCartToggle() {
        boolean v = cartToggleRequested;
        cartToggleRequested = false;
        return v;
    }

    public synchronized int consumeCartChoice() {
        int v = cartChoice;
        cartChoice = 0;
        return v;
    }
}
