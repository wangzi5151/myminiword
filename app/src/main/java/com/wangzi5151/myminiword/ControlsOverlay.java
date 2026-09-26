package com.wangzi5151.myminiword;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

import com.wangzi5151.myminiword.world.BlockType;
import com.wangzi5151.myminiword.world.CityGenerator;

public class ControlsOverlay extends View {
    private static final int ROLE_LOOK = 1;
    private static final int ROLE_JOY = 2;
    private static final int ROLE_JUMP = 3;
    private static final int ROLE_BREAK = 4;
    private static final int ROLE_PLACE = 5;
    private static final int ROLE_FLY = 6;

    private final InputState input;
    private final SparseIntArray roles = new SparseIntArray();

    private float w, h;
    private float joyBaseX, joyBaseY, joyRadius;
    private float joyKnobX, joyKnobY;

    private final RectF jumpRect = new RectF();
    private final RectF breakRect = new RectF();
    private final RectF placeRect = new RectF();
    private final RectF flyRect = new RectF();
    private final RectF cartRect = new RectF();

    private float hotbarSlot, hotbarY, hotbarX0;

    private int lookPointer = -1;
    private float lastLookX, lastLookY;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmpRect = new RectF();

    public volatile boolean flying = false;
    public volatile boolean cartRiding = false;
    public volatile boolean showStopDialog = false;
    public volatile float playerX = 0f, playerZ = 0f, playerYaw = 0f;
    public volatile int statFps = 0;
    public volatile int statChunks = 0;
    public volatile int statTris = 0;
    public volatile float loadProgress = 0f;
    public volatile boolean loading = true;
    public volatile float breakProgress = 0f;
    public volatile boolean showBreak = false;

    private static final int[] BLOCK_COLORS = {
            0x00000000, 0xFF5E9D34, 0xFF866043, 0xFF808082, 0xFF747476, 0xFFDED2A5,
            0xFF684E30, 0xFF3A7628, 0xFFAA854E, 0xFF2E62C8, 0xFFD8EEFF, 0xFF3A3A3C,
            0xFFF2F4FA, 0xFF964436, 0xFF1C142C, 0xFF7E7A78, 0xFF4A4A4E, 0xFFB08A6A,
            0xFFD8B84A, 0xFF5CE0E2, 0xFFDED2A6, 0xFF6E8260, 0xFFA0D2F2, 0xFFA2AAB4,
            0xFFDEDEE2, 0xFFF8D85C, 0xFF7AEAE8, 0xFF8A6A3A, 0xFFEEC65C, 0xFFDEDCD2,
            0xFFCEBE96, 0xFF8A8A8E, 0xFF7A6A4A, 0xFF7A6A4A,
            0xFFE6E8EA, 0xFFA83630, 0xFFE2C454, 0xFF466EAA, 0xFF3A3228
    };
    private static final int HOTBAR_COUNT = 16;

    private final Settings settings;
    private final Inventory inventory;
    private android.graphics.Bitmap[] previews;
    private final Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private boolean settingsOpen = false;
    private boolean invOpen = false;
    private final RectF invRect = new RectF();
    private final RectF gearRect = new RectF();
    private final RectF panelRect = new RectF();
    private final RectF rdMinus = new RectF();
    private final RectF rdPlus = new RectF();
    private final RectF senMinus = new RectF();
    private final RectF senPlus = new RectF();
    private final RectF cloudRect = new RectF();
    private final RectF statsRect = new RectF();
    private final RectF flowRect = new RectF();
    private final RectF shadowRect = new RectF();
    private final RectF postRect = new RectF();
    private final RectF dayRect = new RectF();
    private final RectF duskRect = new RectF();
    private final RectF nightRect = new RectF();
    private final RectF resetRect = new RectF();
    private final RectF closeRect = new RectF();

    private final RectF invPanel = new RectF();
    private final RectF invCloseRect = new RectF();
    private final RectF stopPanelRect = new RectF();
    private final RectF stopBtnRect = new RectF();
    private final RectF contBtnRect = new RectF();
    private float mapSize;
    private static float mapMinX, mapMaxX, mapMinZ, mapMaxZ;
    private float invPad;
    private float invTitleY;
    private int invCols = 7;
    private int invRows = 6;
    private float invSlot;
    private float invGridX;
    private float invGridY;
    private float craftX0, craftX1, craftRowH, craftY0;

    public ControlsOverlay(Context context, InputState input, Settings settings, Inventory inventory) {
        super(context);
        this.input = input;
        this.settings = settings;
        this.inventory = inventory;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(2));
        stroke.setColor(0xCCFFFFFF);
        text.setColor(Color.WHITE);
        text.setTextSize(dp(14));
        text.setTextAlign(Paint.Align.CENTER);
        text.setFakeBoldText(true);
        panel.setColor(0x66000000);
        setFocusable(false);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public void setPreviews(android.graphics.Bitmap[] p) {
        this.previews = p;
        invalidate();
    }

