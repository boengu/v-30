package com.example.phonehud;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Only parses a turn+distance text pair explicitly present in NAVER's notification.
 * It cannot OCR an icon, read a status-bar chip, or derive turns from GPS.
 */
public final class NaverChipParser {
    private static final Pattern CHIP = Pattern.compile(
            "(↰|↱|↶|↷|↖|↗|←|→|↑|⬅|➡|⬆)\\s*" +
            "([0-9]{1,4}(?:[.,][0-9]{1,2})?)\\s*(km|킬로미터|미터|m)(?![\\p{L}/])",
            Pattern.CASE_INSENSITIVE);
    private NaverChipParser() { }

    public static NavData parse(String pkg, String title, String body, long at, String key) {
        if (!NaverNavParser.isNaver(pkg)) return null;
        String all = clean(title) + " " + clean(body);
        Matcher match = CHIP.matcher(all);
        if (!match.find()) return null;
        String arrow = match.group(1);
        String dir = ("↰↶↖←⬅".contains(arrow)) ? "LEFT" :
                (("↱↷↗→➡".contains(arrow)) ? "RIGHT" : "STRAIGHT");
        String unit = match.group(3).toLowerCase(Locale.ROOT);
        String amount = match.group(2).replace(',', '.') +
                ((unit.equals("km") || unit.equals("킬로미터")) ? "km" : "m");
        String road = clean(body).isEmpty() ? clean(title) : clean(body);
        if (road.length() > 76) road = road.substring(0, 75) + "…";
        return new NavData(dir, amount, road, "네이버지도", at, key, dir);
    }
    private static String clean(String s) {
        return s == null ? "" : s.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
