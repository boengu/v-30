package com.example.phonehud;

import java.util.HashMap;
import java.util.Map;

/**
 * Versioned, testable local-wire contract shared by sender and HUD receiver.
 * Map contains JSON-compatible primitive values only; transport is HTTPS-free LAN HTTP.
 * Values not present in NAVER notifications are never estimated.
 */
public final class HudWire {
    public static final int PROTOCOL = 6;
    private HudWire() { }

    public static final class Snapshot {
        public final NavData navigation;
        public final CameraData camera;
        public final boolean compatible;
        public final boolean notificationListenerReady;
        public final long lastNaverAgeMs;
        Snapshot(NavData n, CameraData c, boolean valid, boolean listenerReady, long naverAge) {
            navigation = n; camera = c; compatible = valid;
            notificationListenerReady = listenerReady; lastNaverAgeMs = naverAge;
        }
    }

    public static Map<String, Object> encode(NavData navigation, CameraData warning, long now) {
        return encode(navigation, warning, now, false, 0L);
    }
    public static Map<String, Object> encode(NavData navigation, CameraData warning, long now,
                                              boolean listenerReady, long lastNaverEventAt) {
        NavData n = navigation == null ? NavData.empty() : navigation;
        CameraData c = warning == null ? CameraData.empty() : warning;
        long navMax = "DEMO".equals(n.source) ? 20000L : NaverNavParser.MAX_AGE_MS;
        long age = age(n.timestamp, now, navMax);
        long cameraAge = age(c.timestamp, now, CameraData.MAX_AGE_MS);
        if (age < 0) n = NavData.empty();
        if (cameraAge < 0 || !c.present()) { c = CameraData.empty(); cameraAge = -1L; }
        Map<String,Object> fields = new HashMap<>();
        fields.put("protocol", PROTOCOL);
        fields.put("direction", n.direction);
        fields.put("distance", n.distance);
        fields.put("road", n.road);
        fields.put("diagram", n.diagram);
        fields.put("source", n.source);
        fields.put("ageMs", age);
        fields.put("cameraType", c.type);
        fields.put("cameraDistance", c.distance);
        fields.put("speedLimit", c.limitKmh);
        fields.put("cameraAgeMs", cameraAge);
        fields.put("demo", "DEMO".equals(n.source));
        fields.put("serverTime", now);
        fields.put("listenerReady", listenerReady);
        // Age-only telemetry: no raw notification contents, titles, or device identifiers.
        fields.put("lastNaverAgeMs", lastNaverEventAt > 0 && lastNaverEventAt <= now
                ? now - lastNaverEventAt : -1L);
        return fields;
    }

    /** Fail closed on malformed/obsolete message values; guidance and camera expire separately. */
    public static Snapshot decode(Map<String, ?> fields, long now) {
        if (fields == null || number(fields.get("protocol"), -1L) != PROTOCOL)
            return new Snapshot(NavData.empty(), CameraData.empty(), false, false, -1L);
        String source = str(fields.get("source"), 20);
        long navMax = "DEMO".equals(source) ? 20000L : NaverNavParser.MAX_AGE_MS;
        long navAge = number(fields.get("ageMs"), -1L);
        NavData n = NavData.empty();
        if (navAge >= 0 && navAge <= navMax && now > navAge) {
            String direction = str(fields.get("direction"), 16);
            if (validDirection(direction) && !"WAIT".equals(direction)) {
                String diagram = str(fields.get("diagram"), 32);
                if (!validDiagram(diagram)) diagram = direction;
                // A schematic branch must not contradict the underlying maneuver.
                if (diagram.contains("_") &&
                    !((diagram.endsWith("_LEFT") && "LEFT".equals(direction)) ||
                      (diagram.endsWith("_RIGHT") && "RIGHT".equals(direction)))) diagram = direction;
                // Plain symbols must agree with the recognized instruction.
                if (!diagram.contains("_") && !diagram.equals(direction)) diagram = direction;
                n = new NavData(direction, str(fields.get("distance"), 16),
                        str(fields.get("road"), 85), source, now - navAge, "", diagram);
            }
        }
        CameraData c = CameraData.empty();
        long ca = number(fields.get("cameraAgeMs"), -1L);
        String type = str(fields.get("cameraType"), 20);
        if (ca >= 0 && ca <= CameraData.MAX_AGE_MS && now > ca && validCamera(type)) {
            long rawLimit = number(fields.get("speedLimit"), -1);
            int limit = rawLimit >= 20 && rawLimit <= 130 ? (int) rawLimit : -1;
            c = new CameraData(type, str(fields.get("cameraDistance"), 16), limit, now - ca, "");
        }
        boolean ready = Boolean.TRUE.equals(fields.get("listenerReady"));
        long lastAge = number(fields.get("lastNaverAgeMs"), -1L);
        if (lastAge < 0L || lastAge > 86400000L) lastAge = -1L;
        return new Snapshot(n, c, true, ready, lastAge);
    }

    private static long age(long at, long now, long max) {
        long a = now - at;
        return at <= 0L || a < 0L || a > max ? -1L : a;
    }
    private static String str(Object o, int max) {
        if (!(o instanceof String)) return "";
        String s = ((String) o).replaceAll("[\\p{Cntrl}]", " ");
        return s.length() <= max ? s : s.substring(0, max);
    }
    private static long number(Object o, long fallback) {
        return o instanceof Number ? ((Number) o).longValue() : fallback;
    }
    private static boolean validDirection(String s) {
        return s.equals("WAIT") || s.equals("LEFT") || s.equals("RIGHT") ||
               s.equals("STRAIGHT") || s.equals("UTURN") || s.equals("ROUND") || s.equals("ARRIVE");
    }
    private static boolean validDiagram(String s) {
        return validDirection(s) || s.equals("TUNNEL_LEFT") || s.equals("TUNNEL_RIGHT") ||
               s.equals("FORK_LEFT") || s.equals("FORK_RIGHT") ||
               s.equals("EXIT_LEFT") || s.equals("EXIT_RIGHT");
    }
    private static boolean validCamera(String s) {
        return s.equals("SPEED") || s.equals("RED_LIGHT") || s.equals("COMBINED") ||
               s.equals("SECTION") || s.equals("SECTION_START") || s.equals("SECTION_END") ||
               s.equals("MOBILE") || s.equals("OTHER");
    }
}
