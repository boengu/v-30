package com.example.phonehud;

import android.app.Notification;
import android.os.Bundle;
import java.util.LinkedHashSet;

/** Bounded extraction of text fields exposed by a NAVER Maps Android notification.
 *  A custom status-bar chip may be graphical and have no readable text here.
 *  No Accessibility, screenshots, other app notifications, or external logging.
 */
public final class NaverNotificationText {
    public final String title;
    public final String body;
    public final String fields;
    public final boolean customView;

    private NaverNotificationText(String title, String body, String fields, boolean customView) {
        this.title = title;
        this.body = body;
        this.fields = fields;
        this.customView = customView;
    }

    public static NaverNotificationText read(Notification n) {
        if (n == null) return new NaverNotificationText("", "", "none", false);
        Bundle e = n.extras;
        LinkedHashSet<String> contents = new LinkedHashSet<>();
        StringBuilder names = new StringBuilder();
        String title = "";
        if (e != null) {
            title = text(e.getCharSequence(Notification.EXTRA_TITLE), 256);
            if (!title.isEmpty()) record(names, "title");
            add(contents, names, "expandedTitle", e.getCharSequence(Notification.EXTRA_TITLE_BIG));
            add(contents, names, "bigText", e.getCharSequence(Notification.EXTRA_BIG_TEXT));
            add(contents, names, "text", e.getCharSequence(Notification.EXTRA_TEXT));
            CharSequence[] lines = e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (lines != null) for (CharSequence line : lines) add(contents, names, "textLines", line);
            add(contents, names, "subText", e.getCharSequence(Notification.EXTRA_SUB_TEXT));
            add(contents, names, "summaryText", e.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
            add(contents, names, "infoText", e.getCharSequence(Notification.EXTRA_INFO_TEXT));
        }
        add(contents, names, "ticker", n.tickerText);
        StringBuilder body = new StringBuilder();
        for (String item : contents) {
            if (body.length() > 0) body.append(' ');
            if (body.length() + item.length() > 2048) break;
            body.append(item);
        }
        // Metadata helps distinguish a graphical custom notification from readable text.
        boolean custom = n.contentView != null || n.bigContentView != null || n.headsUpContentView != null;
        return new NaverNotificationText(title, body.toString(),
                names.length() == 0 ? "텍스트 필드 없음" : names.toString(), custom);
    }
    private static void add(LinkedHashSet<String> out, StringBuilder names, String field, CharSequence value) {
        String cleaned = text(value, 1024);
        if (cleaned.isEmpty()) return;
        if (out.add(cleaned)) record(names, field);
    }
    private static void record(StringBuilder names, String key) {
        if (names.length() > 0) names.append(", ");
        names.append(key);
    }
    private static String text(CharSequence v, int max) {
        if (v == null) return "";
        String s = v.toString().replaceAll("[\\r\\n\\t]+", " ").replaceAll(" +", " ").trim();
        return s.length() <= max ? s : s.substring(0, max);
    }
}
