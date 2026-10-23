package com.example.phonehud;

/** Only guidance explicitly obtained from NAVER notification text. */
public final class NavData {
    public final String direction;
    public final String distance;
    public final String road;
    public final String source;
    public final long timestamp;
    public final String key;
    public final String diagram; // TURN, TUNNEL_LEFT/RIGHT, FORK_LEFT/RIGHT, EXIT_LEFT/RIGHT, etc.

    public NavData(String direction, String distance, String road, String source, long timestamp, String key) {
        this(direction, distance, road, source, timestamp, key, direction);
    }
    public NavData(String direction, String distance, String road, String source, long timestamp, String key, String diagram) {
        this.direction = direction;
        this.distance = distance;
        this.road = road;
        this.source = source;
        this.timestamp = timestamp;
        this.key = key == null ? "" : key;
        this.diagram = diagram == null ? direction : diagram;
    }
    public static NavData empty() { return new NavData("WAIT", "--", "길안내 정보 대기", "", 0L, "", "WAIT"); }
}
