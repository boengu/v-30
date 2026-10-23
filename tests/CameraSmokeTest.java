import com.example.phonehud.*;
public class CameraSmokeTest {
 static int n;
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);n++;}
 static CameraData p(String t){return NaverCameraParser.parse(NaverNavParser.PACKAGE,"내비게이션",t,1800000000000L,"camera");}
 public static void main(String[]a){
  CameraData c=p("500m 앞 과속 단속 카메라, 제한속도 60km/h");
  check(c!=null,"speed recognized"); check(c.type.equals("SPEED"),"speed type");
  check(c.distance.equals("500m"),"camera meters"); check(c.limitKmh==60,"speed limit");
  c=p("300m 앞 신호 단속 카메라 제한속도 50");
  check(c!=null && c.type.equals("RED_LIGHT"),"red light");check(c.limitKmh==50,"limit no unit");
  c=p("신호 및 과속 단속 400m 앞 제한속도 70km/h");
  check(c!=null && c.type.equals("COMBINED"),"combined");
  check(c.distance.equals("400m"),"combined distance");
  c=p("구간단속 시작 100m 앞"); check(c!=null && c.type.equals("SECTION_START"),"section start");
  c=p("구간 단속 종료 50m 앞");check(c!=null && c.type.equals("SECTION_END"),"section end");
  c=p("1.2km 전 구간단속");check(c!=null && c.distance.equals("1.2km"),"km distance");
  c=p("이동식 과속 단속 200m 앞");check(c!=null && c.type.equals("MOBILE"),"mobile");
  c=p("무인 단속 카메라");check(c!=null && c.type.equals("OTHER"),"other type");
  check(p("단속카메라 30m 앞").distance.equals("30m"),"compact camera");
  c=p("제한속도 80km/h, 1km 전 과속 단속");
  check(c!=null && c.limitKmh==80,"separate speed limit");
  check(c.distance.equals("1km"),"no mix with speed");
  check(p("주변 카메라 화질 좋은 상품 할인") == null,"shopping irrelevant");
  check(p("카메라 단속 관련 뉴스") == null,"news not a camera warning");
  check(NaverCameraParser.parse("com.google.android.apps.maps","","과속 단속 300m",1,"x")==null,"naver only");
  check(p("300m 앞 우회전") == null,"no fake camera");
  check(p("경로 재탐색 중 과속 단속 200m") == null,"reset clears camera");
  c=p("500m 앞 과속 단속 제한 999");
  check(c!=null && c.limitKmh==-1,"bad limit unknown");
  check(c.distance.equals("500m"),"distance independent limit");
  c=p("1,5km 앞 과속 단속 카메라");check(c!=null && c.distance.equals("1.5km"),"comma distance");
  System.out.println("PASS: "+n+" camera parser checks");
 }
}
