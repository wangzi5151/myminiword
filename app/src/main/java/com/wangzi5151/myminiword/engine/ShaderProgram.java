package com.wangzi5151.myminiword.engine;

import android.opengl.GLES20;

public class ShaderProgram {
    private final int programId;
    private final int vertexShaderId;
    private final int fragmentShaderId;

    public ShaderProgram(String vertexSource, String fragmentSource) {
        vertexShaderId = compile(GLES20.GL_VERTEX_SHADER, vertexSource);
        fragmentShaderId = compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        programId = GLES20.glCreateProgram();
        GLES20.glAttachShader(programId, vertexShaderId);
        GLES20.glAttachShader(programId, fragmentShaderId);
        GLES20.glLinkProgram(programId);
        int[] status = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(programId);
            GLES20.glDeleteProgram(programId);
            throw new RuntimeException("Error linking program: " + log);
        }
    }

    private static int compile(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Error compiling shader: " + log);
        }
        return shader;
    }

    public void bind() {
        GLES20.glUseProgram(programId);
    }

    public int getAttribute(String name) {
        return GLES20.glGetAttribLocation(programId, name);
    }

    public int getUniform(String name) {
        return GLES20.glGetUniformLocation(programId, name);
    }

    public void dispose() {
        GLES20.glDeleteProgram(programId);
        GLES20.glDeleteShader(vertexShaderId);
        GLES20.glDeleteShader(fragmentShaderId);
    }
}
