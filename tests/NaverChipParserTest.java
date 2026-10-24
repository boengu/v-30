import com.example.phonehud.NaverChipParser;
import com.example.phonehud.NaverNavParser;
import com.example.phonehud.NavData;

public final class NaverChipParserTest {
    private static int n;
    private static void test(boolean yes) { n++; if (!yes) throw new AssertionError("chip check " + n); }
    private static NavData parse(String body) {
        return NaverNavParser.parse("com.nhn.android.nmap", "NAVER", body, 1000L, "key");
    }
    public static void main(String[] args) {
        test(parse("↰ 503m").direction.equals("LEFT"));
        test(parse("↰ 503m").distance.equals("503m"));
        test(parse("↱ 48m").direction.equals("RIGHT"));
        test(parse("← 1.2km").direction.equals("LEFT"));
        test(parse("→500m").direction.equals("RIGHT"));
        test(parse("↑ 550m").direction.equals("STRAIGHT"));
        test(parse("↖ 350미터").distance.equals("350m"));
        test(parse("↗ 2킬로미터").distance.equals("2km"));
        test(parse("⬅ 203m").direction.equals("LEFT"));
        test(parse("➡ 100m").direction.equals("RIGHT"));
        test(parse("503m")==null);
        test(parse("↰")==null);
        test(parse("단순한 내비 알림")==null);
        test(NaverChipParser.parse("com.example.other", "", "↱ 48m", 1000L, "a")==null);
        test(parse("↰ 503ms")==null);
        test(parse("↰ 12m 할인 쿠폰")==null);
        System.out.println("PASS: " + n + " explicit navigation symbol checks");
    }
}
