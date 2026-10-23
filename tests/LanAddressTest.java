import com.example.phonehud.LanAddress;

public final class LanAddressTest {
    private static int count;
    private static void test(boolean expected, String ip) {
        if (LanAddress.allowed(ip) != expected) throw new AssertionError("LAN address: " + ip);
        count++;
    }
    public static void main(String[] args) {
        test(true, "192.168.43.1"); test(true, "192.168.0.10");
        test(true, "172.20.10.1"); test(true, "172.31.254.254");
        test(true, "10.0.0.1"); test(true, "100.90.10.11");
        test(false, "127.0.0.1"); test(false, "8.8.8.8");
        test(false, "169.254.1.2"); test(false, "192.168.0.255");
        test(false, "192.168.0.0"); test(false, "172.32.0.1");
        test(false, "192.168.01.1"); test(false, "192.168.0.1:8765");
        test(false, "256.1.1.1"); test(false, ""); test(false, null);
        test(false, "192.168.1.1/24"); test(false, "http://10.0.0.1");
        System.out.println("PASS: " + count + " private-LAN address checks");
    }
}