    private void drawSlotSwatch(Canvas canvas, RectF r, int id, int cnt) {
        fill.setColor(0x55000000);
        canvas.drawRoundRect(r, dp(5), dp(5), fill);
        Bitmap bm = (previews != null && id < previews.length) ? previews[id] : null;
        if (bm != null) {
            bmpPaint.setAlpha(cnt > 0 ? 255 : 90);
            canvas.drawBitmap(bm, null, r, bmpPaint);
            bmpPaint.setAlpha(255);
        } else {
            fill.setColor(id < BLOCK_COLORS.length ? BLOCK_COLORS[id] : 0xFF888888);
            fill.setAlpha(cnt > 0 ? 255 : 90);
            canvas.drawRoundRect(r, dp(5), dp(5), fill);
            fill.setAlpha(255);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        w = width;
        h = height;
        joyRadius = h * 0.17f;
        joyBaseX = h * 0.21f;
        joyBaseY = h * 0.71f;
        joyKnobX = joyBaseX;
        joyKnobY = joyBaseY;

        float bs = h * 0.13f;
        float margin = h * 0.05f;
        float jumpSize = bs * 1.15f;
        float jumpRight = w - margin;
        float jumpBottom = h - margin;
        jumpRect.set(jumpRight - jumpSize, jumpBottom - jumpSize, jumpRight, jumpBottom);

        float gap = h * 0.035f;
        placeRect.set(jumpRect.left - gap - bs, jumpBottom - bs, jumpRect.left - gap, jumpBottom);
        breakRect.set(placeRect.left - gap - bs, jumpBottom - bs, placeRect.left - gap, jumpBottom);
        float flySize = bs * 0.9f;
        flyRect.set(jumpRight - flySize, jumpRect.top - gap - flySize, jumpRight, jumpRect.top - gap);
        cartRect.set(flyRect.left - gap - bs, flyRect.top, flyRect.left - gap, flyRect.bottom);

        hotbarSlot = Math.min(h * 0.08f, w / 18f);
        hotbarY = h - hotbarSlot - dp(10);
        hotbarX0 = (w - hotbarSlot * HOTBAR_COUNT) / 2f;

        float gs = h * 0.085f;
        gearRect.set(w - gs - dp(8), dp(8), w - dp(8), dp(8) + gs);
        invRect.set(gearRect.left - gs - dp(8), dp(8), gearRect.left - dp(8), dp(8) + gs);

        float ipw = Math.min(w * 0.88f, h * 2.2f);
        float iph = h * 0.9f;
        float ipl = (w - ipw) / 2f, ipt = (h - iph) / 2f;
        invPanel.set(ipl, ipt, ipl + ipw, ipt + iph);
        invPad = dp(14);
        invTitleY = ipt + dp(26);
        float leftW = ipw * 0.5f;
        invSlot = Math.min((leftW - invPad * 2f) / invCols, (iph - invPad * 3f - dp(34)) / invRows);
        invGridX = ipl + invPad;
        invGridY = ipt + invPad + dp(36);
        craftX0 = ipl + ipw * 0.54f;
        craftX1 = ipl + ipw - invPad;
        craftY0 = ipt + invPad + dp(36);
        craftRowH = (iph - invPad * 3f - dp(34)) / Inventory.RECIPES.length;
        invCloseRect.set(ipl + ipw / 2f - dp(60), ipt + iph - invPad - dp(34),
                ipl + ipw / 2f + dp(60), ipt + iph - invPad);

        float pw = Math.min(w * 0.68f, h * 1.6f);
        float ph = h * 0.9f;
        float pl = (w - pw) / 2f, pt = (h - ph) / 2f;
        panelRect.set(pl, pt, pl + pw, pt + ph);

        float pad = dp(16);
        float rowH = ph / 11.5f;
        float btn = rowH * 0.62f;
        float right = panelRect.right - pad;
        float valW = rowH * 1.2f;

        float y1 = panelRect.top + rowH * 1.05f;
        float y2 = panelRect.top + rowH * 2.05f;
        float y3 = panelRect.top + rowH * 3.05f;
        float y4 = panelRect.top + rowH * 4.05f;
        float y5 = panelRect.top + rowH * 5.05f;
        float y6 = panelRect.top + rowH * 6.05f;
        float y7 = panelRect.top + rowH * 7.05f;
        float y8 = panelRect.top + rowH * 8.20f;
        float y9 = panelRect.top + rowH * 9.45f;

        rdPlus.set(right - btn, y1 - btn / 2f, right, y1 + btn / 2f);
        rdMinus.set(rdPlus.left - valW - btn, y1 - btn / 2f, rdPlus.left - valW, y1 + btn / 2f);
        senPlus.set(right - btn, y2 - btn / 2f, right, y2 + btn / 2f);
        senMinus.set(senPlus.left - valW - btn, y2 - btn / 2f, senPlus.left - valW, y2 + btn / 2f);

        float toggleW = rowH * 1.9f;
        cloudRect.set(right - toggleW, y3 - btn / 2f, right, y3 + btn / 2f);
        statsRect.set(right - toggleW, y4 - btn / 2f, right, y4 + btn / 2f);
        flowRect.set(right - toggleW, y5 - btn / 2f, right, y5 + btn / 2f);
        shadowRect.set(right - toggleW, y6 - btn / 2f, right, y6 + btn / 2f);
        postRect.set(right - toggleW, y7 - btn / 2f, right, y7 + btn / 2f);

        float innerLeft = panelRect.left + pad;
        float bw = (right - innerLeft - dp(16)) / 3f;
        dayRect.set(innerLeft, y8 - btn / 2f, innerLeft + bw, y8 + btn / 2f);
        duskRect.set(dayRect.right + dp(8), y8 - btn / 2f, dayRect.right + dp(8) + bw, y8 + btn / 2f);
        nightRect.set(duskRect.right + dp(8), y8 - btn / 2f, duskRect.right + dp(8) + bw, y8 + btn / 2f);

        resetRect.set(innerLeft, y9 - btn / 2f, right, y9 + btn / 2f);
        float cb = rowH * 0.75f;
        closeRect.set(panelRect.left + (pw - cb * 2f) / 2f, panelRect.bottom - pad - cb,
                panelRect.left + (pw + cb * 2f) / 2f, panelRect.bottom - pad);

        mapSize = Math.min(h * 0.26f, w * 0.18f);
        float mx0 = Float.MAX_VALUE, mx1 = -Float.MAX_VALUE, mz0 = Float.MAX_VALUE, mz1 = -Float.MAX_VALUE;
        for (int[] c : CityGenerator.CITIES) {
            mx0 = Math.min(mx0, c[0]);
            mx1 = Math.max(mx1, c[0]);
            mz0 = Math.min(mz0, c[1]);
            mz1 = Math.max(mz1, c[1]);
        }
        mapMinX = mx0 - 240;
        mapMaxX = mx1 + 240;
        mapMinZ = mz0 - 240;
        mapMaxZ = mz1 + 240;

        float dw = w * 0.52f, dh = h * 0.42f;
        stopPanelRect.set((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f);
        float bw2 = dp(150), bh2 = dp(54);
        float by2 = stopPanelRect.bottom - dp(76);
        stopBtnRect.set(stopPanelRect.centerX() - bw2 - dp(16), by2, stopPanelRect.centerX() - dp(16), by2 + bh2);
        contBtnRect.set(stopPanelRect.centerX() + dp(16), by2, stopPanelRect.centerX() + bw2 + dp(16), by2 + bh2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // joystick
        fill.setColor(0x22FFFFFF);
        canvas.drawCircle(joyBaseX, joyBaseY, joyRadius, fill);
        stroke.setColor(0x55FFFFFF);
        canvas.drawCircle(joyBaseX, joyBaseY, joyRadius, stroke);
        LinearGradient knob = new LinearGradient(joyKnobX, joyKnobY - joyRadius * 0.4f,
                joyKnobX, joyKnobY + joyRadius * 0.4f, 0xEEFFFFFF, 0x99DDEEFF, Shader.TileMode.CLAMP);
        fill.setShader(knob);
        canvas.drawCircle(joyKnobX, joyKnobY, joyRadius * 0.42f, fill);
        fill.setShader(null);

        drawButton(canvas, jumpRect, "跳跃", 0xFF3FA34D, input.jump);
        drawButton(canvas, breakRect, "挖掘", 0xFFB4533A, false);
        drawButton(canvas, placeRect, "放置", 0xFF4A62C8, false);
        drawButton(canvas, flyRect, flying ? "飞行" : "行走", 0xFFB79A2E, flying);
        drawButton(canvas, cartRect, cartRiding ? "下车" : "矿车", 0xFF8A6A3A, cartRiding);

        // hotbar panel
        float pad = dp(6);
        tmpRect.set(hotbarX0 - pad, hotbarY - pad, hotbarX0 + hotbarSlot * HOTBAR_COUNT + pad, hotbarY + hotbarSlot + pad);
        fill.setColor(0x55000000);
        canvas.drawRoundRect(tmpRect, dp(10), dp(10), fill);
        for (int i = 0; i < HOTBAR_COUNT && i < BlockType.HOTBAR.length; i++) {
            float sx = hotbarX0 + i * hotbarSlot;
            int id = BlockType.HOTBAR[i];
            int cnt = inventory.count(id);
            tmpRect.set(sx + dp(3), hotbarY + dp(3), sx + hotbarSlot - dp(3), hotbarY + hotbarSlot - dp(3));
            drawSlotSwatch(canvas, tmpRect, id, cnt);
            boolean selected = id == input.selectedBlock;
            stroke.setColor(selected ? 0xFFFFFFFF : 0x55FFFFFF);
            stroke.setStrokeWidth(selected ? dp(3) : dp(1.5f));
            canvas.drawRoundRect(tmpRect, dp(5), dp(5), stroke);
            if (cnt > 0) {
                String cs = String.valueOf(cnt);
                float cx0 = tmpRect.centerX();
                float cy0 = tmpRect.bottom - dp(4);
                text.setColor(0xCC000000);
                canvas.drawText(cs, cx0 + dp(1), cy0 + dp(1), text);
                text.setColor(0xFFFFFFFF);
                canvas.drawText(cs, cx0, cy0, text);
            }
        }
        stroke.setStrokeWidth(dp(2));

        // crosshair
        float cx = w / 2f, cy = h / 2f;
        stroke.setColor(0xE6FFFFFF);
        canvas.drawLine(cx - dp(13), cy, cx - dp(4), cy, stroke);
        canvas.drawLine(cx + dp(4), cy, cx + dp(13), cy, stroke);
        canvas.drawLine(cx, cy - dp(13), cx, cy - dp(4), stroke);
        canvas.drawLine(cx, cy + dp(4), cx, cy + dp(13), stroke);
        fill.setColor(0xCCFFFFFF);
        canvas.drawCircle(cx, cy, dp(1.6f), fill);

        if (settings == null || settings.showStats) drawStats(canvas);
        drawBreakProgress(canvas);
        drawLoadingOrError(canvas);
        drawMinimap(canvas);
        drawGear(canvas);
        drawInvButton(canvas);
        if (settingsOpen) drawSettings(canvas);
        if (invOpen) drawInventory(canvas);
        if (showStopDialog) drawStopDialog(canvas);
    }

    private void drawMinimap(Canvas canvas) {
        float s = mapSize;
        float x0 = dp(8), y0 = dp(8);
        tmpRect.set(x0, y0, x0 + s, y0 + s);
        fill.setColor(0x99000000);
        canvas.drawRoundRect(tmpRect, dp(8), dp(8), fill);
        float rangeX = mapMaxX - mapMinX, rangeZ = mapMaxZ - mapMinZ;
        float scale = Math.min(s / rangeX, s / rangeZ);
        float ox = x0 + (s - rangeX * scale) / 2f;
        float oy = y0 + (s - rangeZ * scale) / 2f;

        stroke.setColor(0x88FFFFFF);
        stroke.setStrokeWidth(dp(1.2f));
        for (int[] seg : CityGenerator.SEGMENTS) {
            if (seg[0] == 0) {
                canvas.drawLine(ox + (seg[2] - mapMinX) * scale, oy + (seg[1] - mapMinZ) * scale,
                        ox + (seg[3] - mapMinX) * scale, oy + (seg[1] - mapMinZ) * scale, stroke);
            } else {
                canvas.drawLine(ox + (seg[1] - mapMinX) * scale, oy + (seg[2] - mapMinZ) * scale,
                        ox + (seg[1] - mapMinX) * scale, oy + (seg[3] - mapMinZ) * scale, stroke);
            }
        }

        int[] themeCol = {0xFFE04A3A, 0xFF9A9A9E, 0xFFE8E0C8, 0xFFE2C454, 0xFF6EA8FF, 0xFF5CB85C};
        for (int i = 0; i < CityGenerator.CITIES.length; i++) {
            int[] c = CityGenerator.CITIES[i];
            fill.setColor(themeCol[c[2]]);
            canvas.drawCircle(ox + (c[0] - mapMinX) * scale, oy + (c[1] - mapMinZ) * scale, dp(4), fill);
        }

        float pxn = ox + (playerX - mapMinX) * scale;
        float pzn = oy + (playerZ - mapMinZ) * scale;
        fill.setColor(0xFFFFFFFF);
        canvas.drawCircle(pxn, pzn, dp(3), fill);
        float fx = (float) Math.sin(Math.toRadians(playerYaw));
        float fz = (float) -Math.cos(Math.toRadians(playerYaw));
        stroke.setColor(0xFFFFFFFF);
        stroke.setStrokeWidth(dp(2));
        canvas.drawLine(pxn, pzn, pxn + fx * dp(9), pzn + fz * dp(9), stroke);
        stroke.setStrokeWidth(dp(2));
    }

    private void drawStopDialog(Canvas canvas) {
        fill.setColor(0xEE101820);
        canvas.drawRoundRect(stopPanelRect, dp(16), dp(16), fill);
        stroke.setColor(0x66FFFFFF);
        canvas.drawRoundRect(stopPanelRect, dp(16), dp(16), stroke);
        text.setTextSize(dp(20));
        text.setColor(0xFFFFFFFF);
        canvas.drawText("即将到站，是否停靠？", stopPanelRect.centerX(), stopPanelRect.top + dp(54), text);
        text.setTextSize(dp(14));
        drawButton(canvas, stopBtnRect, "停靠", 0xFFA03A3A, false);
        drawButton(canvas, contBtnRect, "继续前进", 0xFF3FA34D, false);
    }

    private void drawInvButton(Canvas canvas) {
        drawButton(canvas, invRect, "背包", 0xFF4A6A3A, invOpen);
    }

    private void drawInventory(Canvas canvas) {
        fill.setColor(0xF2101820);
        canvas.drawRoundRect(invPanel, dp(16), dp(16), fill);
        stroke.setColor(0x66FFFFFF);
        canvas.drawRoundRect(invPanel, dp(16), dp(16), stroke);

        text.setTextSize(dp(18));
        text.setColor(0xFFFFFFFF);
        canvas.drawText("背包", invGridX + invSlot * invCols / 2f, invTitleY, text);
        canvas.drawText("合成", (craftX0 + craftX1) / 2f, invTitleY, text);
        text.setTextSize(dp(13));

        // inventory grid: all blocks 1..38
        int id = 1;
        for (int row = 0; row < invRows; row++) {
            for (int col = 0; col < invCols; col++) {
                if (id >= 39) break;
                float sx = invGridX + col * invSlot;
                float sy = invGridY + row * invSlot;
                tmpRect.set(sx + dp(2), sy + dp(2), sx + invSlot - dp(2), sy + invSlot - dp(2));
                int cnt = inventory.count(id);
                drawSlotSwatch(canvas, tmpRect, id, cnt);
                boolean selected = id == input.selectedBlock;
                stroke.setColor(selected ? 0xFFFFFFFF : 0x44FFFFFF);
                stroke.setStrokeWidth(selected ? dp(3) : dp(1.2f));
                canvas.drawRoundRect(tmpRect, dp(5), dp(5), stroke);
                text.setColor(0xFFFFFFFF);
                canvas.drawText(String.valueOf(cnt), tmpRect.centerX(), tmpRect.bottom - dp(5), text);
                id++;
            }
        }
        stroke.setStrokeWidth(dp(2));

        // crafting list
        for (int i = 0; i < Inventory.RECIPES.length; i++) {
            int[] r = Inventory.RECIPES[i];
            float yc = craftY0 + craftRowH * i + craftRowH / 2f;
            boolean can = inventory.canCraft(i);
            text.setTextAlign(Paint.Align.LEFT);
            text.setColor(can ? 0xFFFFFFFF : 0x77FFFFFF);
            Paint.FontMetrics fm = text.getFontMetrics();
            String label = name(r[0]) + " x" + r[1] + " -> " + name(r[2]) + " x" + r[3];
            canvas.drawText(label, craftX0, yc - (fm.ascent + fm.descent) / 2f, text);
            text.setTextAlign(Paint.Align.CENTER);
            float bw = dp(58);
            tmpRect.set(craftX1 - bw, yc - craftRowH * 0.32f, craftX1, yc + craftRowH * 0.32f);
            drawButton(canvas, tmpRect, "合成", can ? 0xFF3FA34D : 0xFF555555, false);
        }

        drawButton(canvas, invCloseRect, "关闭", 0xFF44607A, false);
    }

    private static final String[] NAMES = {
            "空气", "草方块", "泥土", "石头", "圆石", "沙子", "原木", "树叶", "木板", "水",
            "玻璃", "基岩", "雪", "砖块", "黑曜石", "砾石", "煤矿", "铁矿", "金矿", "钻石矿",
            "沙岩", "苔石", "冰", "黏土", "铁块", "金块", "钻石块", "书架", "萤石", "白桦原木",
            "白桦木板", "石砖", "铁轨", "铁轨", "白灰泥", "红灰泥", "黄灰泥", "蓝灰泥", "深灰泥"
    };

    private static String name(int id) {
        if (id >= 0 && id < NAMES.length) return NAMES[id];
        return "方块" + id;
    }

    private void drawGear(Canvas canvas) {
        drawButton(canvas, gearRect, "设置", 0xFF3A5C8A, settingsOpen);
    }

    private void drawSettings(Canvas canvas) {
        fill.setColor(0xF2101820);
        canvas.drawRoundRect(panelRect, dp(16), dp(16), fill);
        stroke.setColor(0x66FFFFFF);
        canvas.drawRoundRect(panelRect, dp(16), dp(16), stroke);

        text.setTextSize(dp(18));
        text.setColor(0xFFFFFFFF);
        canvas.drawText("设置", panelRect.centerX(), panelRect.top + dp(26), text);
        text.setTextSize(dp(14));
        float left = panelRect.left + dp(16);

        labelLeft(canvas, "视距", left, rdMinus.centerY());
        drawButton(canvas, rdMinus, "-", 0xFF44566A, false);
        valueCentered(canvas, String.valueOf(settings.renderDistance), rdMinus.right + (rdPlus.left - rdMinus.right) / 2f, rdMinus.centerY());
        drawButton(canvas, rdPlus, "+", 0xFF44566A, false);

        labelLeft(canvas, "灵敏度", left, senMinus.centerY());
        drawButton(canvas, senMinus, "-", 0xFF44566A, false);
        valueCentered(canvas, String.format(java.util.Locale.US, "%.1f", settings.sensitivity),
                senMinus.right + (senPlus.left - senMinus.right) / 2f, senMinus.centerY());
        drawButton(canvas, senPlus, "+", 0xFF44566A, false);

        labelLeft(canvas, "云层", left, cloudRect.centerY());
        drawButton(canvas, cloudRect, settings.clouds ? "开" : "关", settings.clouds ? 0xFF3FA34D : 0xFF666666, settings.clouds);

        labelLeft(canvas, "性能面板", left, statsRect.centerY());
        drawButton(canvas, statsRect, settings.showStats ? "开" : "关", settings.showStats ? 0xFF3FA34D : 0xFF666666, settings.showStats);

        labelLeft(canvas, "时间流动", left, flowRect.centerY());
        drawButton(canvas, flowRect, settings.timeFlowing ? "流动" : "暂停", settings.timeFlowing ? 0xFF3FA34D : 0xFF666666, settings.timeFlowing);

        labelLeft(canvas, "实时阴影", left, shadowRect.centerY());
        drawButton(canvas, shadowRect, settings.shadows ? "开" : "关", settings.shadows ? 0xFF3FA34D : 0xFF666666, settings.shadows);

        labelLeft(canvas, "画质增强", left, postRect.centerY());
        drawButton(canvas, postRect, settings.postFx ? "开" : "关", settings.postFx ? 0xFF3FA34D : 0xFF666666, settings.postFx);

        labelLeft(canvas, "时间", left, dayRect.centerY());
        drawButton(canvas, dayRect, "白天", 0xFFB79A2E, settings.timePreset == 1);
        drawButton(canvas, duskRect, "黄昏", 0xFFB4762E, settings.timePreset == 2);
        drawButton(canvas, nightRect, "夜晚", 0xFF2E3A6A, settings.timePreset == 3);

        drawButton(canvas, resetRect, "重置世界", 0xFFA03A3A, false);
        drawButton(canvas, closeRect, "关闭", 0xFF44607A, false);
    }

    private void labelLeft(Canvas canvas, String s, float x, float cy) {
        text.setTextAlign(Paint.Align.LEFT);
        text.setColor(0xFFFFFFFF);
        Paint.FontMetrics fm = text.getFontMetrics();
        canvas.drawText(s, x, cy - (fm.ascent + fm.descent) / 2f, text);
        text.setTextAlign(Paint.Align.CENTER);
    }

    private void valueCentered(Canvas canvas, String s, float cx, float cy) {
        text.setColor(0xFFFFFFFF);
        Paint.FontMetrics fm = text.getFontMetrics();
        canvas.drawText(s, cx, cy - (fm.ascent + fm.descent) / 2f, text);
    }

    private void drawBreakProgress(Canvas canvas) {
        if (!showBreak || breakProgress <= 0f) return;
        float cx = w / 2f, cy = h / 2f;
        float r = dp(20);
        stroke.setColor(0x66FFFFFF);
        stroke.setStrokeWidth(dp(4));
        canvas.drawCircle(cx, cy, r, stroke);
        stroke.setColor(0xFFFFD24A);
        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, -90f, 360f * breakProgress, false, stroke);
        stroke.setStrokeWidth(dp(2));
    }

    private void drawLoadingOrError(Canvas canvas) {
        if (Diag.hasError()) {
            text.setTextAlign(Paint.Align.LEFT);
            String err = "错误:\n" + Diag.getError();
            String[] lines = err.split("\n");
            float lineH = dp(15);
            float maxW = w * 0.92f;
            float pad = dp(10);
            float boxH = pad * 2 + lineH * Math.min(lines.length, 10);
            tmpRect.set(dp(8), dp(8), w - dp(8), dp(8) + boxH);
            fill.setColor(0xEEDD2222);
            canvas.drawRoundRect(tmpRect, dp(8), dp(8), fill);
            text.setColor(0xFFFFFFFF);
            float y = dp(8) + pad + dp(11);
            for (int i = 0; i < lines.length && i < 10; i++) {
                canvas.drawText(truncate(lines[i], text, maxW - pad * 2), dp(8) + pad, y, text);
                y += lineH;
            }
            text.setTextAlign(Paint.Align.CENTER);
            return;
        }
        if (loading) {
            float barW = w * 0.4f;
            float barH = dp(14);
            float x = (w - barW) / 2f;
            float y = h * 0.5f - barH / 2f;
            tmpRect.set(x, y, x + barW, y + barH);
            fill.setColor(0x88000000);
            canvas.drawRoundRect(tmpRect, dp(7), dp(7), fill);
            tmpRect.set(x, y, x + barW * Math.max(0.02f, loadProgress), y + barH);
            fill.setColor(0xFF5ED36A);
            canvas.drawRoundRect(tmpRect, dp(7), dp(7), fill);
            text.setColor(0xFFFFFFFF);
            String label = "加载中 " + (int) (loadProgress * 100) + "%";
            canvas.drawText(label, w / 2f, y - dp(8), text);
        }
    }

    private static String truncate(String s, Paint p, float maxW) {
        if (p.measureText(s) <= maxW) return s;
        int n = s.length();
        while (n > 1 && p.measureText(s.substring(0, n) + "...") > maxW) n--;
        return s.substring(0, n) + "...";
    }

    private void drawStats(Canvas canvas) {
        float pad = dp(8);
        float y0 = dp(8) + mapSize + dp(6);
        String l1 = "FPS " + statFps;
        String l2 = "区块 " + statChunks;
        String l3 = "三角面 " + statTris;
        text.setTextAlign(Paint.Align.LEFT);
        float tw = Math.max(text.measureText(l1), Math.max(text.measureText(l2), text.measureText(l3)));
        tmpRect.set(dp(8), y0, dp(8) + tw + pad * 2, y0 + dp(58));
        fill.setColor(0x55000000);
        canvas.drawRoundRect(tmpRect, dp(8), dp(8), fill);
        text.setColor(0xFFE6F2FF);
        float ty = y0 + pad + dp(11);
        canvas.drawText(l1, dp(8) + pad, ty, text);
        canvas.drawText(l2, dp(8) + pad, ty + dp(16), text);
        canvas.drawText(l3, dp(8) + pad, ty + dp(32), text);
        text.setTextAlign(Paint.Align.CENTER);
    }

    private void drawButton(Canvas canvas, RectF r, String label, int color, boolean active) {
        int top = active ? 0xFFF2F2F2 : blend(color, 0xFFFFFFFF, 0.25f);
        int bottom = active ? 0xFFCCCCCC : color;
        LinearGradient grad = new LinearGradient(r.left, r.top, r.left, r.bottom, top, bottom, Shader.TileMode.CLAMP);
        fill.setShader(grad);
        float rad = dp(12);
        canvas.drawRoundRect(r, rad, rad, fill);
        fill.setShader(null);
        stroke.setColor(active ? 0xFFFFFFFF : 0x88FFFFFF);
        canvas.drawRoundRect(r, rad, rad, stroke);
        text.setColor(active ? 0xFF202020 : Color.WHITE);
        Paint.FontMetrics fm = text.getFontMetrics();
        float ty = r.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(label, r.centerX(), ty, text);
    }

    private static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xff, ag = (a >> 8) & 0xff, ab = a & 0xff;
        int br = (b >> 16) & 0xff, bg = (b >> 8) & 0xff, bb = b & 0xff;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xff000000 | (r << 16) | (g << 8) | bl;
    }

    private final Runnable statsTicker = new Runnable() {
        @Override
        public void run() {
            invalidate();
            postDelayed(this, 250);
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        postDelayed(statsTicker, 250);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(statsTicker);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int idx = e.getActionIndex();
                assignPointer(e.getPointerId(idx), e.getX(idx), e.getY(idx));
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (settingsOpen || invOpen) break;
                for (int i = 0; i < e.getPointerCount(); i++) {
                    int id = e.getPointerId(i);
                    int role = roles.get(id, -1);
                    float x = e.getX(i), y = e.getY(i);
                    if (role == ROLE_LOOK) {
                        float sens = settings != null ? settings.sensitivity : 1f;
                        input.addLook((x - lastLookX) * 0.22f * sens, (y - lastLookY) * 0.22f * sens);
                        lastLookX = x;
                        lastLookY = y;
                    } else if (role == ROLE_JOY) {
                        updateJoystick(x, y);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int idx = e.getActionIndex();
                releasePointer(e.getPointerId(idx));
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                for (int i = 0; i < roles.size(); i++) {
                    releasePointer(roles.keyAt(i));
                }
                roles.clear();
                break;
            }
        }
        invalidate();
        return true;
    }

    private void assignPointer(int id, float x, float y) {
        if (showStopDialog) {
            if (stopBtnRect.contains(x, y)) input.cartChoice = 1;
            else if (contBtnRect.contains(x, y)) input.cartChoice = 2;
            roles.put(id, 0);
            return;
        }
        if (invOpen) {
            handleInventoryTap(x, y);
            roles.put(id, 0);
            return;
        }
        if (settingsOpen) {
            handleSettingsTap(x, y);
            roles.put(id, 0);
            return;
        }
        if (invRect.contains(x, y)) {
            invOpen = true;
            settingsOpen = false;
            input.moveForward = 0f;
            input.moveStrafe = 0f;
            input.jump = false;
            input.breaking = false;
            roles.put(id, 0);
            return;
        }
        if (gearRect.contains(x, y)) {
            settingsOpen = true;
            invOpen = false;
            input.moveForward = 0f;
            input.moveStrafe = 0f;
            input.jump = false;
            input.breaking = false;
            roles.put(id, 0);
            return;
        }
        if (jumpRect.contains(x, y)) {
            roles.put(id, ROLE_JUMP);
            input.jump = true;
        } else if (breakRect.contains(x, y)) {
            roles.put(id, ROLE_BREAK);
            input.breaking = true;
        } else if (placeRect.contains(x, y)) {
            roles.put(id, ROLE_PLACE);
            input.placeRequested = true;
        } else if (flyRect.contains(x, y)) {
            roles.put(id, ROLE_FLY);
            input.flyToggleRequested = true;
        } else if (cartRect.contains(x, y)) {
            roles.put(id, 0);
            input.cartToggleRequested = true;
        } else if (hitHotbar(x, y)) {
            roles.put(id, 0);
        } else if (x < w * 0.42f) {
            roles.put(id, ROLE_JOY);
            updateJoystick(x, y);
        } else {
            roles.put(id, ROLE_LOOK);
            lastLookX = x;
            lastLookY = y;
        }
    }

    private void handleSettingsTap(float x, float y) {
        if (closeRect.contains(x, y)) {
            settingsOpen = false;
        } else if (rdMinus.contains(x, y)) {
            settings.renderDistance = Math.max(Settings.MIN_RD, settings.renderDistance - 16);
        } else if (rdPlus.contains(x, y)) {
            settings.renderDistance = Math.min(Settings.MAX_RD, settings.renderDistance + 16);
        } else if (senMinus.contains(x, y)) {
            settings.sensitivity = Math.max(0.4f, settings.sensitivity - 0.2f);
        } else if (senPlus.contains(x, y)) {
            settings.sensitivity = Math.min(2.4f, settings.sensitivity + 0.2f);
        } else if (cloudRect.contains(x, y)) {
            settings.clouds = !settings.clouds;
        } else if (statsRect.contains(x, y)) {
            settings.showStats = !settings.showStats;
        } else if (flowRect.contains(x, y)) {
            settings.timeFlowing = !settings.timeFlowing;
            if (settings.timeFlowing) settings.timePreset = -1;
        } else if (shadowRect.contains(x, y)) {
            settings.shadows = !settings.shadows;
        } else if (postRect.contains(x, y)) {
            settings.postFx = !settings.postFx;
        } else if (dayRect.contains(x, y)) {
            settings.timeFlowing = false;
            settings.timePreset = 1;
        } else if (duskRect.contains(x, y)) {
            settings.timeFlowing = false;
            settings.timePreset = 2;
        } else if (nightRect.contains(x, y)) {
            settings.timeFlowing = false;
            settings.timePreset = 3;
        } else if (resetRect.contains(x, y)) {
            settings.resetRequested = true;
            settingsOpen = false;
        }
    }

    private void handleInventoryTap(float x, float y) {
        if (invCloseRect.contains(x, y)) {
            invOpen = false;
            return;
        }
        for (int row = 0; row < invRows; row++) {
            for (int col = 0; col < invCols; col++) {
                int id = 1 + row * invCols + col;
                if (id >= 36) break;
                float sx = invGridX + col * invSlot;
                float sy = invGridY + row * invSlot;
                if (x >= sx && x <= sx + invSlot && y >= sy && y <= sy + invSlot) {
                    if (inventory.count(id) > 0) input.selectedBlock = id;
                    return;
                }
            }
        }
        for (int i = 0; i < Inventory.RECIPES.length; i++) {
            float yc = craftY0 + craftRowH * i + craftRowH / 2f;
            float bw = dp(58);
            if (x >= craftX1 - bw && x <= craftX1
                    && y >= yc - craftRowH * 0.32f && y <= yc + craftRowH * 0.32f) {
                inventory.craft(i);
                return;
            }
        }
    }

    private void releasePointer(int id) {
        int role = roles.get(id, -1);
        if (role == ROLE_JUMP) input.jump = false;
        if (role == ROLE_BREAK) input.breaking = false;
        if (role == ROLE_JOY) {
            input.moveForward = 0f;
            input.moveStrafe = 0f;
            input.sprint = false;
            joyKnobX = joyBaseX;
            joyKnobY = joyBaseY;
        }
        roles.delete(id);
    }

    private void updateJoystick(float x, float y) {
        float dx = x - joyBaseX;
        float dy = y - joyBaseY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float max = joyRadius;
        float nx = dx, ny = dy;
        if (dist > max && dist > 0) {
            nx = dx / dist * max;
            ny = dy / dist * max;
        }
        joyKnobX = joyBaseX + nx;
        joyKnobY = joyBaseY + ny;
        float mag = Math.min(1f, dist / max);
        if (dist > 0) {
            input.moveStrafe = (dx / dist) * mag;
            input.moveForward = (-dy / dist) * mag;
        } else {
            input.moveStrafe = 0f;
            input.moveForward = 0f;
        }
        input.sprint = mag > 0.86f;
    }

    private boolean hitHotbar(float x, float y) {
        if (y < hotbarY - dp(8) || y > hotbarY + hotbarSlot + dp(8)) return false;
        for (int i = 0; i < HOTBAR_COUNT && i < BlockType.HOTBAR.length; i++) {
            float sx = hotbarX0 + i * hotbarSlot;
            if (x >= sx && x <= sx + hotbarSlot) {
                input.selectedBlock = BlockType.HOTBAR[i];
                return true;
            }
        }
        return false;
    }
}
