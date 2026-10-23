package com.example.phonehud;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Single installer, two explicit roles: navigation transmitter or reused-phone HUD. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(6, 15, 23);
    private static final int CARD = Color.rgb(14, 31, 42);
    private static final int CARD2 = Color.rgb(22, 44, 54);
    private static final int CYAN = Color.rgb(0, 235, 224);
    private static final int WHITE = Color.rgb(235, 247, 250);
    private static final int MUTED = Color.rgb(156, 177, 186);
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService diagnostics = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService polling;
    private String screen = "home";
    private TextView senderInfo, diagnosisInfo;
    private EditText ipInput, pinInput;
    private HudCanvas hud;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private int speed = -1, theme = 0, brightnessLevel = 1;
    private boolean oledMode = false;
    private long speedUpdatedAt = 0L;
    private boolean mirror = true, demo = false, linkOkay = false;
    private String linkText = "연결 대기";
    private NavData remote = NavData.empty();
    private CameraData remoteCamera = CameraData.empty();
    private int demoScene = 0;
    private int session = 0;
    private int[] lastSpeeds = {-1, -1, -1};
    private int speedCursor = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        mirror = prefs.getBoolean("mirror", true);
        theme = prefs.getInt("theme", 0);
        oledMode = prefs.getBoolean("oledMode", isV30Device());
        brightnessLevel = prefs.getInt("brightness", isV30Device() ? 0 : 1);
        showHome();
    }
    /** OS version is not assumed: some V30 units still run Nougat/Oreo. */
    private boolean isV30Device() {
        return HudHardwareProfile.isV30(Build.MANUFACTURER, Build.MODEL, Build.DEVICE);
    }
    private float hudBrightness() {
        return HudHardwareProfile.brightness(oledMode, brightnessLevel);
    }
    private int dp(float size) { return Math.round(getResources().getDisplayMetrics().density * size); }
    private GradientDrawable background(int color, int radius, int border) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        if (border != 0) d.setStroke(dp(1), border);
        return d;
    }
    private LinearLayout page(String section, String title, String subtitle) {
        stopReceiver();
        screen = "menu";
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().getAttributes().screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(getWindow().getAttributes());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(23), dp(22), dp(32));
        scroll.addView(root);
        setContentView(scroll);
        label(root, "HUD LINK  /  " + section, 13, CYAN, true);
        label(root, title, 30, WHITE, true);
        label(root, subtitle, 14, MUTED, false);
        gap(root, 15);
        return root;
    }
    private void gap(LinearLayout box, int heightDp) {
        View v = new View(this); box.addView(v, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }
    private TextView label(LinearLayout box, String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        t.setLineSpacing(dp(2), 1.03f);
        t.setPadding(0, dp(3), 0, dp(7));
        box.addView(t, new LinearLayout.LayoutParams(-1, -2));
        return t;
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(17), dp(16), dp(17), dp(17));
        box.setBackground(background(CARD, 18, Color.rgb(36, 68, 78)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(13); parent.addView(box, lp);
        return box;
    }
    private void button(LinearLayout box, String value, boolean primary, Runnable onClick) {
        Button b = new Button(this);
        b.setText(value); b.setTextSize(15); b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        b.setTextColor(primary ? Color.rgb(2, 23, 28) : WHITE);
        b.setBackground(background(primary ? CYAN : CARD2, 13, primary ? 0 : Color.rgb(49, 75, 84)));
        b.setElevation(dp(primary ? 3 : 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.topMargin = dp(9); box.addView(b, lp);
        b.setOnClickListener(v -> onClick.run());
    }
    private void twoButtons(LinearLayout root, String a, Runnable ca, String b, Runnable cb) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(row, new LinearLayout.LayoutParams(-1, -2));
        addSmallButton(row, a, ca);
        addSmallButton(row, b, cb);
    }
    private void addSmallButton(LinearLayout row, String text, Runnable action) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false);
        b.setTextColor(WHITE); b.setTextSize(13);
        b.setBackground(background(CARD2, 11, Color.rgb(40, 70, 82)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
        lp.setMargins(dp(2), dp(6), dp(5), 0); row.addView(b, lp);
        b.setOnClickListener(v -> action.run());
    }
    private EditText edit(LinearLayout box, String value, String hint, boolean numeric) {
        EditText field = new EditText(this);
        field.setSingleLine(true); field.setText(value); field.setHint(hint); field.setSelectAllOnFocus(true);
        field.setTextColor(WHITE); field.setHintTextColor(MUTED); field.setTextSize(18);
        field.setPadding(dp(14), dp(9), dp(12), dp(9));
        field.setBackground(background(CARD2, 11, Color.rgb(57, 94, 102)));
        field.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT);
        box.addView(field, new LinearLayout.LayoutParams(-1, dp(54)));
        return field;
    }
    private void copy(String value) {
        ClipboardManager mgr = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        mgr.setPrimaryClip(ClipData.newPlainText("HUD LINK 연결", value));
        Toast.makeText(this, "클립보드에 복사했습니다", Toast.LENGTH_SHORT).show();
    }
    private String pin() {
        SharedPreferences prefs = getSharedPreferences("hud", MODE_PRIVATE);
        String value = prefs.getString("pin", "");
        if (!value.matches("[0-9]{8}")) {
            value = String.format(Locale.US, "%08d", new SecureRandom().nextInt(100000000));
            prefs.edit().putString("pin", value).apply();
        }
        return value;
    }
    private void showHome() {
        LinearLayout root = page("2.6 NAVER V30 BETA", "차 안의 시선을\n더 간결하게", "안 쓰는 안드로이드폰으로 만드는 미러형 차량 HUD.\n월 구독·클라우드 계정 없이 두 휴대폰을 연결합니다.");
        screen = "home";
        LinearLayout head = card(root);
        label(head, "01  MAIN PHONE", 12, CYAN, true);
        label(head, "길안내를 보내는 휴대폰", 20, WHITE, true);
        label(head, "네이버지도 알림에 있는 방향·분기·터널 문구 및 카메라 정보를 별도로 전송합니다.", 14, MUTED, false);
        button(head, "메인폰 설정  →", true, this::showSender);
        LinearLayout hudCard = card(root);
        label(hudCard, "02  HUD DISPLAY", 12, CYAN, true);
        label(hudCard, "앞유리에 반사할 공기계", 20, WHITE, true);
        label(hudCard, "터널/IC/JC 분기 그림, 카메라 경고, GPS 속도. 반사 모드·OLED 밝기를 조정합니다.", 14, MUTED, false);
        button(hudCard, "공기계 HUD 설정  →", true, this::showHudSetup);
        LinearLayout help = card(root);
        label(help, "시작하기", 19, WHITE, true);
        label(help, "1  두 휴대폰에 같은 앱 설치\n2  메인폰에서 네이버지도 실행\n3  같은 Wi-Fi 또는 메인폰 핫스팟 사용\n4  메인폰에서 알림 접근 허용·송신 시작\n5  V30에 IP와 8자리 PIN 입력 후 HUD 시작", 14, MUTED, false);
        label(root, "그림은 단순 분기 도식이며 실제 차로도가 아닙니다. 네이버지도 알림에 없는 정보는 표시하지 않습니다. 운전 중 조작하지 마세요.", 12, MUTED, false);
    }
    private void showSender() {
        LinearLayout root = page("MAIN PHONE", "네이버지도 송신", "네이버지도가 실행되는 메인 휴대폰에서 한 번 설정합니다.");
        screen = "sender";
        LinearLayout network = card(root);
        label(network, "연결 정보", 19, WHITE, true);
        label(network, "메인폰 IP 주소", 13, MUTED, false);
        String ipList = localIps();
        label(network, ipList, 20, CYAN, true);
        label(network, "공기계에 아래 8자리 PIN을 입력", 13, MUTED, false);
        String password = pin();
        label(network, password.substring(0, 4) + "  " + password.substring(4), 32, WHITE, true);
        twoButtons(network, "PIN 복사", () -> copy(pin()), "PIN 변경", () -> {
            String fresh = String.format(Locale.US, "%08d", new SecureRandom().nextInt(100000000));
            getSharedPreferences("hud", MODE_PRIVATE).edit().putString("pin", fresh).apply();
            showSender();
        });
        label(network, "포트 8765  ·  신뢰할 수 있는 Wi-Fi/핫스팟에서만 사용", 12, MUTED, false);
        LinearLayout statusCard = card(root);
        label(statusCard, "연결·길안내 상태", 19, WHITE, true);
        senderInfo = label(statusCard, "확인 중…", 14, MUTED, false);
        button(statusCard, "1  네이버지도 알림 접근 설정", false, () ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        twoButtons(statusCard, "2  송신 시작", () -> {
            askNotificationPermission();
            try {
                Intent svc = new Intent(this, HudSenderService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
                else startService(svc);
                Toast.makeText(this, "HUD 송신을 요청했습니다", Toast.LENGTH_SHORT).show();
                ui.postDelayed(this::refreshSender, 700);
            } catch (Exception e) {
                Toast.makeText(this, "송신 실패: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            }
        }, "송신 중지", () -> {
            stopService(new Intent(this, HudSenderService.class));
            ui.postDelayed(this::refreshSender, 500);
        });
        button(statusCard, "20초 데모 안내 보내기 (연결 확인용)", false, () -> {
            NavStore.demo(); refreshSender();
            Toast.makeText(this, "데모 안내 전송: 실제 길안내가 아닙니다", Toast.LENGTH_LONG).show();
        });
        LinearLayout apps = card(root);
        label(apps, "길안내 시작", 19, WHITE, true);
        label(apps, "메인폰에서 네이버지도 길안내를 시작하세요. 알림에 회전/단속 문구가 없는 경우 방향/카메라 정보는 대기로 표시됩니다.", 14, MUTED, false);
        button(apps, "네이버지도 열기  →", true, () -> openNavigation(NaverNavParser.PACKAGE));
        label(apps, "점검 팁: 네이버지도 앱 알림이 표시돼도 회전 지시가 포함되어 있지 않으면 HUD로 전송할 길안내 정보가 없습니다. 두 폰은 실도로 주행 전 주차 상태에서 검증하세요.", 12, MUTED, false);
        button(root, "← 처음으로", false, this::showHome);
        refreshSender();
    }
    private void refreshSender() {
        if (!"sender".equals(screen) || senderInfo == null) return;
        NavData n = NavStore.get();
        String server = HudSenderService.listening ? "송신 서버 정상" : HudSenderService.serviceActive ? "송신 서버 준비 중" : "송신 중지";
        if (!HudSenderService.serverError.isEmpty()) server = HudSenderService.serverError;
        String nav = n.timestamp == 0 ? "길안내 수신 대기" : n.source + " / " + n.diagram + " / " + n.distance;
        CameraData c = NavStore.getCamera();
        String cameraStatus = c.present() ? c.label() + " / " + c.distance + (c.limitKmh > 0 ? " / 제한 " + c.limitKmh : "") : "정보 대기";
        String observed = NavNotificationService.lastNaverNotificationAt == 0L ? "아직 없음" :
                ((System.currentTimeMillis() - NavNotificationService.lastNaverNotificationAt) / 1000L) + "초 전";
        senderInfo.setText("● 알림 접근: " + (hasListener() ? "허용됨" : "허용 필요")
                + "\n● 알림 서비스: " + (NavNotificationService.listenerConnected ? "연결됨" : "연결 대기")
                + "\n● 네이버지도 알림 감지: " + observed
                + "\n● 분석 결과: " + NavNotificationService.lastNaverResult
                + "\n● HUD 전송: " + server
                + "\n● 현재 길안내: " + nav
                + "\n● 단속 정보: " + cameraStatus
                + (n.timestamp == 0 ? "\n  회전 정보가 없으면 대기로 표시합니다." : "\n  " + n.road));
        ui.postDelayed(this::refreshSender, 1500);
    }
    private boolean hasListener() {
        if (Build.VERSION.SDK_INT >= 27) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            return manager != null && manager.isNotificationListenerAccessGranted(new ComponentName(this, NavNotificationService.class));
        }
        String listeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return listeners != null && listeners.contains(getPackageName());
    }
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 205);
    }
    private void openNavigation(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) Toast.makeText(this, "내비 앱이 설치되어 있지 않거나 실행할 수 없습니다", Toast.LENGTH_LONG).show();
        else startActivity(i);
    }
    private String localIps() {
        try {
            List<String> result = new ArrayList<>();
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback()) continue;
                String name = nic.getName().toLowerCase(Locale.ROOT);
                if (name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("tun")) continue;
                for (java.net.InetAddress a : Collections.list(nic.getInetAddresses()))
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) result.add(name + "  " + a.getHostAddress());
            }
            return result.isEmpty() ? "Wi-Fi/핫스팟 연결 후 다시 확인" : android.text.TextUtils.join("\n", result);
        } catch (Exception ex) { return "현재 IP를 읽지 못했습니다"; }
    }
    private String gateway() {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            DhcpInfo dhcp = wifi == null ? null : wifi.getDhcpInfo();
            if (dhcp == null || dhcp.gateway == 0) return "";
            int g = dhcp.gateway;
            return (g & 255) + "." + ((g >> 8) & 255) + "." + ((g >> 16) & 255) + "." + ((g >> 24) & 255);
        } catch (Exception e) { return ""; }
    }
    private void showHudSetup() {
        LinearLayout root = page("HUD DISPLAY", "공기계 HUD", "메인폰과 같은 Wi-Fi에 연결하고 화면을 보정합니다.");
        screen = "setup";
        SharedPreferences prefs = getSharedPreferences("receiver", MODE_PRIVATE);
        LinearLayout connect = card(root);
        label(connect, "메인폰 연결", 19, WHITE, true);
        label(connect, "IP 주소", 13, MUTED, false);
        ipInput = edit(connect, prefs.getString("ip", gateway()), "예: 192.168.43.1", false);
        gap(connect, 6);
        label(connect, "8자리 연결 PIN", 13, MUTED, false);
        pinInput = edit(connect, prefs.getString("pin", ""), "메인폰 PIN", true);
        button(connect, "연결 상태 테스트", false, this::testConnection);
        diagnosisInfo = label(connect, "연결 상태를 확인할 수 있습니다.", 13, MUTED, false);
        LinearLayout display = card(root);
        label(display, "화면·속도 설정", 19, WHITE, true);
        button(display, "GPS 속도계 권한 설정", false, this::requestGps);
        twoButtons(display, "반사형: " + (mirror ? "켜짐" : "꺼짐"), () -> {
            mirror = !mirror; saveUiPrefs(); showHudSetup();
        }, "색상: " + themeName(), () -> {
            theme = (theme + 1) % 3; saveUiPrefs(); showHudSetup();
        });
        button(display, "OLED 보호(주기적 픽셀 이동): " + (oledMode ? "켜짐" : "꺼짐"), false, () -> {
            oledMode = !oledMode; saveUiPrefs(); showHudSetup();
        });
        label(display, "V30 기본값: OLED 보호 켜짐 · 낮은 밝기 · 앞유리 좌우반전 켜짐. 픽셀 이동은 번인 위험을 줄이는 보조 수단이며 완전 예방은 아닙니다.", 12, MUTED, false);
        button(display, "HUD 밝기: " + brightnessName() + " (눌러 변경)", false, () -> {
            brightnessLevel = (brightnessLevel + 1) % 3; saveUiPrefs(); showHudSetup();
        });
        button(root, "HUD 시작  →", true, () -> {
            String ip = ipInput.getText().toString().trim();
            String code = pinInput.getText().toString().trim();
            if (!isIpv4(ip) || !code.matches("[0-9]{8}")) {
                Toast.makeText(this, "사설 Wi-Fi/핫스팟 IP와 8자리 PIN을 확인하세요", Toast.LENGTH_LONG).show(); return;
            }
            prefs.edit().putString("ip", ip).putString("pin", code).apply();
            showHud(false);
        });
        button(root, "분기 그림·카메라 9종 미리보기", false, () -> showHud(true));
        LinearLayout guide = card(root);
        label(guide, "주행 전 확인", 19, WHITE, true);
        label(guide, "• 그림은 실제 차로와 동일하지 않습니다. 도로 표지판/네이버지도가 우선입니다.\n• OLED 밝기는 낮음 권장, 번인 방지 보장 안 됨.\n• GPS 미수신 시 속도는 --로 표시됩니다.\n• 단속 정보/제한속도도 알림에 없으면 표시되지 않습니다.", 14, MUTED, false);
        button(root, "← 처음으로", false, this::showHome);
    }
    private String themeName() { return theme == 1 ? "호박색" : theme == 2 ? "초록색" : "청록색"; }
    private String brightnessName() { return brightnessLevel == 0 ? "낮음" : brightnessLevel == 2 ? "높음" : "보통"; }
    private void saveUiPrefs() {
        getPreferences(MODE_PRIVATE).edit().putBoolean("mirror", mirror)
                .putInt("theme", theme).putInt("brightness", brightnessLevel)
                .putBoolean("oledMode", oledMode).apply();
    }
    private void requestGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
        } else Toast.makeText(this, "GPS 위치 권한이 허용되어 있습니다", Toast.LENGTH_SHORT).show();
    }
    private static boolean isIpv4(String ip) {
        return LanAddress.allowed(ip);
    }
    private interface FetchCallback { void done(NavData nav, CameraData camera, String message, boolean success); }
    private void fetch(String ip, String code, FetchCallback cb) {
        HttpURLConnection connection = null;
        try {
            if (!LanAddress.allowed(ip) || code == null || !code.matches("[0-9]{8}")) {
                cb.done(null, null, "사설 IP 또는 PIN 형식 오류", false); return;
            }
            URL url = new URL("http://" + ip + ":" + HudSenderService.PORT + "/status");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(1800); connection.setReadTimeout(1800);
            connection.setRequestMethod("GET"); connection.setRequestProperty("X-HUD-PIN", code);
            connection.setUseCaches(false);
            int response = connection.getResponseCode();
            if (response == 403) { cb.done(null, null, "PIN이 맞지 않습니다", false); return; }
            if (response != 200) { cb.done(null, null, "서버 오류 HTTP " + response, false); return; }
            StringBuilder body = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = in.readLine()) != null) {
                    body.append(line);
                    if (body.length() > 4096) throw new Exception("응답 초과");
                }
            }
            JSONObject json = new JSONObject(body.toString());
            java.util.Map<String, Object> values = new java.util.HashMap<>();
            java.util.Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object v = json.opt(key);
                values.put(key, v);
            }
            HudWire.Snapshot snapshot = HudWire.decode(values, System.currentTimeMillis());
            if (!snapshot.compatible) { cb.done(null, null, "앱 버전이 다릅니다: 두 폰에 2.6 설치", false); return; }
            NavData data = snapshot.navigation;
            CameraData camera = snapshot.camera;
            String label = LinkHealth.message(true, snapshot.notificationListenerReady,
                    snapshot.lastNaverAgeMs, !"WAIT".equals(data.direction), camera.present());
            cb.done(data, camera, label, true);
        } catch (Exception e) { cb.done(null, null, "연결 실패 · Wi-Fi/IP/송신 확인", false); }
        finally { if (connection != null) connection.disconnect(); }
    }
    private void testConnection() {
        String ip = ipInput.getText().toString().trim();
        String code = pinInput.getText().toString().trim();
        if (!isIpv4(ip) || !code.matches("[0-9]{8}")) {
            diagnosisInfo.setText("사설 IP(10.x / 172.16-31.x / 192.168.x)와 8자리 PIN을 확인하세요."); return;
        }
        diagnosisInfo.setText("메인폰에 연결 중…");
        diagnostics.execute(() -> fetch(ip, code, (nav, camera, message, okay) ->
                ui.post(() -> { if ("setup".equals(screen) && diagnosisInfo != null)
                    diagnosisInfo.setText((okay ? "● " : "! ") + message); })));
    }
    private void showHud(boolean preview) {
        stopReceiver();
        screen = "hud"; demo = preview;
        remote = NavData.empty(); remoteCamera = CameraData.empty(); demoScene = 0; linkOkay = false; linkText = preview ? "화면 미리보기" : "연결 중";
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(5894 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        WindowManager.LayoutParams attr = getWindow().getAttributes();
        attr.screenBrightness = hudBrightness();
        getWindow().setAttributes(attr);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hud = new HudCanvas(this, new HudCanvas.Listener() {
            @Override public void onExit() { showHudSetup(); }
            @Override public void onNextDemo() { if (demo) { demoScene = (demoScene + 1) % HudDemo.NAMES.length; drawHud(); } }
            @Override public void onToggleMirror() { mirror = !mirror; saveUiPrefs(); drawHud(); }
            @Override public void onNextTheme() { theme = (theme + 1) % 3; saveUiPrefs(); drawHud(); }
            @Override public void onBrightnessCycle() {
                brightnessLevel = (brightnessLevel + 1) % 3; saveUiPrefs();
                WindowManager.LayoutParams p = getWindow().getAttributes();
                p.screenBrightness = hudBrightness();
                getWindow().setAttributes(p); drawHud();
            }
        });
        setContentView(hud);
        startGps(); if (!preview) startPolling();
        drawHud();
    }
    private void drawHud() {
        if (hud == null || !"hud".equals(screen)) return;
        int shownSpeed = System.currentTimeMillis() - speedUpdatedAt > 8000 ? -1 : speed;
        NavData navShown = demo ? HudDemo.navigation(demoScene) : remote;
        CameraData cameraShown = demo ? HudDemo.camera(demoScene) : remoteCamera;
        hud.update(navShown, cameraShown, linkOkay, shownSpeed, mirror, demo, linkText, theme, brightnessLevel, oledMode);
    }
    private void startGps() {
        speed = -1; speedUpdatedAt = 0L; lastSpeeds = new int[] {-1, -1, -1}; speedCursor = 0;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override public void onLocationChanged(Location location) {
                    if (!"hud".equals(screen)) return;
                    if (location.hasSpeed()) {
                        lastSpeeds[speedCursor++ % lastSpeeds.length] = Math.max(0, Math.round(location.getSpeed() * 3.6f));
                        int sum = 0, count = 0;
                        for (int v : lastSpeeds) if (v >= 0) { sum += v; count++; }
                        speed = count == 0 ? -1 : Math.round((float) sum / count);
                    } else speed = -1;
                    speedUpdatedAt = System.currentTimeMillis(); drawHud();
                }
                @Override public void onProviderDisabled(String provider) { speed = -1; drawHud(); }
            };
            if (locationManager != null) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener);
        } catch (Exception e) { speed = -1; }
    }
    private void startPolling() {
        SharedPreferences prefs = getSharedPreferences("receiver", MODE_PRIVATE);
        final String ip = prefs.getString("ip", "");
        final String code = prefs.getString("pin", "");
        final int stamp = ++session;
        polling = Executors.newSingleThreadScheduledExecutor();
        polling.scheduleWithFixedDelay(() -> fetch(ip, code, (nav, camera, message, ok) -> ui.post(() -> {
            if (!"hud".equals(screen) || stamp != session) return;
            linkOkay = ok; linkText = message;
            remote = ok && nav != null ? nav : NavData.empty();
            remoteCamera = ok && camera != null ? camera : CameraData.empty();
            drawHud();
        })), 0, 1100, TimeUnit.MILLISECONDS);
    }
    private void stopReceiver() {
        session++;
        if (polling != null) { polling.shutdownNow(); polling = null; }
        if (locationManager != null && locationListener != null) {
            try { locationManager.removeUpdates(locationListener); } catch (Exception ignored) { }
        }
        locationListener = null; locationManager = null; hud = null;
    }
    @Override public void onBackPressed() {
        if ("hud".equals(screen)) showHudSetup();
        else if ("home".equals(screen)) super.onBackPressed();
        else showHome();
    }
    @Override protected void onDestroy() {
        stopReceiver(); diagnostics.shutdownNow(); ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
