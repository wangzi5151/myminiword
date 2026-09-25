package com.wangzi5151.shahe;

public final class Diag {
    private static volatile String error = null;
    private static volatile String stage = "";

    private Diag() {
    }

    public static void setStage(String s) {
        stage = s;
    }

    public static String getStage() {
        return stage;
    }

    public static void setError(Throwable t) {
        if (error != null) return;
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getSimpleName()).append(": ");
        sb.append(String.valueOf(t.getMessage()));
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length && i < 6; i++) {
            sb.append("\n").append(st[i].toString());
        }
        error = sb.toString();
    }

    public static void setErrorText(String s) {
        if (error == null) error = s;
    }

    public static boolean hasError() {
        return error != null;
    }

    public static String getError() {
        return error;
    }
}
