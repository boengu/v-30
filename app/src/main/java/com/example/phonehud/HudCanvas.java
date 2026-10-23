package com.example.phonehud;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

/** Low-distraction, landscape, mirrored windscreen layout with stationary touch controls. */
public final class HudCanvas extends View {
    public interface Listener {
        void onExit();
        void onToggleMirror();
        void onNextTheme();
        void onBrightnessCycle();
        void onNextDemo();
    }
    private static final int[] ACCENTS = {Color.rgb(0, 245, 233), Color.rgb(255, 189, 74), Color.rgb(134, 255, 134)};
    private static final int MUTED = Color.rgb(133, 160, 167);
    private static final int WHITE = Color.rgb(227, 244, 248);
    private static final int WARNING = Color.rgb(255, 170, 74);
    private static final int BACK = Color.rgb(1, 5, 10);
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Listener actions;
    private NavData data = NavData.empty();
    private CameraData camera = CameraData.empty();
    private boolean connected, mirror, preview, controls, oledMode;
    private int speed = -1, theme, brightnessLevel;
    private String status = "연결 대기";
    private final Runnable hideControls = () -> { controls = false; invalidate(); };
    private final Runnable oledTick = this::invalidate;

    public HudCanvas(Context context, Listener actions) { super(context); this.actions = actions; }

    public void update(NavData value, CameraData cameraValue, boolean connected, int speed, boolean mirror,
                       boolean preview, String status, int theme, int brightnessLevel, boolean oledMode) {
        if (value != null) this.data = value;
        this.camera = cameraValue == null ? CameraData.empty() : cameraValue;
        this.connected = connected; this.speed = speed; this.mirror = mirror;
        this.preview = preview; this.status = status == null ? "" : status;
        this.theme = Math.max(0, theme % ACCENTS.length);
        this.brightnessLevel = brightnessLevel;
        this.oledMode = oledMode;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);
        float scale = Math.min(getWidth() / 1100f, getHeight() / 550f);
        float dx = (getWidth() - 1100f * scale) / 2f;
        float dy = (getHeight() - 550f * scale) / 2f;
        int accent = ACCENTS[theme];
        if (oledMode && theme == 0) accent = Color.rgb(0, 212, 204);
        if (oledMode && theme == 1) accent = Color.rgb(223, 160, 65);
        if (oledMode && theme == 2) accent = Color.rgb(113, 215, 116);
        canvas.save();
        canvas.translate(dx, dy); canvas.scale(scale, scale);
        // Shifts by <= 4 logical pixels every 45 seconds for OLED panel care.
        // Pixel shifting is mitigation only and cannot guarantee burn-in prevention.
        if (oledMode) {
            int step = (int) ((SystemClock.uptimeMillis() / 45000L) % 9);
            canvas.translate((step % 3 - 1) * 4f, (step / 3 - 1) * 3f);
            removeCallbacks(oledTick);
            postDelayed(oledTick, 45000L);
        }
        if (mirror) { canvas.translate(1100f, 0f); canvas.scale(-1f, 1f); }

