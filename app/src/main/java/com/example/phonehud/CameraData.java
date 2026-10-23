package com.example.phonehud;

/** A camera warning comes ONLY from text actually present in a NAVER notification. */
public final class CameraData {
    public static final long MAX_AGE_MS = 20000L;
    public final String type; // SPEED, RED_LIGHT, COMBINED, SECTION_START, SECTION_END, SECTION, MOBILE, OTHER
    public final String distance; // Parsed camera distance, never synthesized or estimated
    public final int limitKmh; // -1 means unknown
    public final long timestamp;
    public final String key;

    public CameraData(String type, String distance, int limitKmh, long timestamp, String key) {
        this.type = type;
        this.distance = distance;
        this.limitKmh = limitKmh;
        this.timestamp = timestamp;
        this.key = key == null ? "" : key;
    }
    public static CameraData empty() { return new CameraData("NONE", "--", -1, 0L, ""); }
    public boolean present() { return timestamp > 0 && !"NONE".equals(type); }
    public String label() {
        switch (type) {
            case "SPEED": return "과속 단속";
            case "RED_LIGHT": return "신호 단속";
            case "COMBINED": return "신호·과속 단속";
            case "SECTION_START": return "구간단속 시작";
            case "SECTION_END": return "구간단속 종료";
            case "SECTION": return "구간단속";
            case "MOBILE": return "이동식 단속";
            case "OTHER": return "단속 카메라";
            default: return "카메라 정보 없음";
        }
    }
}
