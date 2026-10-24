package com.example.phonehud;

/** Pure Java classification of currently active NAVER Map notification snapshots. */
public final class NotificationProbe {
    private NotificationProbe() { }

    public static String summary(boolean listenerConnected, int count, boolean guidance, boolean camera) {
        if (!listenerConnected) return "알림 서비스 미연결 · 메인폰 알림 접근 권한을 확인하세요";
        if (count <= 0) return "활성 네이버지도 알림 0개 · 내비 화면과 시스템 알림은 다릅니다";
        if (guidance && camera) return "활성 네이버지도 알림에 방향 및 단속 문구 있음 (자동 갱신 여부는 별도)";
        if (guidance) return "활성 네이버지도 알림에 방향 문구 있음 (자동 갱신 여부는 별도)";
        if (camera) return "활성 네이버지도 알림에 단속 문구 있음 (자동 갱신 여부는 별도)";
        return "활성 네이버지도 알림 " + count + "개 · 회전/단속 문구 없음 · HUD에 보낼 정보 없음";
    }
}
