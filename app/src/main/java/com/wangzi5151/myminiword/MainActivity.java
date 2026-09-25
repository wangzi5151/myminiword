package com.wangzi5151.myminiword;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private GLSurfaceView glView;
    private GameRenderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            buildUi();
        } catch (Throwable t) {
            Diag.setError(t);
            showFatal();
        }
    }

    private void buildUi() {
        InputState input = new InputState();
        Settings settings = new Settings();
        Inventory inventory = new Inventory();

        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        renderer = new GameRenderer(getApplicationContext(), input);
        renderer.setSettings(settings);
        renderer.setInventory(inventory);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        ControlsOverlay controls = new ControlsOverlay(this, input, settings, inventory);
        renderer.setControls(controls);

        FrameLayout root = new FrameLayout(this);
        root.addView(glView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(controls, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        setImmersive();
    }

    private void showFatal() {
        TextView tv = new TextView(this);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(Color.rgb(140, 20, 20));
        tv.setTextSize(12f);
        tv.setPadding(24, 24, 24, 24);
        tv.setText("启动失败:\n\n" + Diag.getError());
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        setContentView(sv);
    }

    private void setImmersive() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) setImmersive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
    }

    @Override
    protected void onPause() {
        if (renderer != null) renderer.saveWorld();
        super.onPause();
        glView.onPause();
    }
}
