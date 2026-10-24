package com.example.phonehud;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort parser for text visible in NAVER Map Android notifications.
 * This is NOT an official NAVER navigation SDK or a guaranteed live-turn API.
 * The app must fail closed when a usable maneuver is not present.
 */
public final class NaverNavParser {
    public static final String PACKAGE = "com.nhn.android.nmap";
    public static final long MAX_AGE_MS = 30000L;
    private static final Pattern TURN = Pattern.compile(
            "유턴|U\\s*[- ]?턴|회전\\s*교차로|로터리|좌회전|우회전|좌측\\s*방향|우측\\s*방향|왼쪽\\s*방향|오른쪽\\s*방향|왼쪽|오른쪽|좌측|우측|직진|계속\\s*진행|직진\\s*방향|좌측\\s*진입|우측\\s*진입",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DISTANCE = Pattern.compile(
            "(?<![\\d.])([0-9]+(?:[.,][0-9]+)?)\\s*(km|킬로미터|미터|m)(?![\\p{L}/])",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern AD = Pattern.compile("쿠폰|프로모션|이벤트|할인|포인트\\s*적립|리뷰\\s*이벤트|광고|예약\\s*혜택");
    private static final Pattern RESET = Pattern.compile("경로\\s*재탐색|경로를\\s*벗어|경로\\s*이탈|길안내\\s*종료|안내를\\s*종료|주행\\s*종료|운행\\s*종료");
    private static final Pattern ARRIVED = Pattern.compile("목적지에\\s*도착|목적지\\s*도착했습니다|안내가\\s*종료되었습니다");

    private NaverNavParser() { }
    public static boolean isNaver(String packageName) { return PACKAGE.equals(packageName); }
    public static boolean isReset(String title, String body) {
        String full = compact(title + " " + body);
        return RESET.matcher(full).find();
    }

    public static NavData parse(String pkg, String title, String body, long timestamp, String key) {
        if (!isNaver(pkg)) return null;
        String a = compact(title), b = compact(body);
        String full = (a + " " + b).trim();
        if (full.isEmpty() || AD.matcher(full).find() || RESET.matcher(full).find()) return null;
        if (ARRIVED.matcher(full).find())
            return new NavData("ARRIVE", "--", "목적지 도착", "네이버지도", timestamp, key);
        Matcher action = TURN.matcher(full);
        if (!action.find()) {
            // Some navigation notifications expose a turn symbol and distance as text.
            // This works only if Android actually exposes the symbol to the listener.
            return NaverChipParser.parse(pkg, a, b, timestamp, key);
        }
        // Do not interpret an isolated word like "좌회전" in a place recommendation
        // as an instruction unless it occurs in navigation-like context.
        if (!DISTANCE.matcher(full).find() &&
                !Pattern.compile("전방|다음|잠시|직후|\\s후|진입|도로|교차로|로터리|안내|차로|방향|하세요|턴하세요|도착").matcher(full).find())
            return null;
        String maneuver = action.group().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        String direction;
        if (maneuver.contains("유턴") || maneuver.contains("u턴") || maneuver.contains("u-턴")) direction = "UTURN";
        else if (maneuver.contains("회전교차로") || maneuver.contains("로터리")) direction = "ROUND";
        else if (maneuver.contains("좌") || maneuver.contains("왼쪽")) direction = "LEFT";
        else if (maneuver.contains("우") || maneuver.contains("오른쪽")) direction = "RIGHT";
        else direction = "STRAIGHT";
        // Associate distance with the FIRST maneuver, not a subsequent one.
        // Example: "300m 후 우회전, 다음 2km 후 좌회전" must show RIGHT/300m.
        Matcher dist = DISTANCE.matcher(full);
        int bestGap = Integer.MAX_VALUE;
        String nearest = "--";
        while (dist.find()) {
            int gap = dist.end() <= action.start() ? action.start() - dist.end() : dist.start() - action.end();
            if (gap < 0) gap = 0;
            // Ignore far-away unrelated metrics and distances attached to later maneuvers.
            if (gap > 32) continue;
            if (dist.start() > action.end()) {
                Matcher laterTurn = TURN.matcher(full);
                if (laterTurn.find(action.end()) && laterTurn.start() < dist.start()) continue;
                String between = full.substring(action.end(), dist.start());
                // "우회전 후 100m 좌회전": 100m belongs to the next turn.
                if (between.matches("(?s).*후\\s*")) continue;
            }
            if (gap < bestGap) {
                bestGap = gap;
                String unit = dist.group(2).toLowerCase(Locale.ROOT);
                nearest = dist.group(1).replace(',', '.') +
                        ("km".equals(unit) || "킬로미터".equals(unit) ? "km" : "m");
            }
        }
        String road = !b.isEmpty() ? b : a;
        if (road.length() > 76) road = road.substring(0, 75) + "…";
        String localClause = full.substring(Math.max(0, action.start() - 42), Math.min(full.length(), action.end() + 16));
        // Limit the schematic to the clause around the FIRST maneuver.
        String diagram = DiagramClassifier.select(localClause, direction);
        return new NavData(direction, nearest, road, "네이버지도", timestamp, key, diagram);
    }
    private static String compact(String text) {
        return text == null ? "" : text.replace('\r', ' ').replace('\n', ' ')
                .replaceAll("\\s+", " ").trim();
    }
}
