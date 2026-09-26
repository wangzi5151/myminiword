package com.wangzi5151.myminiword;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.wangzi5151.myminiword.engine.Camera;
import com.wangzi5151.myminiword.engine.ShaderProgram;
import com.wangzi5151.myminiword.engine.TextureAtlas;
import com.wangzi5151.myminiword.player.Player;
import com.wangzi5151.myminiword.world.BlockType;
import com.wangzi5151.myminiword.world.Chunk;
import com.wangzi5151.myminiword.world.ChunkMesh;
import com.wangzi5151.myminiword.world.CityGenerator;
import com.wangzi5151.myminiword.world.MeshData;
import com.wangzi5151.myminiword.world.World;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameRenderer implements GLSurfaceView.Renderer {
    private static final String BLOCK_VS =
            "uniform mat4 uMVP;\n" +
            "uniform vec3 uChunkOffset;\n" +
            "uniform mediump float uTime;\n" +
            "uniform mediump float uWave;\n" +
            "attribute vec3 aPos;\n" +
            "attribute vec3 aTile;\n" +
            "attribute float aShade;\n" +
            "varying mediump vec2 vTileOrigin;\n" +
            "varying mediump vec2 vTileLocal;\n" +
            "varying mediump float vShade;\n" +
            "varying mediump float vDist;\n" +
            "varying mediump vec3 vWorld;\n" +
            "void main(){\n" +
            "  vec3 p = aPos + uChunkOffset;\n" +
            "  if (uWave > 0.5) {\n" +
            "    p.y += sin(p.x * 0.6 + uTime * 1.7) * 0.05 + cos(p.z * 0.55 + uTime * 1.3) * 0.05;\n" +
            "  }\n" +
            "  vWorld = p;\n" +
            "  vec4 cp = uMVP * vec4(p, 1.0);\n" +
            "  gl_Position = cp;\n" +
            "  float t = aTile.x;\n" +
            "  vTileOrigin = vec2(mod(t, 16.0), floor(t / 16.0)) * 0.0625;\n" +
            "  vTileLocal = aTile.yz;\n" +
            "  vShade = aShade;\n" +
            "  vDist = length(cp.xyz);\n" +
            "}\n";

    private static final String BLOCK_FS =
            "precision mediump float;\n" +
            "uniform sampler2D uTex;\n" +
            "uniform vec3 uFogColor;\n" +
            "uniform vec3 uCamPos;\n" +
            "uniform vec3 uSunDir;\n" +
            "uniform vec3 uSunColor;\n" +
            "uniform vec2 uResolution;\n" +
            "uniform float uFogStart;\n" +
            "uniform float uFogEnd;\n" +
            "uniform float uAmbient;\n" +
            "uniform mediump float uTime;\n" +
            "uniform mediump float uWave;\n" +
            "varying mediump vec2 vTileOrigin;\n" +
            "varying mediump vec2 vTileLocal;\n" +
            "varying mediump float vShade;\n" +
            "varying mediump float vDist;\n" +
            "varying mediump vec3 vWorld;\n" +
            "vec3 grade(vec3 col){\n" +
            "  col = max(col, 0.0);\n" +
            "  float l = dot(col, vec3(0.2126, 0.7152, 0.0722));\n" +
            "  col = mix(vec3(l), col, 1.14);\n" +
            "  col = (col - 0.5) * 1.07 + 0.5;\n" +
            "  col = pow(clamp(col, 0.0, 1.0), vec3(0.95));\n" +
            "  return clamp(col, 0.0, 1.0);\n" +
            "}\n" +
            "float vig(){\n" +
            "  vec2 q = gl_FragCoord.xy / max(uResolution, vec2(1.0)) - 0.5;\n" +
            "  return clamp(1.0 - dot(q, q) * 0.45, 0.0, 1.0);\n" +
            "}\n" +
            "void main(){\n" +
            "  vec2 local = fract(vTileLocal);\n" +
            "  vec2 uv = vTileOrigin + 0.001953125 + local * 0.060546875;\n" +
            "  vec4 c = texture2D(uTex, uv);\n" +
            "  if (c.a < 0.1) discard;\n" +
            "  float bright = vShade * uAmbient;\n" +
            "  if (uWave > 0.5) {\n" +
            "    bright *= 1.0 + 0.10 * sin(vWorld.x * 1.3 + uTime * 2.0) * cos(vWorld.z * 1.1 - uTime * 1.6);\n" +
            "  }\n" +
            "  c.rgb *= bright;\n" +
            "  if (uWave > 0.5) {\n" +
            "    float dYdx = cos(vWorld.x * 0.6 + uTime * 1.7) * 0.03;\n" +
            "    float dYdz = -sin(vWorld.z * 0.55 + uTime * 1.3) * 0.0275;\n" +
            "    vec3 N = normalize(vec3(-dYdx, 1.0, -dYdz));\n" +
            "    vec3 V = normalize(uCamPos - vWorld);\n" +
            "    vec3 H = normalize(V + uSunDir);\n" +
            "    float spec = pow(max(dot(N, H), 0.0), 90.0);\n" +
            "    c.rgb += uSunColor * spec * 1.15;\n" +
            "    float fres = pow(1.0 - max(dot(N, V), 0.0), 3.0);\n" +
            "    c.rgb = mix(c.rgb, uFogColor, clamp(fres, 0.0, 1.0) * 0.45);\n" +
            "  }\n" +
            "  float f = clamp((vDist - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);\n" +
            "  c.rgb = mix(c.rgb, uFogColor, f);\n" +
            "  c.rgb = grade(c.rgb) * vig();\n" +
            "  gl_FragColor = c;\n" +
            "}\n";

    private static final String SKY_VS =
            "attribute vec2 aPos;\n" +
            "uniform vec3 uFwd;\n" +
            "uniform vec3 uRight;\n" +
            "uniform vec3 uUp;\n" +
            "uniform float uTan;\n" +
            "uniform float uAspect;\n" +
            "varying mediump vec3 vDir;\n" +
            "void main(){\n" +
            "  vDir = uFwd + uRight * (aPos.x * uTan * uAspect) + uUp * (aPos.y * uTan);\n" +
            "  gl_Position = vec4(aPos, 0.0, 1.0);\n" +
            "}\n";

    private static final String SKY_FS =
            "precision mediump float;\n" +
            "varying mediump vec3 vDir;\n" +
            "uniform vec3 uSunDir;\n" +
            "uniform vec3 uMoonDir;\n" +
            "uniform vec3 uSunColor;\n" +
            "uniform vec3 uTop;\n" +
            "uniform vec3 uBottom;\n" +
            "uniform vec2 uResolution;\n" +
            "uniform float uNight;\n" +
            "uniform float uTime;\n" +
            "float hash(vec3 p){ p = fract(p * 0.3183099 + 0.1); p *= 17.0; return fract(p.x*p.y*p.z*(p.x+p.y+p.z)); }\n" +
            "vec3 grade(vec3 col){\n" +
            "  col = max(col, 0.0);\n" +
            "  float l = dot(col, vec3(0.2126, 0.7152, 0.0722));\n" +
            "  col = mix(vec3(l), col, 1.14);\n" +
            "  col = (col - 0.5) * 1.07 + 0.5;\n" +
            "  col = pow(clamp(col, 0.0, 1.0), vec3(0.95));\n" +
            "  return clamp(col, 0.0, 1.0);\n" +
            "}\n" +
            "float vig(){\n" +
            "  vec2 q = gl_FragCoord.xy / max(uResolution, vec2(1.0)) - 0.5;\n" +
            "  return clamp(1.0 - dot(q, q) * 0.45, 0.0, 1.0);\n" +
            "}\n" +
            "void main(){\n" +
            "  vec3 d = normalize(vDir);\n" +
            "  float h = clamp(d.y, 0.0, 1.0);\n" +
            "  vec3 col = mix(uBottom, uTop, pow(h, 0.55));\n" +
            "  float sd = max(dot(d, uSunDir), 0.0);\n" +
            "  // Broad atmospheric haze around the sun, strongest near the horizon.\n" +
            "  float horizon = pow(1.0 - h, 3.0);\n" +
            "  col += uSunColor * (pow(sd, 6.0) * 0.18 * horizon + pow(sd, 64.0) * 0.20);\n" +
            "  col += uSunColor * (pow(sd, 700.0) * 1.6);\n" +
            "  float md = max(dot(d, uMoonDir), 0.0);\n" +
            "  col += vec3(0.85,0.9,1.0) * (pow(md, 1800.0) * 1.3 + pow(md, 40.0) * 0.05);\n" +
            "  if (uNight > 0.02 && d.y > 0.0) {\n" +
            "    vec3 g = floor(d * 230.0);\n" +
            "    float s = hash(g);\n" +
            "    float star = smoothstep(0.9973, 1.0, s);\n" +
            "    float tw = 0.5 + 0.5 * sin(uTime * 3.0 + s * 100.0);\n" +
            "    col += vec3(star * tw) * uNight * smoothstep(0.0, 0.2, d.y);\n" +
            "  }\n" +
            "  gl_FragColor = vec4(grade(col) * vig(), 1.0);\n" +
            "}\n";

    private static final String CLOUD_VS =
            "attribute vec3 aPos;\n" +
            "attribute vec2 aUV;\n" +
            "uniform mat4 uMVP;\n" +
            "varying mediump vec2 vUV;\n" +
            "varying mediump float vDist;\n" +
            "void main(){ vec4 cp = uMVP * vec4(aPos, 1.0); gl_Position = cp; vUV = aUV; vDist = length(cp.xyz); }\n";

    private static final String CLOUD_FS =
            "precision mediump float;\n" +
            "varying mediump vec2 vUV;\n" +
            "varying mediump float vDist;\n" +
            "uniform sampler2D uCloudTex;\n" +
            "uniform vec3 uFogColor;\n" +
            "uniform vec2 uResolution;\n" +
            "uniform float uFogStart;\n" +
            "uniform float uFogEnd;\n" +
            "uniform float uAlpha;\n" +
            "vec3 grade(vec3 col){\n" +
            "  col = max(col, 0.0);\n" +
            "  float l = dot(col, vec3(0.2126, 0.7152, 0.0722));\n" +
            "  col = mix(vec3(l), col, 1.14);\n" +
            "  col = (col - 0.5) * 1.07 + 0.5;\n" +
            "  col = pow(clamp(col, 0.0, 1.0), vec3(0.95));\n" +
            "  return clamp(col, 0.0, 1.0);\n" +
            "}\n" +
            "float vig(){\n" +
            "  vec2 q = gl_FragCoord.xy / max(uResolution, vec2(1.0)) - 0.5;\n" +
            "  return clamp(1.0 - dot(q, q) * 0.45, 0.0, 1.0);\n" +
            "}\n" +
            "void main(){\n" +
            "  vec4 c = texture2D(uCloudTex, vUV);\n" +
            "  if (c.a < 0.03) discard;\n" +
            "  float f = clamp((vDist - uFogStart) / (uFogEnd - uFogStart), 0.0, 1.0);\n" +
            "  vec3 col = mix(c.rgb, uFogColor, f);\n" +
            "  col = grade(col) * vig();\n" +
            "  gl_FragColor = vec4(col, c.a * uAlpha * (1.0 - f * 0.5));\n" +
            "}\n";

    private static final String LINE_VS =
            "uniform mat4 uMVP;\n" +
            "attribute vec3 aPos;\n" +
            "void main(){ gl_Position = uMVP * vec4(aPos, 1.0); }\n";

    private static final String LINE_FS =
            "precision mediump float;\n" +
            "uniform vec4 uColor;\n" +
            "void main(){ gl_FragColor = uColor; }\n";

    private static final String PART_VS =
            "attribute vec3 aPos;\n" +
            "attribute vec4 aColor;\n" +
            "attribute vec2 aCorner;\n" +
            "uniform mat4 uMVP;\n" +
            "uniform vec3 uCamRight;\n" +
            "uniform vec3 uCamUp;\n" +
            "uniform float uSize;\n" +
            "varying mediump vec4 vColor;\n" +
            "varying mediump vec2 vCorner;\n" +
            "void main(){\n" +
            "  vec3 p = aPos + (uCamRight * aCorner.x + uCamUp * aCorner.y) * uSize;\n" +
            "  gl_Position = uMVP * vec4(p, 1.0);\n" +
            "  vColor = aColor;\n" +
            "  vCorner = aCorner;\n" +
            "}\n";

    private static final String PART_FS =
            "precision mediump float;\n" +
            "varying mediump vec4 vColor;\n" +
            "varying mediump vec2 vCorner;\n" +
            "void main(){\n" +
            "  float d = length(vCorner);\n" +
            "  if (d > 1.0) discard;\n" +
            "  gl_FragColor = vec4(vColor.rgb, vColor.a * (1.0 - d));\n" +
            "}\n";

    private static final float[] PART_COLORS = {
            0, 0, 0,                 // air
            0.34f, 0.60f, 0.20f,     // grass
            0.52f, 0.38f, 0.26f,     // dirt
            0.50f, 0.50f, 0.50f,     // stone
            0.46f, 0.46f, 0.46f,     // cobble
            0.86f, 0.82f, 0.64f,     // sand
            0.40f, 0.30f, 0.19f,     // log
            0.23f, 0.46f, 0.16f,     // leaves
            0.66f, 0.52f, 0.30f,     // planks
            0.18f, 0.38f, 0.78f,     // water
            0.85f, 0.93f, 1.00f,     // glass
            0.23f, 0.23f, 0.23f,     // bedrock
            0.94f, 0.95f, 0.98f,     // snow
            0.59f, 0.27f, 0.21f,     // brick
            0.11f, 0.08f, 0.17f,     // obsidian
            0.49f, 0.48f, 0.47f,     // gravel
            0.29f, 0.29f, 0.31f,     // coal_ore
            0.69f, 0.54f, 0.42f,     // iron_ore
            0.85f, 0.72f, 0.29f,     // gold_ore
            0.36f, 0.88f, 0.89f,     // diamond_ore
            0.87f, 0.82f, 0.65f,     // sandstone
            0.43f, 0.51f, 0.38f,     // mossy
            0.63f, 0.82f, 0.95f,     // ice
            0.64f, 0.67f, 0.71f,     // clay
            0.87f, 0.87f, 0.89f,     // iron_block
            0.97f, 0.85f, 0.36f,     // gold_block
            0.48f, 0.92f, 0.91f,     // diamond_block
            0.54f, 0.42f, 0.23f,     // bookshelf
            0.87f, 0.70f, 0.36f,     // glowstone
            0.87f, 0.86f, 0.82f,     // birch_log
            0.81f, 0.75f, 0.59f,     // birch_planks
            0.54f, 0.54f, 0.56f,     // stone_brick
            0.48f, 0.42f, 0.30f,     // rail_x
            0.48f, 0.42f, 0.30f      // rail_z
    };

    private static final float DAY_LENGTH = 240f;
    private static final int VERTEX_STRIDE = 7 * 4;
    private static final float FIXED_STEP = 1f / 60f;
    private static final int MAX_PARTICLES = 512;
    private static final float CLOUD_Y = 92f;
    private static final float CLOUD_HALF = 220f;
    private static final float CLOUD_SCALE = 60f;

    private final Context context;
    private final InputState input;
    private ControlsOverlay controls;
    private Settings settings;
    private Inventory inventory;

    private World world;
    private Player player;
    private Camera camera;
    private TextureAtlas atlas;
    private ShaderProgram blockShader;
    private ShaderProgram lineShader;
    private ShaderProgram skyShader;
    private ShaderProgram cloudShader;
    private ShaderProgram partShader;

    private int aPos, aTile, aShade;
    private int uMVP, uChunkOffset, uTex, uFogColor, uFogStart, uFogEnd, uAmbient, uTime, uWave;
    private int uCamPos, uSunDir, uSunColor, uResolution;
    private int lPos, lMVP, lColor;
    private int skyPos, skyFwd, skyRight, skyUp, skyTan, skyAspect;
    private int skySunDir, skyMoonDir, skySunColor, skyTop, skyBottom, skyNight, skyTime, skyResolution;
    private int cldPos, cldUV, cldMVP, cldTex, cldFog, cldFogStart, cldFogEnd, cldAlpha, cldResolution;
    private int screenW = 1, screenH = 1;
    private int prPos, prColor, prCorner, prMVP, prRight, prUp, prSize;

    private int lineVbo = -1;
    private int skyVbo = -1;
    private int cloudVbo = -1;
    private int particleVbo = -1;
    private volatile boolean worldReady = false;

    private boolean hasSaved = false;
    private float startX = 0.5f, startY = 40f, startZ = 0.5f;
    private float startYaw = 0f, startPitch = 0f;
    private boolean startFlying = false;
    private int startSelected = 1;
    private int appliedRd = 116;
    private boolean pendingRespawn = false;

    private static final float CART_SPEED = 40f;
    private boolean riding = false;
    private float cartX, cartY, cartZ, cartDirX, cartDirZ;
    private int cartVbo = -1, cartIbo = -1, cartCount = 0;
    private float cartSpeed = CART_SPEED;
    private boolean atStation = false;
    private int lastPassedCity = -1;

    private long lastNanos = 0L;
    private int frames = 0;
    private long fpsTimer = 0L;
    private float dayTime = 0.30f;
    private float renderDist = 116f;
    private float aspect = 1.6f;

    private float accumulator = 0f;
    private float prevX, prevY, prevZ, interpAlpha = 1f;

    private final float[] vp = new float[16];
    private final float[][] planes = new float[6][4];
    private final float[] normalScratch = new float[3];
    private final float[] camFwd = new float[3];
    private final float[] camRight = new float[3];
    private final float[] camUp = new float[3];

    private float skyTopR, skyTopG, skyTopB;
    private float skyBotR, skyBotG, skyBotB;
    private float fogR, fogG, fogB;
    private float sunDirX, sunDirY, sunDirZ;
    private float moonDirX, moonDirY, moonDirZ;
    private float sunColR, sunColG, sunColB;
    private float ambient, nightFactor;
    private float cloudAlpha = 1f;
    private final FloatBuffer highlightBuffer = ByteBuffer.allocateDirect(24 * 3 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final float[] hlVerts = new float[24 * 3];

    private final FloatBuffer cloudBuffer = ByteBuffer.allocateDirect(6 * 5 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final float[] cloudVerts = new float[6 * 5];

    private final FloatBuffer particleBuffer = ByteBuffer.allocateDirect(MAX_PARTICLES * 6 * 9 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final float[] particleVerts = new float[MAX_PARTICLES * 6 * 9];

    private final float[] ppx = new float[MAX_PARTICLES];
    private final float[] ppy = new float[MAX_PARTICLES];
    private final float[] ppz = new float[MAX_PARTICLES];
    private final float[] pvx = new float[MAX_PARTICLES];
    private final float[] pvy = new float[MAX_PARTICLES];
    private final float[] pvz = new float[MAX_PARTICLES];
    private final float[] plife = new float[MAX_PARTICLES];
    private final float[] pmax = new float[MAX_PARTICLES];
    private final float[] pcr = new float[MAX_PARTICLES];
    private final float[] pcg = new float[MAX_PARTICLES];
    private final float[] pcb = new float[MAX_PARTICLES];
    private int particleCursor = 0;
    private final Random random = new Random();

    private static final int MAX_VISIBLE = 2048;
    private final int[] visOpaque = new int[MAX_VISIBLE];
    private final float[] distOpaque = new float[MAX_VISIBLE];
    private final int[] visTransparent = new int[MAX_VISIBLE];
    private final float[] distTransparent = new float[MAX_VISIBLE];
    private final int[] visLiquid = new int[MAX_VISIBLE];
    private final float[] distLiquid = new float[MAX_VISIBLE];
    private Chunk[] renderChunks = new Chunk[0];

    private int breakX = Integer.MIN_VALUE, breakY, breakZ;
    private float breakProgress = 0f;
    private float walkPhase = 0f, bobY = 0f, bobSide = 0f;
    private int heldVbo = -1, heldIbo = -1, heldCount = 0, heldBlockId = -1;
    private final float[] heldModel = new float[16];
    private final float[] heldMvp = new float[16];

    public GameRenderer(Context context, InputState input) {
        this.context = context;
        this.input = input;
    }

    public void setControls(ControlsOverlay controls) {
        this.controls = controls;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void saveWorld() {
        if (world != null) {
            world.save();
        }
        if (player != null) {
            savePlayerState();
        }
    }

    private void loadPlayerState() {
        android.content.SharedPreferences sp = context.getSharedPreferences("myminiword", Context.MODE_PRIVATE);
        hasSaved = sp.getBoolean("has", false);
        startX = sp.getFloat("px", 0.5f);
        startY = sp.getFloat("py", 40f);
        startZ = sp.getFloat("pz", 0.5f);
        startYaw = sp.getFloat("yaw", 0f);
        startPitch = sp.getFloat("pitch", 0f);
        startFlying = sp.getBoolean("flying", false);
        startSelected = sp.getInt("sel", 1);
    }

    private void savePlayerState() {
        android.content.SharedPreferences sp = context.getSharedPreferences("myminiword", Context.MODE_PRIVATE);
        sp.edit()
                .putBoolean("has", true)
                .putFloat("px", player.x)
                .putFloat("py", player.y)
                .putFloat("pz", player.z)
                .putFloat("yaw", player.yaw)
                .putFloat("pitch", player.pitch)
                .putBoolean("flying", player.flying)
                .putInt("sel", input.selectedBlock)
                .apply();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        try {
            setupGL();
        } catch (Throwable t) {
            Diag.setError(t);
        }
    }

    private void setupGL() {
        Diag.setStage("初始化 OpenGL");
        GLES20.glClearColor(0.72f, 0.84f, 0.96f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        atlas = new TextureAtlas();
        Diag.setStage("上传纹理");
        atlas.upload();
        atlas.uploadClouds();
        if (controls != null) {
            android.graphics.Bitmap[] prev = new android.graphics.Bitmap[64];
            for (int id = 1; id <= 38; id++) {
                BlockType bt = BlockType.get(id);
                if (bt == BlockType.AIR) continue;
                int tile = bt.getTile(BlockType.FACE_PX);
                if (tile >= 0) prev[id] = atlas.tileBitmap(tile);
            }
            controls.setPreviews(prev);
        }

        blockShader = new ShaderProgram(BLOCK_VS, BLOCK_FS);
        aPos = blockShader.getAttribute("aPos");
        aTile = blockShader.getAttribute("aTile");
        aShade = blockShader.getAttribute("aShade");
        uMVP = blockShader.getUniform("uMVP");
        uChunkOffset = blockShader.getUniform("uChunkOffset");
        uTex = blockShader.getUniform("uTex");
        uFogColor = blockShader.getUniform("uFogColor");
        uFogStart = blockShader.getUniform("uFogStart");
        uFogEnd = blockShader.getUniform("uFogEnd");
        uAmbient = blockShader.getUniform("uAmbient");
        uTime = blockShader.getUniform("uTime");
        uWave = blockShader.getUniform("uWave");
        uCamPos = blockShader.getUniform("uCamPos");
        uSunDir = blockShader.getUniform("uSunDir");
        uSunColor = blockShader.getUniform("uSunColor");
        uResolution = blockShader.getUniform("uResolution");

        lineShader = new ShaderProgram(LINE_VS, LINE_FS);
        lPos = lineShader.getAttribute("aPos");
        lMVP = lineShader.getUniform("uMVP");
        lColor = lineShader.getUniform("uColor");

        skyShader = new ShaderProgram(SKY_VS, SKY_FS);
        skyPos = skyShader.getAttribute("aPos");
        skyFwd = skyShader.getUniform("uFwd");
        skyRight = skyShader.getUniform("uRight");
        skyUp = skyShader.getUniform("uUp");
        skyTan = skyShader.getUniform("uTan");
        skyAspect = skyShader.getUniform("uAspect");
        skySunDir = skyShader.getUniform("uSunDir");
        skyMoonDir = skyShader.getUniform("uMoonDir");
        skySunColor = skyShader.getUniform("uSunColor");
        skyTop = skyShader.getUniform("uTop");
        skyBottom = skyShader.getUniform("uBottom");
        skyNight = skyShader.getUniform("uNight");
        skyTime = skyShader.getUniform("uTime");
        skyResolution = skyShader.getUniform("uResolution");

        cloudShader = new ShaderProgram(CLOUD_VS, CLOUD_FS);
        cldPos = cloudShader.getAttribute("aPos");
        cldUV = cloudShader.getAttribute("aUV");
        cldMVP = cloudShader.getUniform("uMVP");
        cldTex = cloudShader.getUniform("uCloudTex");
        cldFog = cloudShader.getUniform("uFogColor");
        cldFogStart = cloudShader.getUniform("uFogStart");
        cldFogEnd = cloudShader.getUniform("uFogEnd");
        cldAlpha = cloudShader.getUniform("uAlpha");
        cldResolution = cloudShader.getUniform("uResolution");

        partShader = new ShaderProgram(PART_VS, PART_FS);
        prPos = partShader.getAttribute("aPos");
        prColor = partShader.getAttribute("aColor");
        prCorner = partShader.getAttribute("aCorner");
        prMVP = partShader.getUniform("uMVP");
        prRight = partShader.getUniform("uCamRight");
        prUp = partShader.getUniform("uCamUp");
        prSize = partShader.getUniform("uSize");

        int[] ids = new int[4];
        GLES20.glGenBuffers(4, ids, 0);
        lineVbo = ids[0];
        skyVbo = ids[1];
        cloudVbo = ids[2];
        particleVbo = ids[3];
        uploadSkyQuad();

        camera = new Camera();
        player = new Player();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Diag.setStage("生成世界");
                    loadPlayerState();
                    world = new World(20260925L, new java.io.File(context.getFilesDir(), "world.dat"));
                    int scx, scz;
                    if (hasSaved) {
                        scx = (int) Math.floor(startX) >> 4;
                        scz = (int) Math.floor(startZ) >> 4;
                    } else {
                        scx = 0;
                        scz = 0;
                    }
                    world.setCenter(startX, startZ);
                    long deadline = System.currentTimeMillis() + 20000L;
                    while (!world.isChunkGenerated(scx, scz) && System.currentTimeMillis() < deadline) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    Diag.setStage("角色出生");
                    spawnPlayer();
                    worldReady = true;
                } catch (Throwable t) {
                    Diag.setError(t);
                    worldReady = true;
                }
            }
        }, "world-init").start();
    }

    private void uploadSkyQuad() {
        float[] v = {-1, -1, 1, -1, 1, 1, -1, -1, 1, 1, -1, 1};
        FloatBuffer buf = ByteBuffer.allocateDirect(v.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buf.put(v);
        buf.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, skyVbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, v.length * 4, buf, GLES20.GL_STATIC_DRAW);
    }

    private void spawnPlayer() {
        if (hasSaved) {
            player.x = startX;
            player.y = startY;
            player.z = startZ;
            player.flying = startFlying;
            input.selectedBlock = startSelected;
        } else {
            player.x = 0.5f;
            player.z = 0.5f;
            player.y = world.getSurfaceHeight(0, 0) + 2;
        }
        player.yaw = startYaw;
        player.pitch = startPitch;
        prevX = player.x;
        prevY = player.y;
        prevZ = player.z;
        if (controls != null) controls.flying = player.flying;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        aspect = (float) width / (float) height;
        screenW = Math.max(1, width);
        screenH = Math.max(1, height);
        if (camera != null) camera.setPerspective(74f, aspect, 0.05f, 360f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        if (lastNanos == 0L) lastNanos = now;
        float dt = (now - lastNanos) / 1e9f;
        lastNanos = now;
        if (dt > 0.1f) dt = 0.1f;
        updateFps(now);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (Diag.hasError()) return;
        if (!worldReady || world == null) return;

        if (controls != null) {
            controls.loadProgress = world.getProgress();
            controls.loading = world.getProgress() < 1f;
        }

        if (settings != null && settings.resetRequested) {
            settings.resetRequested = false;
            world.resetWorld();
            hasSaved = false;
            pendingRespawn = true;
        }
        if (pendingRespawn && world.isChunkGenerated(0, 0)) {
            player.x = 0.5f;
            player.z = 0.5f;
            player.y = world.getSurfaceHeight(0, 0) + 2;
            prevX = player.x;
            prevY = player.y;
            prevZ = player.z;
            player.flying = false;
            if (controls != null) controls.flying = false;
            pendingRespawn = false;
        }

        try {
            world.setCenter(player.x, player.z);
            world.processDeletions();
            world.uploadPending();
            updateDayTime(dt);
            handleInput(dt);
            updateCamera();
            if (controls != null) {
                controls.playerX = player.x;
                controls.playerZ = player.z;
                controls.playerYaw = player.yaw;
            }
            extractPlanes(vp);

            drawSky();
            drawWorld();
            drawCart();
            drawParticles(dt);
            drawHighlight();
            drawHeldItem();
        } catch (Throwable t) {
            Diag.setError(t);
        }
    }

    private void updateDayTime(float dt) {
        if (settings != null && !settings.timeFlowing) {
            if (settings.timePreset == 1) dayTime = 0.30f;
            else if (settings.timePreset == 2) dayTime = 0.78f;
            else if (settings.timePreset == 3) dayTime = 0.95f;
        } else {
            dayTime += dt / DAY_LENGTH;
        }
        while (dayTime >= 1f) dayTime -= 1f;

        float ang = (float) ((dayTime - 0.25f) * Math.PI * 2.0);
        sunDirX = (float) Math.cos(ang);
        sunDirY = (float) Math.sin(ang);
        sunDirZ = 0.25f;
        float len = (float) Math.sqrt(sunDirX * sunDirX + sunDirY * sunDirY + sunDirZ * sunDirZ);
        sunDirX /= len; sunDirY /= len; sunDirZ /= len;
        moonDirX = -sunDirX; moonDirY = -sunDirY; moonDirZ = -sunDirZ;

        float day = smoothstep(0f, 0.30f, sunDirY);
        float night = smoothstep(0f, -0.25f, sunDirY);
        float twilight = Math.max(0f, 1f - day - night);

        skyTopR = day * 0.34f + twilight * 0.20f + night * 0.02f;
        skyTopG = day * 0.55f + twilight * 0.24f + night * 0.03f;
        skyTopB = day * 0.90f + twilight * 0.52f + night * 0.10f;

        skyBotR = day * 0.72f + twilight * 0.97f + night * 0.05f;
        skyBotG = day * 0.84f + twilight * 0.55f + night * 0.07f;
        skyBotB = day * 0.96f + twilight * 0.33f + night * 0.16f;

        fogR = skyBotR; fogG = skyBotG; fogB = skyBotB;

        sunColR = 1.0f;
        sunColG = 0.96f * day + 0.55f * twilight + 0.6f * night;
        sunColB = 0.85f * day + 0.30f * twilight + 0.7f * night;

        ambient = 0.30f + 0.70f * day + 0.10f * twilight * (1f - night);
        nightFactor = night;
        cloudAlpha = 0.35f + 0.65f * day;
    }

    private static float smoothstep(float e0, float e1, float x) {
        float t = (x - e0) / (e1 - e0);
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        return t * t * (3 - 2 * t);
    }

    private void updateFps(long now) {
        frames++;
        if (fpsTimer == 0L) fpsTimer = now;
        if (now - fpsTimer >= 1_000_000_000L) {
            int fps = frames;
            frames = 0;
            fpsTimer = now;
            if (controls != null) controls.statFps = fps;
            if (settings != null && settings.renderDistance != appliedRd) {
                appliedRd = settings.renderDistance;
                renderDist = appliedRd;
            }
            int maxRd = settings != null ? settings.renderDistance : 140;
            if (fps < 48) renderDist -= 5f;
            else if (fps > 58) renderDist += 2f;
            if (renderDist < 56f) renderDist = 56f;
            if (renderDist > maxRd) renderDist = maxRd;
            if (world != null) world.setViewRadius(Math.round(renderDist / 16f));
        }
    }

    private void handleInput(float dt) {
        if (input.consumeFlyToggle()) {
            if (riding) {
                riding = false;
                if (controls != null) controls.cartRiding = false;
            } else {
                player.flying = !player.flying;
                if (controls != null) controls.flying = player.flying;
            }
        }
        if (input.consumeCartToggle()) {
            if (riding) {
                riding = false;
                atStation = false;
                if (controls != null) {
                    controls.cartRiding = false;
                    controls.showStopDialog = false;
                }
            } else {
                startRide();
            }
        }
        int choice = input.consumeCartChoice();
        if (choice == 1) {
            riding = false;
            atStation = false;
            if (controls != null) {
                controls.cartRiding = false;
                controls.showStopDialog = false;
            }
        } else if (choice == 2) {
            atStation = false;
            lastPassedCity = nearestCityIndex(cartX, cartZ);
            if (controls != null) controls.showStopDialog = false;
        }

        player.yaw += input.consumeLookDx();
        player.pitch -= input.consumeLookDy();
        if (player.pitch > 89f) player.pitch = 89f;
        if (player.pitch < -89f) player.pitch = -89f;
        while (player.yaw >= 360f) player.yaw -= 360f;
        while (player.yaw < 0f) player.yaw += 360f;

        camera.yaw = player.yaw;
        camera.pitch = player.pitch;
        camera.updateView();

        if (riding) {
            updateCart(dt);
        } else {
            accumulator += dt;
            if (accumulator > 0.25f) accumulator = 0.25f;
            boolean stepped = false;
            while (accumulator >= FIXED_STEP) {
                prevX = player.x;
                prevY = player.y;
                prevZ = player.z;
                player.update(FIXED_STEP, input.moveForward, input.moveStrafe, input.jump, input.sprint, world);
                accumulator -= FIXED_STEP;
                stepped = true;
            }
            if (!stepped) {
                prevX = player.x;
                prevY = player.y;
                prevZ = player.z;
            }
            interpAlpha = accumulator / FIXED_STEP;
        }

        float dx = camera.getDirX();
        float dy = camera.getDirY();
        float dz = camera.getDirZ();

        if (!riding) {
            updateMining(dt, dx, dy, dz);
            if (input.consumePlace()) {
                boolean hit = world.raycast(player.eyeX(), player.eyeY(), player.eyeZ(), dx, dy, dz, 6.5f);
                if (hit) {
                    computeNormal(world.hitFace, normalScratch);
                    int nx = world.hitX + (int) normalScratch[0];
                    int ny = world.hitY + (int) normalScratch[1];
                    int nz = world.hitZ + (int) normalScratch[2];
                    int existing = world.getBlock(nx, ny, nz);
                    if ((existing == 0 || existing == BlockType.WATER.id)
                            && !player.wouldCollideAt(nx, ny, nz)
                            && (inventory == null || inventory.remove(input.selectedBlock, 1))) {
                        world.setBlock(nx, ny, nz, input.selectedBlock);
                        spawnParticles(nx + 0.5f, ny + 0.5f, nz + 0.5f, input.selectedBlock, 8);
                    }
                }
            }
        }

        float hs = (float) Math.sqrt(player.vx * player.vx + player.vz * player.vz);
        if (riding) hs = CART_SPEED;
        if (player.onGround && hs > 0.4f) walkPhase += hs * dt * 2.2f;
        float bobAmp = Math.min(1f, hs / 5f);
        bobY = (float) Math.sin(walkPhase * 2.0) * 0.045f * bobAmp;
        bobSide = (float) Math.cos(walkPhase) * 0.035f * bobAmp;
    }

    private void startRide() {
        int px = (int) Math.floor(player.x);
        int py = (int) Math.floor(player.y);
        int pz = (int) Math.floor(player.z);
        for (int dy = -3; dy <= 1; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int id = world.getBlock(px + dx, py + dy, pz + dz);
                    if (id == BlockType.RAIL_X.id) {
                        beginRide(px + dx + 0.5f, py + dy, pz + dz + 0.5f, camera.getDirX() >= 0 ? 1 : -1, 0);
                        return;
                    }
                    if (id == BlockType.RAIL_Z.id) {
                        beginRide(px + dx + 0.5f, py + dy, pz + dz + 0.5f, 0, camera.getDirZ() >= 0 ? 1 : -1);
                        return;
                    }
                }
            }
        }
        // fallback: jump to the nearest city station on the rail network
        float best = Float.MAX_VALUE;
        int bx = 0, bz = 0;
        for (int[] c : CityGenerator.CITIES) {
            float dd = (c[0] - player.x) * (c[0] - player.x) + (c[1] - player.z) * (c[1] - player.z);
            if (dd < best) {
                best = dd;
                bx = c[0];
                bz = c[1];
            }
        }
        float cx = bx + 0.5f;
        float cz = bz + 0.5f;
        int icx = (int) Math.floor(cx);
        int icz = (int) Math.floor(cz);
        if (findRailY(icx + 1, icz) >= 0) beginRide(cx, World.TRACK_Y, cz, 1, 0);
        else if (findRailY(icx - 1, icz) >= 0) beginRide(cx, World.TRACK_Y, cz, -1, 0);
        else if (findRailY(icx, icz + 1) >= 0) beginRide(cx, World.TRACK_Y, cz, 0, 1);
        else if (findRailY(icx, icz - 1) >= 0) beginRide(cx, World.TRACK_Y, cz, 0, -1);
        else if (Math.abs(camera.getDirX()) >= Math.abs(camera.getDirZ()))
            beginRide(cx, World.TRACK_Y, cz, camera.getDirX() >= 0 ? 1 : -1, 0);
        else beginRide(cx, World.TRACK_Y, cz, 0, camera.getDirZ() >= 0 ? 1 : -1);
    }

    private void beginRide(float x, float y, float z, float dx, float dz) {
        riding = true;
        cartX = x;
        cartY = y;
        cartZ = z;
        cartDirX = dx;
        cartDirZ = dz;
        cartSpeed = CART_SPEED;
        atStation = false;
        lastPassedCity = nearestCityIndex(x, z);
        player.flying = false;
        if (controls != null) {
            controls.cartRiding = true;
            controls.flying = false;
            controls.showStopDialog = false;
        }
    }

    private int nearestCityIndex(float x, float z) {
        int best = -1;
        float bd = 14f * 14f;
        for (int i = 0; i < CityGenerator.CITIES.length; i++) {
            float dx = CityGenerator.CITIES[i][0] - x;
            float dz = CityGenerator.CITIES[i][1] - z;
            float d2 = dx * dx + dz * dz;
            if (d2 < bd) {
                bd = d2;
                best = i;
            }
        }
        return best;
    }

    private int findRailY(int bx, int bz) {
        for (int y = World.TRACK_Y - 3; y <= World.TRACK_Y + 3; y++) {
            int id = world.getBlock(bx, y, bz);
            if (id == BlockType.RAIL_X.id || id == BlockType.RAIL_Z.id) return y;
        }
        return -1;
    }

    private void updateCart(float dt) {
        int near = nearestCityIndex(cartX, cartZ);
        if (near >= 0 && near != lastPassedCity) {
            atStation = true;
            if (controls != null) controls.showStopDialog = true;
        }
        float targetSpeed = atStation ? 0f : CART_SPEED;
        float accel = atStation ? 10f : 8f;
        if (cartSpeed < targetSpeed) cartSpeed = Math.min(targetSpeed, cartSpeed + accel * dt);
        else cartSpeed = Math.max(targetSpeed, cartSpeed - accel * dt);
        if (atStation && cartSpeed < 0.4f) cartSpeed = 0f;

        float remaining = cartSpeed * dt;
        while (remaining > 0f && riding && !atStation) {
            float s = Math.min(0.5f, remaining);
            remaining -= s;
            float nx = cartX + cartDirX * s;
            float nz = cartZ + cartDirZ * s;
            int bx = (int) Math.floor(nx);
            int bz = (int) Math.floor(nz);
            int ry = findRailY(bx, bz);
            if (ry < 0) {
                int curBX = (int) Math.floor(cartX);
                int curBZ = (int) Math.floor(cartZ);
                boolean turned = false;
                if (cartDirX != 0) {
                    if (findRailY(curBX, curBZ + 1) >= 0) {
                        cartDirZ = 1;
                        cartDirX = 0;
                        turned = true;
                    } else if (findRailY(curBX, curBZ - 1) >= 0) {
                        cartDirZ = -1;
                        cartDirX = 0;
                        turned = true;
                    }
                } else {
                    if (findRailY(curBX + 1, curBZ) >= 0) {
                        cartDirX = 1;
                        cartDirZ = 0;
                        turned = true;
                    } else if (findRailY(curBX - 1, curBZ) >= 0) {
                        cartDirX = -1;
                        cartDirZ = 0;
                        turned = true;
                    }
                }
                if (!turned) {
                    riding = false;
                    if (controls != null) controls.cartRiding = false;
                    break;
                }
            } else {
                cartX = nx;
                cartZ = nz;
                cartY = ry;
            }
        }
        player.x = cartX;
        player.y = cartY + 1.0f;
        player.z = cartZ;
        player.vx = 0f;
        player.vy = 0f;
        player.vz = 0f;
        player.onGround = true;
        prevX = player.x;
        prevY = player.y;
        prevZ = player.z;
        interpAlpha = 1f;
    }

    private void updateMining(float dt, float dx, float dy, float dz) {
        if (!input.breaking) {
            breakProgress = 0f;
            breakX = Integer.MIN_VALUE;
            syncBreakUi();
            return;
        }
        boolean hit = world.raycast(player.eyeX(), player.eyeY(), player.eyeZ(), dx, dy, dz, 6.5f);
        if (!hit) {
            breakProgress = 0f;
            breakX = Integer.MIN_VALUE;
            syncBreakUi();
            return;
        }
        int bid = world.getBlock(world.hitX, world.hitY, world.hitZ);
        float hard = BlockType.hardness(bid);
        if (bid == 0 || hard < 0f) {
            breakProgress = 0f;
            breakX = Integer.MIN_VALUE;
            syncBreakUi();
            return;
        }
        if (world.hitX != breakX || world.hitY != breakY || world.hitZ != breakZ) {
            breakX = world.hitX;
            breakY = world.hitY;
            breakZ = world.hitZ;
            breakProgress = 0f;
        }
        breakProgress += dt / Math.max(0.12f, hard * 0.35f);
        if (breakProgress >= 1f) {
            spawnParticles(breakX + 0.5f, breakY + 0.5f, breakZ + 0.5f, bid, 18);
            if (inventory != null) inventory.add(bid, 1);
            world.setBlock(breakX, breakY, breakZ, 0);
            breakProgress = 0f;
            breakX = Integer.MIN_VALUE;
        }
        syncBreakUi();
    }

    private void syncBreakUi() {
        if (controls != null) {
            controls.breakProgress = breakProgress;
            controls.showBreak = input.breaking && breakProgress > 0.01f;
        }
    }

    private void updateCamera() {
        float ix = prevX + (player.x - prevX) * interpAlpha;
        float iy = prevY + (player.y - prevY) * interpAlpha;
        float iz = prevZ + (player.z - prevZ) * interpAlpha;
        camera.x = ix;
        camera.y = iy + Player.EYE;
        camera.z = iz;

        camFwd[0] = camera.getDirX();
        camFwd[1] = camera.getDirY();
        camFwd[2] = camera.getDirZ();
        cross(camFwd[0], camFwd[1], camFwd[2], 0, 1, 0, camRight);
        normalize(camRight);
        cross(camRight[0], camRight[1], camRight[2], camFwd[0], camFwd[1], camFwd[2], camUp);
        normalize(camUp);

        camera.x += camRight[0] * bobSide;
        camera.y += bobY;
        camera.z += camRight[2] * bobSide;

        camera.updateView();
        System.arraycopy(camera.getVP(), 0, vp, 0, 16);
    }

    private static void cross(float ax, float ay, float az, float bx, float by, float bz, float[] out) {
        out[0] = ay * bz - az * by;
        out[1] = az * bx - ax * bz;
        out[2] = ax * by - ay * bx;
    }

    private static void normalize(float[] v) {
        float l = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (l > 1e-6f) {
            v[0] /= l; v[1] /= l; v[2] /= l;
        }
    }

    private void computeNormal(int face, float[] out) {
        switch (face) {
            case BlockType.FACE_PX: out[0]=1; out[1]=0; out[2]=0; break;
            case BlockType.FACE_NX: out[0]=-1; out[1]=0; out[2]=0; break;
            case BlockType.FACE_PY: out[0]=0; out[1]=1; out[2]=0; break;
            case BlockType.FACE_NY: out[0]=0; out[1]=-1; out[2]=0; break;
            case BlockType.FACE_PZ: out[0]=0; out[1]=0; out[2]=1; break;
            default: out[0]=0; out[1]=0; out[2]=-1; break;
        }
    }

    private void drawSky() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);
        skyShader.bind();
        GLES20.glUniform3f(skyFwd, camFwd[0], camFwd[1], camFwd[2]);
        GLES20.glUniform3f(skyRight, camRight[0], camRight[1], camRight[2]);
        GLES20.glUniform3f(skyUp, camUp[0], camUp[1], camUp[2]);
        GLES20.glUniform1f(skyTan, (float) Math.tan(Math.toRadians(74f / 2f)));
        GLES20.glUniform1f(skyAspect, aspect);
        GLES20.glUniform3f(skySunDir, sunDirX, sunDirY, sunDirZ);
        GLES20.glUniform3f(skyMoonDir, moonDirX, moonDirY, moonDirZ);
        GLES20.glUniform3f(skySunColor, sunColR, sunColG, sunColB);
        GLES20.glUniform3f(skyTop, skyTopR, skyTopG, skyTopB);
        GLES20.glUniform3f(skyBottom, skyBotR, skyBotG, skyBotB);
        GLES20.glUniform1f(skyNight, nightFactor);
        GLES20.glUniform1f(skyTime, (float) (System.nanoTime() / 1e9));
        GLES20.glUniform2f(skyResolution, screenW, screenH);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, skyVbo);
        GLES20.glEnableVertexAttribArray(skyPos);
        GLES20.glVertexAttribPointer(skyPos, 2, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        GLES20.glDisableVertexAttribArray(skyPos);
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void drawWorld() {
        int opaqueCount = 0, transCount = 0, liquidCount = 0;
        float px = camera.x, pz = camera.z;
        float maxDist = renderDist + 8f;
        float maxDist2 = maxDist * maxDist;

        renderChunks = world.getRenderChunks();
        for (int i = 0; i < renderChunks.length; i++) {
            Chunk c = renderChunks[i];
            if (!c.generated) continue;
            int total = c.countOpaque + c.countTransparent + c.countLiquid;
            if (total <= 0) continue;
            float vx = c.cx * Chunk.SIZE_X + 8f;
            float vz = c.cz * Chunk.SIZE_Z + 8f;
            float ddx = vx - px, ddz = vz - pz;
            float d2 = ddx * ddx + ddz * ddz;
            if (d2 > maxDist2) continue;
            if (!aabbInFrustum(c.minX, 0, c.minZ, c.maxX, Chunk.SIZE_Y, c.maxZ)) continue;
            if (c.countOpaque > 0) {
                visOpaque[opaqueCount] = i;
                distOpaque[opaqueCount] = d2;
                opaqueCount++;
            }
            if (c.countTransparent > 0) {
                visTransparent[transCount] = i;
                distTransparent[transCount] = d2;
                transCount++;
            }
            if (c.countLiquid > 0) {
                visLiquid[liquidCount] = i;
                distLiquid[liquidCount] = d2;
                liquidCount++;
            }
        }

        float fogStart = Math.max(24f, renderDist - 64f);
        float fogEnd = renderDist;

        setupBlockPass(fogStart, fogEnd);

        int triangles = 0;
        sortFrontToBack(opaqueCount);
        for (int k = 0; k < opaqueCount; k++) {
            triangles += drawChunk(renderChunks[visOpaque[k]], 0);
        }

        if (settings == null || settings.clouds) {
            drawClouds(fogStart, fogEnd);
        }
        setupBlockPass(fogStart, fogEnd);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthMask(false);

        if (liquidCount > 0) {
            sortBackToFront(visLiquid, distLiquid, liquidCount);
            GLES20.glUniform1f(uWave, 1f);
            for (int k = 0; k < liquidCount; k++) {
                triangles += drawChunk(renderChunks[visLiquid[k]], 2);
            }
            GLES20.glUniform1f(uWave, 0f);
        }

        if (transCount > 0) {
            sortBackToFront(visTransparent, distTransparent, transCount);
            for (int k = 0; k < transCount; k++) {
                triangles += drawChunk(renderChunks[visTransparent[k]], 1);
            }
        }

        GLES20.glDepthMask(true);
        GLES20.glDisable(GLES20.GL_BLEND);

        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aTile);
        GLES20.glDisableVertexAttribArray(aShade);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        if (controls != null) {
            controls.statChunks = opaqueCount + transCount + liquidCount;
            controls.statTris = triangles;
        }
    }

    private void setupBlockPass(float fogStart, float fogEnd) {
        blockShader.bind();
        GLES20.glUniformMatrix4fv(uMVP, 1, false, vp, 0);
        GLES20.glUniform1i(uTex, 0);
        GLES20.glUniform3f(uFogColor, fogR, fogG, fogB);
        GLES20.glUniform1f(uFogStart, fogStart);
        GLES20.glUniform1f(uFogEnd, fogEnd);
        GLES20.glUniform1f(uAmbient, ambient);
        GLES20.glUniform1f(uTime, (float) (System.nanoTime() / 1e9));
        GLES20.glUniform1f(uWave, 0f);
        GLES20.glUniform3f(uCamPos, camera.x, camera.y, camera.z);
        GLES20.glUniform3f(uSunDir, sunDirX, sunDirY, sunDirZ);
        GLES20.glUniform3f(uSunColor, sunColR, sunColG, sunColB);
        GLES20.glUniform2f(uResolution, screenW, screenH);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, atlas.getTextureId());
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glEnableVertexAttribArray(aTile);
        GLES20.glEnableVertexAttribArray(aShade);
    }

    private int drawChunk(Chunk c, int pass) {
        int vbo, ibo, count;
        if (pass == 0) {
            vbo = c.vboOpaque; ibo = c.iboOpaque; count = c.countOpaque;
        } else if (pass == 1) {
            vbo = c.vboTransparent; ibo = c.iboTransparent; count = c.countTransparent;
        } else {
            vbo = c.vboLiquid; ibo = c.iboLiquid; count = c.countLiquid;
        }
        if (count <= 0 || vbo == -1 || ibo == -1) return 0;
        GLES20.glUniform3f(uChunkOffset, c.cx * Chunk.SIZE_X, 0f, c.cz * Chunk.SIZE_Z);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, VERTEX_STRIDE, 0);
        GLES20.glVertexAttribPointer(aTile, 3, GLES20.GL_FLOAT, false, VERTEX_STRIDE, 12);
        GLES20.glVertexAttribPointer(aShade, 1, GLES20.GL_FLOAT, false, VERTEX_STRIDE, 24);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, 0);
        return count / 3;
    }

    private void drawClouds(float fogStart, float fogEnd) {
        drawCloudLayer(CLOUD_Y, 1.0f, 0.004f, cloudAlpha, fogStart, fogEnd);
        drawCloudLayer(CLOUD_Y + 9f, 0.72f, 0.0016f, cloudAlpha * 0.55f, fogStart, fogEnd);
    }

    private void drawCloudLayer(float y, float scale, float driftSpeed, float alpha, float fogStart, float fogEnd) {
        float t = (float) (System.nanoTime() / 1e9);
        float drift = t * driftSpeed;
        float cx = camera.x, cz = camera.z;
        float sc = CLOUD_SCALE / scale;
        float u0 = (cx - CLOUD_HALF) / sc + drift;
        float u1 = (cx + CLOUD_HALF) / sc + drift;
        float v0 = (cz - CLOUD_HALF) / sc;
        float v1 = (cz + CLOUD_HALF) / sc;
        float[] v = cloudVerts;
        int p = 0;
        p = cvert(v, p, cx - CLOUD_HALF, y, cz - CLOUD_HALF, u0, v0);
        p = cvert(v, p, cx + CLOUD_HALF, y, cz - CLOUD_HALF, u1, v0);
        p = cvert(v, p, cx + CLOUD_HALF, y, cz + CLOUD_HALF, u1, v1);
        p = cvert(v, p, cx - CLOUD_HALF, y, cz - CLOUD_HALF, u0, v0);
        p = cvert(v, p, cx + CLOUD_HALF, y, cz + CLOUD_HALF, u1, v1);
        cvert(v, p, cx - CLOUD_HALF, y, cz + CLOUD_HALF, u0, v1);

        cloudBuffer.clear();
        cloudBuffer.put(cloudVerts);
        cloudBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, cloudVbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, cloudVerts.length * 4, cloudBuffer, GLES20.GL_DYNAMIC_DRAW);

        cloudShader.bind();
        GLES20.glUniformMatrix4fv(cldMVP, 1, false, vp, 0);
        GLES20.glUniform1i(cldTex, 0);
        GLES20.glUniform3f(cldFog, fogR, fogG, fogB);
        GLES20.glUniform1f(cldFogStart, fogStart);
        GLES20.glUniform1f(cldFogEnd, fogEnd);
        GLES20.glUniform1f(cldAlpha, alpha);
        GLES20.glUniform2f(cldResolution, screenW, screenH);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, atlas.getCloudTextureId());
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthMask(false);
        GLES20.glEnableVertexAttribArray(cldPos);
        GLES20.glEnableVertexAttribArray(cldUV);
        GLES20.glVertexAttribPointer(cldPos, 3, GLES20.GL_FLOAT, false, 5 * 4, 0);
        GLES20.glVertexAttribPointer(cldUV, 2, GLES20.GL_FLOAT, false, 5 * 4, 12);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        GLES20.glDisableVertexAttribArray(cldPos);
        GLES20.glDisableVertexAttribArray(cldUV);
        GLES20.glDepthMask(true);
        GLES20.glDisable(GLES20.GL_BLEND);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, atlas.getTextureId());
    }

    private static int cvert(float[] v, int p, float x, float y, float z, float u, float vv) {
        v[p++] = x; v[p++] = y; v[p++] = z; v[p++] = u; v[p++] = vv;
        return p;
    }

    private void sortFrontToBack(int n) {
        for (int i = 1; i < n; i++) {
            int ci = visOpaque[i];
            float di = distOpaque[i];
            int j = i - 1;
            while (j >= 0 && distOpaque[j] > di) {
                visOpaque[j + 1] = visOpaque[j];
                distOpaque[j + 1] = distOpaque[j];
                j--;
            }
            visOpaque[j + 1] = ci;
            distOpaque[j + 1] = di;
        }
    }

    private void sortBackToFront(int[] idx, float[] dist, int n) {
        for (int i = 1; i < n; i++) {
            int ci = idx[i];
            float di = dist[i];
            int j = i - 1;
            while (j >= 0 && dist[j] < di) {
                idx[j + 1] = idx[j];
                dist[j + 1] = dist[j];
                j--;
            }
            idx[j + 1] = ci;
            dist[j + 1] = di;
        }
    }

    private void spawnParticles(float x, float y, float z, int blockId, int count) {
        if (blockId <= 0 || blockId * 3 + 2 >= PART_COLORS.length) return;
        float cr = PART_COLORS[blockId * 3];
        float cg = PART_COLORS[blockId * 3 + 1];
        float cb = PART_COLORS[blockId * 3 + 2];
        for (int i = 0; i < count; i++) {
            int s = findFreeParticle();
            ppx[s] = x + (random.nextFloat() - 0.5f) * 0.8f;
            ppy[s] = y + (random.nextFloat() - 0.5f) * 0.8f;
            ppz[s] = z + (random.nextFloat() - 0.5f) * 0.8f;
            pvx[s] = (random.nextFloat() - 0.5f) * 3.2f;
            pvy[s] = 1.5f + random.nextFloat() * 3.0f;
            pvz[s] = (random.nextFloat() - 0.5f) * 3.2f;
            plife[s] = 0.5f + random.nextFloat() * 0.6f;
            pmax[s] = plife[s];
            float j = (random.nextFloat() - 0.5f) * 0.15f;
            pcr[s] = clamp01(cr + j);
            pcg[s] = clamp01(cg + j);
            pcb[s] = clamp01(cb + j);
        }
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private int findFreeParticle() {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            int s = (particleCursor + i) % MAX_PARTICLES;
            if (plife[s] <= 0f) {
                particleCursor = (s + 1) % MAX_PARTICLES;
                return s;
            }
        }
        int s = particleCursor;
        particleCursor = (particleCursor + 1) % MAX_PARTICLES;
        return s;
    }

    private void drawParticles(float dt) {
        int p = 0;
        float[] v = particleVerts;
        for (int i = 0; i < MAX_PARTICLES; i++) {
            if (plife[i] <= 0f) continue;
            plife[i] -= dt;
            if (plife[i] <= 0f) continue;
            pvy[i] -= 16f * dt;
            ppy[i] += pvy[i] * dt;
            ppx[i] += pvx[i] * dt;
            ppz[i] += pvz[i] * dt;
            pvx[i] *= (1f - dt * 1.5f);
            pvz[i] *= (1f - dt * 1.5f);
            float a = plife[i] / pmax[i];
            if (a > 1f) a = 1f;
            float r = pcr[i], g = pcg[i], b = pcb[i];
            p = quad(v, p, ppx[i], ppy[i], ppz[i], r, g, b, a);
        }
        if (p == 0) return;

        particleBuffer.clear();
        particleBuffer.put(particleVerts, 0, p);
        particleBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, particleVbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, p * 4, particleBuffer, GLES20.GL_DYNAMIC_DRAW);

        partShader.bind();
        GLES20.glUniformMatrix4fv(prMVP, 1, false, vp, 0);
        GLES20.glUniform3f(prRight, camRight[0], camRight[1], camRight[2]);
        GLES20.glUniform3f(prUp, camUp[0], camUp[1], camUp[2]);
        GLES20.glUniform1f(prSize, 0.09f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthMask(false);
        GLES20.glEnableVertexAttribArray(prPos);
        GLES20.glEnableVertexAttribArray(prColor);
        GLES20.glEnableVertexAttribArray(prCorner);
        GLES20.glVertexAttribPointer(prPos, 3, GLES20.GL_FLOAT, false, 9 * 4, 0);
        GLES20.glVertexAttribPointer(prColor, 4, GLES20.GL_FLOAT, false, 9 * 4, 12);
        GLES20.glVertexAttribPointer(prCorner, 2, GLES20.GL_FLOAT, false, 9 * 4, 28);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, p / 9);
        GLES20.glDisableVertexAttribArray(prPos);
        GLES20.glDisableVertexAttribArray(prColor);
        GLES20.glDisableVertexAttribArray(prCorner);
        GLES20.glDepthMask(true);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    private static final float[] CORNERS = {-1, -1, 1, -1, 1, 1, -1, -1, 1, 1, -1, 1};

    private static int quad(float[] v, int p, float x, float y, float z, float r, float g, float b, float a) {
        for (int c = 0; c < 6; c++) {
            v[p++] = x; v[p++] = y; v[p++] = z;
            v[p++] = r; v[p++] = g; v[p++] = b; v[p++] = a * 0.9f;
            v[p++] = CORNERS[c * 2];
            v[p++] = CORNERS[c * 2 + 1];
        }
        return p;
    }

    private void extractPlanes(float[] m) {
        planes[0][0] = m[3] + m[0]; planes[0][1] = m[7] + m[4]; planes[0][2] = m[11] + m[8]; planes[0][3] = m[15] + m[12];
        planes[1][0] = m[3] - m[0]; planes[1][1] = m[7] - m[4]; planes[1][2] = m[11] - m[8]; planes[1][3] = m[15] - m[12];
        planes[2][0] = m[3] + m[1]; planes[2][1] = m[7] + m[5]; planes[2][2] = m[11] + m[9]; planes[2][3] = m[15] + m[13];
        planes[3][0] = m[3] - m[1]; planes[3][1] = m[7] - m[5]; planes[3][2] = m[11] - m[9]; planes[3][3] = m[15] - m[13];
        planes[4][0] = m[3] + m[2]; planes[4][1] = m[7] + m[6]; planes[4][2] = m[11] + m[10]; planes[4][3] = m[15] + m[14];
        planes[5][0] = m[3] - m[2]; planes[5][1] = m[7] - m[6]; planes[5][2] = m[11] - m[10]; planes[5][3] = m[15] - m[14];
    }

    private boolean aabbInFrustum(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (int i = 0; i < 6; i++) {
            float[] p = planes[i];
            float vx = p[0] > 0 ? maxX : minX;
            float vy = p[1] > 0 ? maxY : minY;
            float vz = p[2] > 0 ? maxZ : minZ;
            if (p[0] * vx + p[1] * vy + p[2] * vz + p[3] < 0) return false;
        }
        return true;
    }

    private void drawHighlight() {
        boolean hit = world.raycast(player.eyeX(), player.eyeY(), player.eyeZ(),
                camera.getDirX(), camera.getDirY(), camera.getDirZ(), 6.5f);
        if (!hit) return;

        float x = world.hitX, y = world.hitY, z = world.hitZ;
        float e = 0.003f;
        float x0 = x - e, y0 = y - e, z0 = z - e;
        float x1 = x + 1 + e, y1 = y + 1 + e, z1 = z + 1 + e;
        float[] v = hlVerts;
        int p = 0;
        p = edge(v, p, x0, y0, z0, x1, y0, z0);
        p = edge(v, p, x1, y0, z0, x1, y0, z1);
        p = edge(v, p, x1, y0, z1, x0, y0, z1);
        p = edge(v, p, x0, y0, z1, x0, y0, z0);
        p = edge(v, p, x0, y1, z0, x1, y1, z0);
        p = edge(v, p, x1, y1, z0, x1, y1, z1);
        p = edge(v, p, x1, y1, z1, x0, y1, z1);
        p = edge(v, p, x0, y1, z1, x0, y1, z0);
        p = edge(v, p, x0, y0, z0, x0, y1, z0);
        p = edge(v, p, x1, y0, z0, x1, y1, z0);
        p = edge(v, p, x1, y0, z1, x1, y1, z1);
        edge(v, p, x0, y0, z1, x0, y1, z1);

        highlightBuffer.clear();
        highlightBuffer.put(hlVerts);
        highlightBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, lineVbo);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, hlVerts.length * 4, highlightBuffer, GLES20.GL_DYNAMIC_DRAW);

        lineShader.bind();
        GLES20.glUniformMatrix4fv(lMVP, 1, false, vp, 0);
        GLES20.glUniform4f(lColor, 0f, 0f, 0f, 0.85f);
        GLES20.glEnableVertexAttribArray(lPos);
        GLES20.glVertexAttribPointer(lPos, 3, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glLineWidth(3f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 24);
        GLES20.glDisableVertexAttribArray(lPos);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    private void drawHeldItem() {
        int blockId = input.selectedBlock;
        if (blockId != heldBlockId) uploadHeldMesh(blockId);
        if (heldCount <= 0 || heldVbo == -1 || heldIbo == -1) return;

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.setIdentityM(heldModel, 0);
        Matrix.translateM(heldModel, 0, 0.60f + bobSide * 1.4f, -0.52f + bobY * 2.2f, -0.95f);
        Matrix.rotateM(heldModel, 0, 16f, 1f, 0f, 0f);
        Matrix.rotateM(heldModel, 0, -34f, 0f, 1f, 0f);
        Matrix.scaleM(heldModel, 0, 0.34f, 0.34f, 0.34f);
        Matrix.multiplyMM(heldMvp, 0, camera.getProjection(), 0, heldModel, 0);

        blockShader.bind();
        GLES20.glUniformMatrix4fv(uMVP, 1, false, heldMvp, 0);
        GLES20.glUniform3f(uChunkOffset, 0f, 0f, 0f);
        GLES20.glUniform1f(uWave, 0f);
        GLES20.glUniform1f(uAmbient, Math.max(ambient, 0.55f));
        GLES20.glUniform1f(uFogStart, 1000f);
        GLES20.glUniform1f(uFogEnd, 2000f);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, atlas.getTextureId());
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glEnableVertexAttribArray(aTile);
        GLES20.glEnableVertexAttribArray(aShade);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, heldVbo);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, VERTEX_STRIDE, 0);
        GLES20.glVertexAttribPointer(aTile, 3, GLES20.GL_FLOAT, false, VERTEX_STRIDE, 12);
        GLES20.glVertexAttribPointer(aShade, 1, GLES20.GL_FLOAT, false, VERTEX_STRIDE, 24);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, heldIbo);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, heldCount, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aTile);
        GLES20.glDisableVertexAttribArray(aShade);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void uploadHeldMesh(int blockId) {
        MeshData md = ChunkMesh.buildBlockMesh(blockId);
        if (heldVbo == -1) {
            int[] ids = new int[1];
            GLES20.glGenBuffers(1, ids, 0);
            heldVbo = ids[0];
        }
        if (heldIbo == -1) {
            int[] ids = new int[1];
            GLES20.glGenBuffers(1, ids, 0);
            heldIbo = ids[0];
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, heldVbo);
        FloatBuffer vb = ByteBuffer.allocateDirect(md.vertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vb.put(md.vertices);
        vb.position(0);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, md.vertices.length * 4, vb, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, heldIbo);
        ShortBuffer ib = ByteBuffer.allocateDirect(md.indices.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        ib.put(md.indices);
        ib.position(0);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, md.indices.length * 2, ib, GLES20.GL_STATIC_DRAW);
        heldCount = md.indices.length;
        heldBlockId = blockId;
    }

    private void drawCart() {
        if (!riding) return;
        if (cartVbo == -1) uploadCartMesh();
        if (cartCount <= 0 || cartVbo == -1 || cartIbo == -1) return;

        blockShader.bind();
        GLES20.glUniformMatrix4fv(uMVP, 1, false, vp, 0);
        GLES20.glUniform3f(uChunkOffset, cartX, cartY, cartZ);
        GLES20.glUniform1f(uWave, 0f);
        GLES20.glUniform1f(uAmbient, ambient);
        GLES20.glUniform3f(uFogColor, fogR, fogG, fogB);
        GLES20.glUniform1f(uFogStart, Math.max(24f, renderDist - 64f));
        GLES20.glUniform1f(uFogEnd, renderDist);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, atlas.getTextureId());
        GLES20.glEnableVertexAttribArray(aPos);
        GLES20.glEnableVertexAttribArray(aTile);
        GLES20.glEnableVertexAttribArray(aShade);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, cartVbo);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, VERTEX_STRIDE, 0);
        GLES20.glVertexAttribPointer(aTile, 3, GLES20.GL_FLOAT, false, VERTEX_STRIDE, 12);
        GLES20.glVertexAttribPointer(aShade, 1, GLES20.GL_FLOAT, false, VERTEX_STRIDE, 24);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, cartIbo);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, cartCount, GLES20.GL_UNSIGNED_SHORT, 0);
        GLES20.glDisableVertexAttribArray(aPos);
        GLES20.glDisableVertexAttribArray(aTile);
        GLES20.glDisableVertexAttribArray(aShade);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void uploadCartMesh() {
        MeshData md = ChunkMesh.buildBlockMesh(BlockType.IRON_BLOCK.id);
        int[] ids = new int[1];
        GLES20.glGenBuffers(1, ids, 0);
        cartVbo = ids[0];
        GLES20.glGenBuffers(1, ids, 0);
        cartIbo = ids[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, cartVbo);
        FloatBuffer vb = ByteBuffer.allocateDirect(md.vertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vb.put(md.vertices);
        vb.position(0);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, md.vertices.length * 4, vb, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, cartIbo);
        ShortBuffer ib = ByteBuffer.allocateDirect(md.indices.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        ib.put(md.indices);
        ib.position(0);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, md.indices.length * 2, ib, GLES20.GL_STATIC_DRAW);
        cartCount = md.indices.length;
    }

    private static int edge(float[] v, int p, float ax, float ay, float az, float bx, float by, float bz) {
        v[p++] = ax; v[p++] = ay; v[p++] = az;
        v[p++] = bx; v[p++] = by; v[p++] = bz;
        return p;
    }
}
