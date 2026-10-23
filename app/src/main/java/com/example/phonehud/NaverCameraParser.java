package com.example.phonehud;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Best-effort, notification-only camera parser; does not infer live road cameras. */
public final class NaverCameraParser {
    private static final Pattern CAMERA = Pattern.compile("과속\\s*단속|과속\\s*카메라|속도\\s*단속|신호\\s*단속|신호위반\\s*단속|단속\\s*카메라|구간\\s*단속|이동식\\s*단속|고정식\\s*단속|무인\\s*단속|과속\\s*감시");
    private static final Pattern DIST = Pattern.compile("(?<![\\d.])([0-9]+(?:[.,][0-9]+)?)\\s*(km|킬로미터|미터|m)(?![\\p{L}/])", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIMIT = Pattern.compile("(?:제한\\s*속도|제한속도|제한|규정\\s*속도)\\s*[:：]?\\s*(\\d{2,3})\\s*(?:km\\s*/\\s*h|kmh|킬로미터|키로)?|(?:^|\\s)(\\d{2,3})\\s*km\\s*/\\s*h\\s*(?:제한|도로)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROMO = Pattern.compile("쿠폰|프로모션|할인|광고|예약\\s*혜택|포인트\\s*적립");
    private NaverCameraParser() { }

    public static CameraData parse(String pkg, String title, String body, long timestamp, String key) {
        if (!NaverNavParser.isNaver(pkg)) return null;
        String text = ((title == null ? "" : title) + " " + (body == null ? "" : body)).replaceAll("\\s+", " ").trim();
        if (text.isEmpty() || PROMO.matcher(text).find() || NaverNavParser.isReset(title, body)) return null;
        Matcher hit = CAMERA.matcher(text);
        if (!hit.find()) return null;
        int index = hit.start();
        boolean section = text.contains("구간단속") || text.contains("구간 단속");
        String type;
        if (section) {
            boolean start = Pattern.compile("시작|진입|시점").matcher(text).find();
            boolean end = Pattern.compile("종료|끝|종점").matcher(text).find();
            type = start && !end ? "SECTION_START" : end && !start ? "SECTION_END" : "SECTION";
        } else if (text.matches("(?s).*(?:신호\\s*[/·및+&]\\s*과속|신호\\s*및\\s*과속|과속\\s*[/·및+&]\\s*신호).*")
                || (text.contains("신호") && text.contains("과속"))) type = "COMBINED";
        else if (text.contains("신호") || text.contains("신호위반")) type = "RED_LIGHT";
        else if (text.contains("이동식")) type = "MOBILE";
        else if (text.contains("과속") || text.contains("속도단속") || text.contains("속도 단속")) type = "SPEED";
        else type = "OTHER";
        int limit = -1;
        Matcher lm = LIMIT.matcher(text);
        if (lm.find()) {
            String number = lm.group(1) != null ? lm.group(1) : lm.group(2);
            try { int value = Integer.parseInt(number); if (value >= 20 && value <= 130) limit = value; }
            catch (NumberFormatException ignored) { }
        }
        Matcher dm = DIST.matcher(text);
        String near = "--";
        int best = Integer.MAX_VALUE;
        while (dm.find()) {
            // A range value near a different word is not automatically a camera distance.
            if (lm.find(0) && dm.start() >= lm.start() && dm.start() <= lm.end()) continue;
            int gap = dm.end() <= index ? index - dm.end() : dm.start() - hit.end();
            if (gap < 0) gap = 0;
            if (gap > 38 || gap >= best) continue;
            String between = dm.end() <= index ? text.substring(dm.end(), index) : text.substring(hit.end(), dm.start());
            if (between.matches("(?s).*(?:다음|그다음|이후).*") && dm.start() > hit.end()) continue;
            best = gap;
            near = dm.group(1).replace(',', '.') + (dm.group(2).equalsIgnoreCase("km") || dm.group(2).equals("킬로미터") ? "km" : "m");
        }
        return new CameraData(type, near, limit, timestamp, key);
    }
}
