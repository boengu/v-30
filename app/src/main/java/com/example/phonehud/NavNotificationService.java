package com.example.phonehud;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** Only handles NAVER Map notifications; no messages or notifications from other apps. */
public final class NavNotificationService extends NotificationListenerService {
    public static volatile boolean listenerConnected;
    public static volatile long lastNaverNotificationAt;
    public static volatile String lastNaverResult = "네이버지도 알림 대기";

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        listenerConnected = true;
        try {
            StatusBarNotification[] all = getActiveNotifications();
            if (all != null) for (StatusBarNotification sbn : all) receive(sbn);
        } catch (Exception ignored) { }
    }
    @Override public void onListenerDisconnected() {
        listenerConnected = false;
        NavStore.clear();
        super.onListenerDisconnected();
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
        if (n == null || n.extras == null) return;
        Bundle e = n.extras;
        String title = text(e.getCharSequence(Notification.EXTRA_TITLE));
        StringBuilder body = new StringBuilder();
        append(body, e.getCharSequence(Notification.EXTRA_BIG_TEXT));
        append(body, e.getCharSequence(Notification.EXTRA_TEXT));
        CharSequence[] lines = e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null) for (CharSequence line : lines) append(body, line);
        append(body, e.getCharSequence(Notification.EXTRA_SUB_TEXT));
        String fullBody = body.length() > 2048 ? body.substring(0, 2048) : body.toString();
        if (title.length() > 256) title = title.substring(0, 256);
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
            lastNaverResult = camera == null ? "알림 확인됨 · 회전/단속 정보 없음" : "카메라 인식: " + camera.label() + " / " + camera.distance;
        }
    }
    private static void append(StringBuilder b, CharSequence value) {
        if (value == null || value.length() == 0) return;
        if (b.length() > 0) b.append(' ');
        b.append(value);
    }
    private static String text(CharSequence c) { return c == null ? "" : c.toString(); }
}
