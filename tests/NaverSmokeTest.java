import com.example.phonehud.NavData;
import com.example.phonehud.NaverNavParser;
import com.example.phonehud.NavStore;

public class NaverSmokeTest {
    static int count = 0;
    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); count++; }
    static NavData parse(String title, String body) {
        return NaverNavParser.parse(NaverNavParser.PACKAGE, title, body, System.currentTimeMillis(), "naver");
    }
    public static void main(String[] args) {
        check(parse("300m 후 우회전", "내비게이션") != null, "simple nav");
        check("RIGHT".equals(parse("300m 후 우회전", "내비게이션").direction), "right");
        check("300m".equals(parse("300m 후 우회전", "내비게이션").distance), "distance meter");
        check("LEFT".equals(parse("다음 1.2km 좌회전", "").direction), "left");
        check("1.2km".equals(parse("다음 1.2km 좌회전", "").distance), "decimal km");
        check("1.5km".equals(parse("1,5km 후 우회전", "").distance), "decimal comma");
        check("RIGHT".equals(parse("300m 후 우회전, 다음 2km 후 좌회전", "").direction), "first maneuver first");
        check("300m".equals(parse("300m 후 우회전, 다음 2km 후 좌회전", "").distance), "first distance first");
        check("LEFT".equals(parse("전방 200 미터 좌측 방향", "").direction), "left side");
        check("RIGHT".equals(parse("전방 80m 우측 진입", "").direction), "right fork");
        check("STRAIGHT".equals(parse("전방 300m 직진", "").direction), "straight");
        check("UTURN".equals(parse("잠시 후 유턴", "").direction), "uturn no distance");
        check("--".equals(parse("잠시 후 유턴", "").distance), "no invented distance");
        check("ROUND".equals(parse("회전교차로 진입", "").direction), "roundabout");
        check("ARRIVE".equals(parse("목적지에 도착했습니다", "").direction), "arrive");
        check(parse("목적지까지 3km", "") == null, "destination far not arrived");
        check(parse("Naver 할인 이벤트", "300m 우회전 할인") == null, "ignore advertising");
        check(parse("새로운 장소 추천", "가을 여행지") == null, "ignore recommendation");
        check(parse("안내 중", "제한속도 80km/h") == null, "ignore speed");
        check(parse("출발", "운행 시작") == null, "wait if no maneuver");
        check(parse("좌회전 맛집", "새로운 발견") == null, "non-nav turn word is ignored");
        check("--".equals(parse("우회전 후 100m 좌회전", "다음 방향").distance), "later distance not current turn");
        check(NaverNavParser.isReset("경로 재탐색 중", ""), "recalc clears");
        check(NaverNavParser.isReset("", "길안내 종료"), "stop clears");
        check(!NaverNavParser.isReset("", "좌회전 300m"), "turn not reset");
        check(NaverNavParser.parse("com.skt.tmap.ku", "300m 우회전", "", 1L, "a") == null, "naver only");
        check(!NaverNavParser.isNaver("com.chat.app"), "private filters");
        NavStore.update(new NavData("RIGHT", "30m", "test", "네이버지도", System.currentTimeMillis()-NaverNavParser.MAX_AGE_MS-1, "stale"));
        check("WAIT".equals(NavStore.get().direction), "stale nav hidden");
        NavStore.clear();
        check("WAIT".equals(NavStore.get().direction), "reset nav");
        System.out.println("PASS: " + count + " Naver-specific checks");
    }
}
