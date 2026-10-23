import com.example.phonehud.*;
import java.util.Map;
public final class LinkHealthTest {
 static int n;
 static void check(boolean pass,String reason){ if(!pass) throw new AssertionError(reason); n++; }
 static final long NOW=1800000000000L;
 public static void main(String[] args){
   check(LinkHealth.message(false,true,10,false,false).contains("연결 실패"),"offline");
   check(LinkHealth.message(true,false,10,false,false).contains("알림 접근"),"permission");
   check(LinkHealth.message(true,true,-1,false,false).contains("미수신"),"never received");
   check(LinkHealth.message(true,true,120001,false,false).contains("갱신"),"stale");
   check(LinkHealth.message(true,true,1000,false,false).contains("내용 없음"),"no parse result");
   check(LinkHealth.message(true,true,1000,true,false).contains("길안내 수신"),"nav received");
   check(LinkHealth.message(true,true,1000,false,true).contains("카메라 안내"),"camera only");
   check(LinkHealth.message(true,true,1000,true,true).contains("길안내 및 카메라"),"both");
   NavData nav = new NavData("LEFT","200m","분기","네이버지도",NOW-1000,"key","FORK_LEFT");
   Map<String,Object> map = HudWire.encode(nav,CameraData.empty(),NOW,true,NOW-1500);
   check(map.get("protocol").equals(6),"protocol six");
   check(map.get("listenerReady").equals(true),"flag set");
   check(map.get("lastNaverAgeMs").equals(1500L),"age set");
   HudWire.Snapshot snap=HudWire.decode(map,NOW);
   check(snap.compatible && snap.notificationListenerReady,"round trip flag");
   check(snap.lastNaverAgeMs==1500L,"round trip age");
   map.put("lastNaverAgeMs",999999999L);
   check(HudWire.decode(map,NOW).lastNaverAgeMs==-1,"reject impossible last age");
   map.put("listenerReady","true");
   check(!HudWire.decode(map,NOW).notificationListenerReady,"reject string instead of boolean");
   map.put("protocol",5);
   check(!HudWire.decode(map,NOW).compatible,"reject v2.4 packet");
   map.put("protocol",6);map.put("speedLimit",4294967356L);
   map.put("cameraType","SPEED");map.put("cameraAgeMs",50L);
   HudWire.Snapshot limit=HudWire.decode(map,NOW);
   check(limit.camera.limitKmh==-1,"reject overflowed speed limit");
   System.out.println("PASS: "+n+" link health / protocol checks");
 }
}
