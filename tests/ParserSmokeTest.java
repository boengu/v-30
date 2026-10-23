import com.example.phonehud.NavData;
import com.example.phonehud.NavParser;

public class ParserSmokeTest {
    private static int pass = 0;
    private static void verify(String label, boolean ok) {
        if (!ok) throw new AssertionError("FAILED: " + label);
        pass++;
    }
    private static NavData nav(String pkg, String title, String body) {
        return NavParser.parse(pkg, title, body, System.currentTimeMillis(), "test");
    }
    public static void main(String[] args) {
        String tmap = "com.skt.tmap.ku", kakao = "com.locnall.KimGiSa",
                naver = "com.nhn.android.nmap", google = "com.google.android.apps.maps";
        NavData left = nav(tmap, "200m 앞 좌회전", "영통로 방면");
        verify("korean left", left != null && "LEFT".equals(left.direction));
        verify("korean distance", left != null && "200m".equals(left.distance));
        verify("source TMap", left != null && "T맵".equals(left.source));
        NavData right = nav(kakao, "1.2km 후 우회전", "영통구청 사거리");
        verify("korean right", right != null && "RIGHT".equals(right.direction));
        verify("decimal distance", right != null && "1.2km".equals(right.distance));
        verify("source Kakao", right != null && "카카오내비".equals(right.source));
        NavData n = nav(naver, "전방 120미터에서 직진", "사거리");
        verify("korean straight", n != null && "STRAIGHT".equals(n.direction));
        verify("korean meter", n != null && "120m".equals(n.distance));
        NavData g = nav(google, "In 500 m, turn right", "Main Street");
        verify("english right", g != null && "RIGHT".equals(g.direction));
        verify("english distance", g != null && "500m".equals(g.distance));
        verify("english source", g != null && "구글지도".equals(g.source));
        NavData u = nav(tmap, "100 m 후 유턴", "유턴하세요");
        verify("uturn", u != null && "UTURN".equals(u.direction));
        NavData r = nav(tmap, "회전교차로 진입", "2차로");
        verify("roundabout", r != null && "ROUND".equals(r.direction));
        NavData arrive = nav(google, "목적지에 도착했습니다", "운행 종료");
        verify("arrive", arrive != null && "ARRIVE".equals(arrive.direction));
        verify("ignore messenger", nav("com.android.messaging", "좌회전", "300m") == null);
        verify("ignore unknown", nav("com.any.other", "turn right", "400m") == null);
        verify("ignore promotion", nav(tmap, "할인 쿠폰", "이벤트 알림") == null);
        verify("ignore empty", nav(tmap, "", "") == null);
        verify("ignore plain distance", nav(tmap, "할인 30km", "혜택") == null);
        verify("ignore speed", nav(tmap, "80km/h", "알림") == null);
        verify("accept route distance", nav(tmap, "전방 1km 안내", "고속도로") != null);
        verify("accept comma decimals", "1.5km".equals(nav(tmap, "1,5km 후 우회전", "도로").distance));
        verify("truncate long roads", nav(tmap, "다음 좌회전", "길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길길").road.length() <= 76);
        verify("default wait", "WAIT".equals(NavData.empty().direction));
        verify("default distance", "--".equals(NavData.empty().distance));
        verify("source filter", NavParser.isNavigationPackage(google) && !NavParser.isNavigationPackage("com.chat.app"));
        System.out.println("PASS: " + pass + " navigation parser checks");
    }
}