        long age = data.timestamp == 0 ? -1 : System.currentTimeMillis() - data.timestamp;
        boolean navigationFresh = (preview || (connected && age >= 0 && age <= NaverNavParser.MAX_AGE_MS))
                && !"WAIT".equals(data.direction);
        boolean stale = !preview && connected && age > 12000 && age <= NaverNavParser.MAX_AGE_MS && !"WAIT".equals(data.direction);
        boolean isDemo = preview || "DEMO".equals(data.source);
        String direction = navigationFresh ? data.direction : "WAIT";
        String diagram = navigationFresh ? data.diagram : "WAIT";
        String distance = navigationFresh ? data.distance : "--";
        String road = navigationFresh ? data.road : "길안내 정보를 기다리고 있습니다";
        long cameraAge = camera.timestamp == 0 ? -1 : System.currentTimeMillis() - camera.timestamp;
        boolean cameraFresh = (preview || connected) && camera.present()
                && (preview || cameraAge >= 0 && cameraAge <= CameraData.MAX_AGE_MS);
        int navColor = stale ? WARNING : accent;
        // In OLED mode, true black interiors reduce lit pixels and static panel exposure.
        if (!oledMode) {
            roundRect(canvas, 22, 20, 1078, 530, 28, Color.rgb(4, 13, 21));
            roundRect(canvas, 34, 90, 705, 420, 21, Color.rgb(6, 25, 33));
            roundRect(canvas, 719, 90, 1066, 420, 21, Color.rgb(4, 21, 29));
        }
        circle(canvas, 67, 60, 7, !connected && !preview ? WARNING : accent);
        text(canvas, "HUD LINK  2.5", 88, 67, 23, WHITE, false);
        String s = isDemo ? "DEMO · 실주행 아님" : !connected ? "연결 확인 필요" : stale ? "안내 갱신 지연" : navigationFresh ? "길안내 연결됨" : status;
        if (s == null || s.isEmpty()) s = "연결됨 · 알림 대기";
        textFit(canvas, s, 264, 67, 20, isDemo || stale || !connected || s.contains("필요") || s.contains("미수신") ? WARNING : MUTED, 660);
        text(canvas, "" + (mirror ? "MIRROR" : "DIRECT") + "   •   " + themeName(), 1040, 67, 17, MUTED, true);

        drawDiagram(canvas, diagram, navColor);
        text(canvas, distance == null ? "--" : distance, 432, 260, 91, navColor, false);
        text(canvas, diagramText(diagram), 438, 318, 25, MUTED, false);
        // Speed is always from the HUD phone's GPS, never from notification text.
        int shownSpeed = preview ? 68 : speed;
        text(canvas, shownSpeed >= 0 ? Integer.toString(shownSpeed) : "--", 1020, 276, 120, accent, true);
        text(canvas, "km/h", 1021, 325, 30, MUTED, true);
        text(canvas, "GPS SPEED", 1021, 376, 18, MUTED, true);
        // Independently timed safety/camera ribbon. Never infer missing camera details.
        if (cameraFresh) {
            boolean excess = camera.limitKmh > 0 && shownSpeed >= 0 && shownSpeed > camera.limitKmh;
            int warn = excess ? Color.rgb(255, 109, 104) : Color.rgb(255, 190, 84);
            if (!oledMode) roundRect(canvas, 38, 437, 1062, 521, 16, Color.rgb(45, 31, 22));
            cameraIcon(canvas, 69, 467, warn);
            text(canvas, camera.label(), 132, 483, 26, warn, false);
            text(canvas, "카메라 " + camera.distance, 586, 484, 30, WHITE, false);
            if (camera.limitKmh > 0) {
                text(canvas, "제한 " + camera.limitKmh, 1030, 484, 32, excess ? warn : WHITE, true);
                text(canvas, excess ? "속도 확인" : "km/h", 1030, 510, 15, excess ? warn : MUTED, true);
            } else text(canvas, "제한속도 미확인", 1032, 484, 21, MUTED, true);
        } else {
            line(canvas, 45, 439, 1055, 439, Color.rgb(34, 56, 62), 1.5f);
            text(canvas, "단속 안내 없음 · 실제 네이버지도 알림에 정보가 있을 때만 표시", 53, 477, 23, MUTED, false);
        }
        textFit(canvas, navigationFresh ? road : "알림 기반 보조 정보 · 도로 표지판과 실제 내비 안내 우선", 52, 413,
                22, navigationFresh ? WHITE : MUTED, 995);
        canvas.restore();

