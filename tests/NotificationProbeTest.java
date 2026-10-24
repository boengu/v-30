import com.example.phonehud.NotificationProbe;

public final class NotificationProbeTest {
    private static int checks;
    private static void ok(boolean b) { checks++; if (!b) throw new AssertionError("check " + checks); }
    public static void main(String[] args) {
        ok(NotificationProbe.summary(false, 3, true, true).contains("미연결"));
        ok(NotificationProbe.summary(true, 0, false, false).contains("0개"));
        ok(NotificationProbe.summary(true, -1, false, false).contains("0개"));
        ok(NotificationProbe.summary(true, 1, false, false).contains("문구 없음"));
        ok(NotificationProbe.summary(true, 7, false, false).contains("7개"));
        ok(NotificationProbe.summary(true, 1, true, false).contains("방향 문구"));
        ok(NotificationProbe.summary(true, 2, false, true).contains("단속 문구"));
        ok(NotificationProbe.summary(true, 3, true, true).contains("방향 및 단속"));
        ok(NotificationProbe.summary(true, 1, true, false).contains("별도"));
        ok(NotificationProbe.summary(true, 1, false, false).contains("보낼 정보 없음"));
        System.out.println("PASS: " + checks + " notification diagnosis checks");
    }
}
