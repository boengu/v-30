package com.example.phonehud;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Interprets only notification text already made public by an allow-listed navigation app. */
public final class NavParser {
    private NavParser() { }
    private static final Pattern DISTANCE = Pattern.compile(
            "(?<![\\d.])([0-9]+(?:[.,][0-9]+)?)\\s*(km|킬로미터|미터|m)(?![a-zA-Z/])", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROAD_HINT = Pattern.compile("(도로|방면|사거리|교차로|고속도로|터널|방향|진입|갈림길|램프)");
    public static boolean isNavigationPackage(String pkg) {
        return "com.skt.tmap.ku".equals(pkg) || "com.locnall.KimGiSa".equals(pkg)
                || "com.nhn.android.nmap".equals(pkg) || "com.google.android.apps.maps".equals(pkg);
    }
    public static String appName(String pkg) {
        if ("com.skt.tmap.ku".equals(pkg)) return "T맵";
        if ("com.locnall.KimGiSa".equals(pkg)) return "카카오내비";
        if ("com.nhn.android.nmap".equals(pkg)) return "네이버지도";
        if ("com.google.android.apps.maps".equals(pkg)) return "구글지도";
        return "내비";
    }
    public static NavData parse(String pkg, String title, String detail, long now, String key) {
        if (!isNavigationPackage(pkg)) return null;
        String a = clean(title), b = clean(detail);
        String full = (a + " " + b).trim();
        if (full.isEmpty()) return null;
        String s = full.toLowerCase(Locale.ROOT);
        String direction = "WAIT";
        if (has(s, "목적지에 도착", "도착했습니다", "목적지 도착", "arrived at", "destination reached")) direction = "ARRIVE";
        else if (has(s, "유턴", "u-turn", "uturn")) direction = "UTURN";
        else if (has(s, "회전교차로", "로터리", "roundabout")) direction = "ROUND";
        else if (has(s, "좌회전", "왼쪽", "좌측", "turn left", "keep left", "bear left")) direction = "LEFT";
        else if (has(s, "우회전", "오른쪽", "우측", "turn right", "keep right", "bear right")) direction = "RIGHT";
        else if (has(s, "직진", "계속 진행", "go straight", "continue straight", "head straight")) direction = "STRAIGHT";
        Matcher m = DISTANCE.matcher(full);
        String distance = "--";
        if (m.find()) {
            String unit = m.group(2).toLowerCase(Locale.ROOT);
            distance = m.group(1).replace(',', '.') + ((unit.equals("km") || unit.equals("킬로미터")) ? "km" : "m");
        }
        // A distance alone is insufficient to identify a navigation maneuver.
        if ("WAIT".equals(direction) && ("--".equals(distance)
                || (!has(s, "안내", "전방", "진입", "이동", "후", "turn", "exit") && !ROAD_HINT.matcher(s).find()))) return null;
        String road = !b.isEmpty() ? b : a;
        road = road.replaceAll("\\s+", " ");
        if (road.length() > 76) road = road.substring(0, 75) + "…";
        return new NavData(direction, distance, road, appName(pkg), now, key);
    }
    private static boolean has(String s, String... list) { for (String v : list) if (s.contains(v)) return true; return false; }
    private static String clean(String s) { return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').trim(); }
}
