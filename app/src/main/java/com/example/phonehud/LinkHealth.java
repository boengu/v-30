package com.example.phonehud;

/** Connection and NAVER notification availability are independent signals. */
public final class LinkHealth {
    public static final long NO_RECENT_EVENT_MS = 120000L;
    private LinkHealth() { }
    public static String message(boolean connected, boolean listenerReady, long naverAgeMs,
                                 boolean hasNavigation, boolean hasCamera) {
        if (!connected) return "메인폰 연결 실패";
        if (!listenerReady) return "메인폰 알림 접근 확인 필요";
        if (hasNavigation && hasCamera) return "길안내 및 카메라 수신";
        if (hasNavigation) return "길안내 수신 중";
        if (hasCamera) return "카메라 안내 수신 중";
        if (naverAgeMs < 0L) return "네이버지도 알림 미수신";
        if (naverAgeMs > NO_RECENT_EVENT_MS) return "네이버지도 알림 갱신 안 됨";
        return "알림 수신 · 길안내 내용 없음";
    }
}
