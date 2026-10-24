package com.example.phonehud;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** Only handles NAVER Map notifications; no messages or notifications from other apps. */
public final class NavNotificationService extends NotificationListenerService {
    private static volatile NavNotificationService activeListener;
    public static volatile String lastManualScanResult = "검사 전 · 메인폰에서 검사 버튼을 눌러주세요";
    public static volatile boolean listenerConnected;
    public static volatile long lastNaverNotificationAt;
    public static volatile String lastNaverResult = "네이버지도 알림 대기";

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        activeListener = this;
        listenerConnected = true;
        try {
            StatusBarNotification[] all = getActiveNotifications();
            if (all != null) for (StatusBarNotification sbn : all) receive(sbn);
        } catch (Exception ignored) { }
    }
    @Override public void onListenerDisconnected() {
        if (activeListener == this) activeListener = null;
        listenerConnected = false;
        NavStore.clear();
        super.onListenerDisconnected();
    }
    @Override public void onDestroy() {
        if (activeListener == this) activeListener = null;
        listenerConnected = false;
        super.onDestroy();
    }

    /**
     * On-demand local diagnostic. Does not refresh navigation timestamps or
     * claim that an unchanged active notification contains live navigation data.
     * Reads NAVER Map only, without sending raw text across the LAN.
     */
    public static String scanActiveNow() {
        NavNotificationService service = activeListener;
        if (!listenerConnected || service == null) {
            lastManualScanResult = NotificationProbe.summary(false, 0, false, false);
            return lastManualScanResult;
        }
        try {
            StatusBarNotification[] all = service.getActiveNotifications();
            int count = 0;
            boolean maneuver = false, camera = false;
            String preview = "";
            if (all != null) for (StatusBarNotification sbn : all) {
                if (sbn == null || !NaverNavParser.isNaver(sbn.getPackageName())) continue;
                count++;
                NaverNotificationText data = NaverNotificationText.read(sbn.getNotification());
                if (NaverNavParser.parse(sbn.getPackageName(), data.title, data.body, 1L, sbn.getKey()) != null)
                    maneuver = true;
                if (NaverCameraParser.parse(sbn.getPackageName(), data.title, data.body, 1L, sbn.getKey()) != null)
                    camera = true;
                if (preview.isEmpty()) {
                    preview = "텍스트항목: " + data.fields + " · 사용자정의화면: "
                            + (data.customView ? "있음" : "없음")
                            + "\n· 읽힌 문구: " + shortPreview(data.title + " " + data.body);
                }
            }
            lastManualScanResult = NotificationProbe.summary(true, count, maneuver, camera)
                    + (preview.isEmpty() ? "" : "\n· 알림 문구 예시(메인폰만 표시): " + preview);
        } catch (SecurityException ex) {
            lastManualScanResult = "알림 읽기 권한 오류 · 메인폰에서 알림 접근을 다시 확인하세요";
        } catch (Exception ex) {
            lastManualScanResult = "알림 검사 오류: " + ex.getClass().getSimpleName();
        }
        return lastManualScanResult;
    }

    private static String shortPreview(String raw) {
        if (raw == null) return "";
        String clean = raw.replaceAll("[\\r\\n\\t]+", " ").replaceAll(" +", " ").trim();
        return clean.length() > 110 ? clean.substring(0, 110) + "…" : clean;
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) { receive(sbn); }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null && NaverNavParser.isNaver(sbn.getPackageName())) {
            NavStore.clearIfKey(sbn.getKey());
            lastNaverResult = "네이버지도 알림 제거됨";
        }
    }

    private void receive(StatusBarNotification sbn) {
        if (sbn == null || !NaverNavParser.isNaver(sbn.getPackageName())) return;
        Notification n = sbn.getNotification();
        if (n == null) return;
        NaverNotificationText readout = NaverNotificationText.read(n);
        String title = readout.title;
        String fullBody = readout.body;
        lastNaverNotificationAt = System.currentTimeMillis();
        if (NaverNavParser.isReset(title, fullBody)) {
            NavStore.clear();
            lastNaverResult = "재탐색·안내 종료 감지: 방향 표시 해제";
            return;
        }
        NavData parsed = NaverNavParser.parse(sbn.getPackageName(), title, fullBody,
                lastNaverNotificationAt, sbn.getKey());
        CameraData camera = NaverCameraParser.parse(sbn.getPackageName(), title, fullBody,
                lastNaverNotificationAt, sbn.getKey());
        if (camera != null) NavStore.updateCamera(camera);
        else NavStore.clearCameraIfKey(sbn.getKey());
        if (parsed != null) {
            NavStore.update(parsed);
            lastNaverResult = "방향: " + parsed.diagram + " / " + parsed.distance
                    + (camera == null ? "" : " · 단속: " + camera.label());
        } else {
            // An ordinary NAVER notification must not turn into a fake maneuver.
            NavStore.clearGuidanceIfKey(sbn.getKey());
            lastNaverResult = camera == null ?
                    "알림 확인됨 · 회전/단속 정보 없음 (읽은 필드: " + readout.fields + ")" :
                    "카메라 인식: " + camera.label() + " / " + camera.distance;
        }
    }
    private static void append(StringBuilder b, CharSequence value) {
        if (value == null || value.length() == 0) return;
        if (b.length() > 0) b.append(' ');
        b.append(value);
    }
    private static String text(CharSequence c) { return c == null ? "" : c.toString(); }
}
