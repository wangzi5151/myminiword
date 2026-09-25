package com.wangzi5151.myminiword.engine;

import android.opengl.Matrix;

public class Camera {
    public float x, y, z;
    public float yaw;
    public float pitch;

    private final float[] view = new float[16];
    private final float[] projection = new float[16];
    private final float[] vp = new float[16];

    public Camera() {
        Matrix.setIdentityM(view, 0);
        Matrix.setIdentityM(projection, 0);
    }

    public void setPerspective(float fovDeg, float aspect, float near, float far) {
        Matrix.perspectiveM(projection, 0, fovDeg, aspect, near, far);
    }

    public void updateView() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float dx = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        float dy = (float) Math.sin(pitchRad);
        float dz = (float) (-Math.cos(yawRad) * Math.cos(pitchRad));
        Matrix.setLookAtM(view, 0,
                x, y, z,
                x + dx, y + dy, z + dz,
                0f, 1f, 0f);
    }

    public float[] getVP() {
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0);
        return vp;
    }

    public float[] getView() {
        return view;
    }

    public float[] getProjection() {
        return projection;
    }

    public float getDirX() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        return (float) (Math.sin(yawRad) * Math.cos(pitchRad));
    }

    public float getDirY() {
        return (float) Math.sin(Math.toRadians(pitch));
    }

    public float getDirZ() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        return (float) (-Math.cos(yawRad) * Math.cos(pitchRad));
    }
}
