package com.example.phonehud;

/** Independent in-memory guidance and camera channels with fail-closed expiry. */
public final class NavStore {
    private static volatile NavData current = NavData.empty();
    private static volatile CameraData camera = CameraData.empty();
    private NavStore() { }
    public static NavData get() {
        NavData n = current;
        long maxAge = "DEMO".equals(n.source) ? 20000L : NaverNavParser.MAX_AGE_MS;
        long age = System.currentTimeMillis() - n.timestamp;
        return n.timestamp == 0L || age < 0 || age > maxAge ? NavData.empty() : n;
    }
    public static CameraData getCamera() {
        CameraData c = camera;
        long age = System.currentTimeMillis() - c.timestamp;
        return c.timestamp == 0L || age < 0 || age > CameraData.MAX_AGE_MS ? CameraData.empty() : c;
    }
    public static void update(NavData next) { if (next != null) current = next; }
    public static void updateCamera(CameraData next) { if (next != null) camera = next; }
    public static void clear() { current = NavData.empty(); camera = CameraData.empty(); }
    public static void clearGuidanceIfKey(String key) {
        if (key != null && key.equals(current.key)) current = NavData.empty();
    }
    public static void clearCameraIfKey(String key) {
        if (key != null && key.equals(camera.key)) camera = CameraData.empty();
    }
    public static void clearIfKey(String key) { clearGuidanceIfKey(key); clearCameraIfKey(key); }
    public static void demo() {
        long now = System.currentTimeMillis();
        current = new NavData("RIGHT", "350m", "데모 · 터널 후 우측 진출", "DEMO", now, "demo", "TUNNEL_RIGHT");
        camera = new CameraData("COMBINED", "500m", 80, now, "demo-camera");
    }
}
