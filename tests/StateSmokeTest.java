import com.example.phonehud.NavData;
import com.example.phonehud.NavStore;

public class StateSmokeTest {
    private static int pass;
    private static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); pass++; }
    public static void main(String[] args) {
        NavStore.update(new NavData("LEFT", "50m", "도로", "T맵", System.currentTimeMillis(), "a"));
        check("LEFT".equals(NavStore.get().direction), "new route");
        NavStore.clearIfKey("b");
        check("LEFT".equals(NavStore.get().direction), "unrelated key");
        NavStore.clearIfKey("a");
        check("WAIT".equals(NavStore.get().direction), "remove matching");
        NavStore.update(new NavData("RIGHT", "1km", "old", "T맵", System.currentTimeMillis() - 91000, "old"));
        check("WAIT".equals(NavStore.get().direction), "expire route");
        NavStore.update(new NavData("RIGHT", "1km", "old-demo", "DEMO", System.currentTimeMillis() - 21000, "demo"));
        check("WAIT".equals(NavStore.get().direction), "expire demo");
        NavStore.demo();
        check("DEMO".equals(NavStore.get().source), "inject demo");
        System.out.println("PASS: " + pass + " state checks");
    }
}
