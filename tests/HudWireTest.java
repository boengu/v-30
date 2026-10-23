import com.example.phonehud.*;
import java.util.Map;
import java.util.HashMap;

public class HudWireTest {
    static int n;
    static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); n++; }
    static final long NOW = 1800000000000L;
    static NavData nav(long age) { return new NavData("RIGHT", "300m", "터널 후 우측 진출", "네이버지도", NOW-age, "k", "TUNNEL_RIGHT"); }
    static CameraData cam(long age) { return new CameraData("COMBINED", "500m", 80, NOW-age, "camera"); }
    static HudWire.Snapshot decode(Map<String,Object> values) { return HudWire.decode(values,NOW); }
    public static void main(String[] args) {
        Map<String,Object> m = HudWire.encode(nav(1250),cam(2400),NOW);
        check(m.get("protocol").equals(6),"protocol v6");
        check(m.get("diagram").equals("TUNNEL_RIGHT"),"diagram encoded");
        check(m.get("cameraType").equals("COMBINED"),"camera encoded");
        check(m.get("cameraDistance").equals("500m"),"camera distance encoded");
        check(m.get("speedLimit").equals(80),"speed limit encoded");
        check(m.get("cameraAgeMs").equals(2400L),"camera age encoded");
        HudWire.Snapshot r=decode(m);
        check(r.compatible,"compatible");
        check(r.navigation.direction.equals("RIGHT"),"nav round trip");
        check(r.navigation.diagram.equals("TUNNEL_RIGHT"),"diagram round trip");
        check(r.camera.type.equals("COMBINED"),"camera round trip");
        check(r.camera.limitKmh==80,"limit round trip");
        check(r.camera.distance.equals("500m"),"distance round trip");
        r=decode(HudWire.encode(NavData.empty(),cam(10),NOW));
        check(r.navigation.direction.equals("WAIT"),"camera-only no fake maneuver");
        check(r.camera.present(),"camera-only visible");
        r=decode(HudWire.encode(nav(10),CameraData.empty(),NOW));
        check(!r.camera.present(),"nav-only no fake camera");
        check(r.navigation.direction.equals("RIGHT"),"nav-only preserved");
        r=decode(HudWire.encode(nav(100),cam(CameraData.MAX_AGE_MS+5),NOW));
        check(!r.camera.present(),"expired camera cleared");
        check(r.navigation.direction.equals("RIGHT"),"nav survives expired camera");
        r=decode(HudWire.encode(nav(NaverNavParser.MAX_AGE_MS+4),cam(300),NOW));
        check(r.navigation.direction.equals("WAIT"),"expired nav cleared");
        check(r.camera.present(),"camera survives expired nav");
        m=HudWire.encode(nav(50),cam(50),NOW); m.put("protocol",4);
        check(!decode(m).compatible,"old protocol rejected");
        m.put("protocol",6); m.put("cameraType","UNKNOWN");
        check(!decode(m).camera.present(),"unknown camera rejected");
        m.put("cameraType","SPEED"); m.put("speedLimit",200);
        check(decode(m).camera.limitKmh==-1,"unrealistic limit rejected");
        m.put("speedLimit",60); m.put("cameraAgeMs",50000L);
        check(!decode(m).camera.present(),"camera age on wire enforced");
        m.put("cameraAgeMs",100L);m.put("direction","FLY");
        check(decode(m).navigation.direction.equals("WAIT"),"unknown direction rejected");
        m.put("direction","RIGHT");m.put("diagram","TUNNEL_LEFT");
        check(decode(m).navigation.diagram.equals("RIGHT"),"mismatched branch rejected");
        m.put("diagram","FAKE_RIGHT");
        check(decode(m).navigation.diagram.equals("RIGHT"),"fabricated branch rejected");
        m.put("diagram","STRAIGHT");
        check(decode(m).navigation.diagram.equals("RIGHT"),"mismatched plain arrow rejected");
        m.put("direction","WAIT");
        check(decode(m).navigation.direction.equals("WAIT"),"wait never navigation");
        m.put("ageMs",-1L);m.put("direction","RIGHT");
        check(decode(m).navigation.direction.equals("WAIT"),"negative age clears navigation");
        m.put("ageMs",50L);m.put("cameraAgeMs",-1L);
        check(!decode(m).camera.present(),"negative age clears camera");
        r=decode(HudWire.encode(nav(-1),cam(-1),NOW));
        check(r.navigation.direction.equals("WAIT") && !r.camera.present(),"future timestamp rejected");
        r=decode(null);
        check(!r.compatible,"null rejected");
        System.out.println("PASS: " + n + " HUD wire-contract checks");
    }
}