        if (controls) {
            canvas.save(); canvas.translate(dx, dy); canvas.scale(scale, scale);
            roundRect(canvas, 100, 423, 1000, 533, 20, Color.rgb(17, 32, 40));
            String[] controlsText = preview ? new String[]{"나가기", "좌우반전", "색상", "밝기", "다음 그림"}
                    : new String[]{"나가기", "좌우반전", "색상", "밝기", "설정은 정차 후"};
            for (int i = 0; i < 5; i++) {
                int x0 = 111 + i * 177;
                roundRect(canvas, x0, 439, x0 + 164, 518, 12, i == 0 ? Color.rgb(42, 50, 57) : Color.rgb(27, 58, 66));
                text(canvas, controlsText[i], x0 + 82, 488, i == 4 ? 17 : 20, WHITE, false, true);
            }
            canvas.restore();
        }
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(oledTick);
        removeCallbacks(hideControls);
        super.onDetachedFromWindow();
    }

    private String themeName() { return theme == 1 ? "AMBER" : theme == 2 ? "GREEN" : "CYAN"; }
    private String diagramText(String diagram) {
        switch (diagram) {
            case "TUNNEL_LEFT": return "터널 후 좌측 진출 · 도식";
            case "TUNNEL_RIGHT": return "터널 후 우측 진출 · 도식";
            case "EXIT_LEFT": return "IC / 출구 좌측 진출 · 도식";
            case "EXIT_RIGHT": return "IC / 출구 우측 진출 · 도식";
            case "FORK_LEFT": return "분기점 좌측 · 도식";
            case "FORK_RIGHT": return "분기점 우측 · 도식";
            case "LEFT": return "다음 좌회전";
            case "RIGHT": return "다음 우회전";
            case "STRAIGHT": return "직진 유지";
            case "UTURN": return "유턴";
            case "ROUND": return "회전교차로";
            case "ARRIVE": return "목적지 도착";
            default: return "길안내 데이터 없음";
        }
    }
    private void drawDiagram(Canvas c, String diagram, int accent) {
        if (diagram.startsWith("TUNNEL_") || diagram.startsWith("FORK_") || diagram.startsWith("EXIT_")) {
            boolean right = diagram.endsWith("RIGHT");
            boolean tunnel = diagram.startsWith("TUNNEL_");
            boolean exit = diagram.startsWith("EXIT_");
            int dim = Color.rgb(52, 82, 89);
            // Schematic shape, explicitly not a map of the true road or number of lanes.
            if (tunnel) {
                p.setColor(Color.rgb(66, 104, 115)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5);
                c.drawArc(new RectF(95, 104, 356, 365), 180, 180, false, p);
                c.drawLine(95, 233, 95, 370, p); c.drawLine(356, 233, 356, 370, p);
                text(c, "TUNNEL", 225, 157, 17, MUTED, false, true);
            } else {
                roundRect(c, 152, 115, 295, 159, 12, Color.rgb(20, 46, 52));
                text(c, exit ? "IC  EXIT" : "JC  FORK", 224, 143, 19, MUTED, false, true);
            }
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(21); p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            Path left = new Path(); left.moveTo(225, 352); left.lineTo(225, 272);
            left.cubicTo(225, 227, 180, 222, 144, 190);
            Path rightPath = new Path(); rightPath.moveTo(225, 352); rightPath.lineTo(225, 272);
            rightPath.cubicTo(225, 227, 270, 222, 306, 190);
            p.setColor(right ? dim : accent); c.drawPath(left, p);
            p.setColor(right ? accent : dim); c.drawPath(rightPath, p);
            p.setStrokeWidth(16);
            Path tip = new Path();
            if (right) { tip.moveTo(268, 192); tip.lineTo(308, 189); tip.lineTo(302, 229); }
            else { tip.moveTo(181, 192); tip.lineTo(142, 189); tip.lineTo(147, 229); }
            c.drawPath(tip,p); p.setStyle(Paint.Style.FILL);
            return;
        }
        drawTurn(c, diagram, accent);
    }
    private void cameraIcon(Canvas c, float x, float y, int color) {
        p.setStyle(Paint.Style.STROKE); p.setColor(color); p.setStrokeWidth(5);
        c.drawRoundRect(x - 22, y - 16, x + 22, y + 18, 6, 6, p);
        c.drawCircle(x, y + 1, 8, p);
        c.drawLine(x - 10, y - 25, x + 10, y - 25, p);
        p.setStyle(Paint.Style.FILL);
    }
    private void drawTurn(Canvas c, String dir, int color) {
        p.setStyle(Paint.Style.STROKE); p.setColor(color); p.setStrokeWidth(24);
        p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        Path x = new Path();
        switch (dir) {
            case "LEFT": case "RIGHT": {
                int sign = "RIGHT".equals(dir) ? 1 : -1;
                float center = 216;
                x.moveTo(center, 347); x.lineTo(center, 262);
                x.quadTo(center, 208, center + sign * 47, 208); x.lineTo(center + sign * 115, 208);
                c.drawPath(x, p); x.reset();
                float end = center + sign * 115;
                x.moveTo(end - sign * 43, 167); x.lineTo(end, 208); x.lineTo(end - sign * 43, 249);
                c.drawPath(x, p); break;
            }
            case "STRAIGHT":
                x.moveTo(225, 348); x.lineTo(225, 189); c.drawPath(x, p); x.reset();
                x.moveTo(174, 238); x.lineTo(225, 187); x.lineTo(276, 238); c.drawPath(x, p); break;
            case "UTURN":
                x.moveTo(152, 349); x.lineTo(152, 267);
                x.arcTo(new RectF(152, 161, 298, 307), 180, 180, false);
                x.lineTo(298, 352); c.drawPath(x, p); x.reset();
                x.moveTo(259, 308); x.lineTo(298, 352); x.lineTo(336, 307); c.drawPath(x, p); break;
            case "ROUND":
                c.drawArc(new RectF(143, 185, 301, 344), 27, 310, false, p);
                x.moveTo(294, 207); x.lineTo(325, 201); x.lineTo(318, 234); c.drawPath(x,p); break;
            case "ARRIVE":
                c.drawCircle(225, 258, 71, p); p.setStyle(Paint.Style.FILL);
                c.drawCircle(225, 258, 19, p); break;
            default:
                p.setStyle(Paint.Style.FILL);
                text(c, "—", 225, 298, 135, MUTED, true, true);
        }
        p.setStyle(Paint.Style.FILL);
    }
    private void roundRect(Canvas c, float l, float t, float r, float b, float rad, int fill) {
        p.setColor(fill); p.setStyle(Paint.Style.FILL);
        c.drawRoundRect(l, t, r, b, rad, rad, p);
    }
    private void circle(Canvas c, float x, float y, float rad, int fill) {
        p.setColor(fill); p.setStyle(Paint.Style.FILL); c.drawCircle(x, y, rad, p);
    }
    private void line(Canvas c, float x1, float y1, float x2, float y2, int color, float thick) {
        p.setColor(color); p.setStrokeWidth(thick); c.drawLine(x1, y1, x2, y2, p);
    }
    private void text(Canvas c, String value, float x, float y, float size, int color, boolean right) {
        text(c, value, x, y, size, color, right, false);
    }
    private void text(Canvas c, String value, float x, float y, float size, int color, boolean right, boolean center) {
        p.setColor(color); p.setStyle(Paint.Style.FILL); p.setTextSize(size);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        p.setTextAlign(center ? Paint.Align.CENTER : right ? Paint.Align.RIGHT : Paint.Align.LEFT);
        c.drawText(value == null ? "" : value, x, y, p);
    }
    private void textFit(Canvas c, String value, float x, float y, float size, int color, float width) {
        String v = value == null ? "" : value;
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); p.setTextSize(size);
        while (v.length() > 2 && p.measureText(v) > width) v = v.substring(0, v.length() - 2) + "…";
        text(c, v, x, y, size, color, false);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float scale = Math.min(getWidth() / 1100f, getHeight() / 550f);
        float dx = (getWidth() - 1100f * scale) / 2f;
        float dy = (getHeight() - 550f * scale) / 2f;
        float x = (event.getX() - dx) / scale, y = (event.getY() - dy) / scale;
        if (controls && x >= 100 && x < 1000 && y >= 423 && y <= 533) {
            int index = Math.min(4, Math.max(0, (int) ((x - 111) / 177)));
            if (index == 0) actions.onExit();
            else if (index == 1) actions.onToggleMirror();
            else if (index == 2) actions.onNextTheme();
            else if (index == 3) actions.onBrightnessCycle();
            else if (preview) actions.onNextDemo();
        }
        controls = !controls;
        removeCallbacks(hideControls);
        if (controls) postDelayed(hideControls, 7000);
        invalidate();
        return true;
    }
}
