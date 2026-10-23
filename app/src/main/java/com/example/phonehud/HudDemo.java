package com.example.phonehud;

/** Explicit visual test scenes: never injected into real navigation results. */
public final class HudDemo {
    public static final String[] NAMES = {
        "터널 후 우측 진출", "터널 후 좌측 진출", "우측 분기점", "좌측 분기점",
        "IC 우측 진출", "IC 좌측 진출", "일반 좌회전", "직진 유지", "카메라만 표시"
    };
    private HudDemo() { }
    public static NavData navigation(int index) {
        int n = Math.floorMod(index, NAMES.length);
        String dir = n==1 || n==3 || n==5 || n==6 ? "LEFT" : n==7 ? "STRAIGHT" : n==8 ? "WAIT" : "RIGHT";
        String diagram = n==0?"TUNNEL_RIGHT":n==1?"TUNNEL_LEFT":n==2?"FORK_RIGHT":n==3?"FORK_LEFT":n==4?"EXIT_RIGHT":n==5?"EXIT_LEFT":dir;
        return new NavData(dir, n==8?"--":n<2?"350m":n<6?"700m":"250m",
                "데모 · " + NAMES[n] + " (실제 안내 아님)", "DEMO", System.currentTimeMillis(), "demo", diagram);
    }
    public static CameraData camera(int index) {
        int n = Math.floorMod(index, NAMES.length);
        String type = n==1 ? "SPEED" : n==4 ? "SECTION_START" : n==5 ? "SECTION_END" : n==8 ? "RED_LIGHT" : "COMBINED";
        return new CameraData(type, n==8?"150m":"500m", n==5?-1:n==8?60:80, System.currentTimeMillis(), "demo");
    }
}
