# HUD LINK NAVER 2.6 · LG V30용 개발 베타

**결과물: 안드로이드 앱 소스 + Java 테스트 + 미리보기 + GitHub Actions APK 빌드 설정 + PDF 설명서. Android APK 파일 자체는 아직 생성하지 못했습니다.**

## 지금 사용할 수 있는 것
- 메인폰의 **네이버지도** `com.nhn.android.nmap` 알림 중 길안내/카메라 문구를 추출해 PIN이 있는 동일 사설 LAN의 LG V30에 전달
- 안내 문구에 존재하는 왼쪽/오른쪽, 분기/터널/IC의 **간략 도식** 표시. 실제 차선·도로 형상은 표시하지 않음
- 알림에 존재하는 카메라 종류/거리/제한속도, V30 GPS 속도(위치 권한 필요), 반사형 좌우반전, OLED 검은 화면
- 길안내 30초, 카메라 20초 안에 갱신이 없으면 자동 만료
- 데모는 반드시 DEMO로 표시하며 **실제 네이버지도 데이터를 대신 생성하지 않음**

## 2.6 새로 개선한 사항
1. V30 송신 IP 입력을 사설 주소(10.*, 172.16~31.*, 192.168.*, 일부 통신사 핫스팟의 100.64~127.*)로 제한하여 임의 인터넷 IP로 PIN이 송신되는 것을 예방합니다.
2. 통신 서버 요청 처리 큐를 제한하여 비정상적인 연결 폭주 상황에서 무제한 요청 적재를 방지합니다.
3. 네이버지도에서 매우 긴 알림이 오는 경우 제목 256자·본문 2048자로 상한을 둡니다. 앱이 별도 전송하지 않는 카메라/방향을 추측하지 않습니다.
4. GitHub Actions APK 빌드 전 자바 테스트 실행과 APK 산출물 확인 단계를 추가했습니다.
5. IP 형식 테스트 19개를 추가해 총 **164개 순수 Java 검사 통과**했습니다. 실제 Android APK 빌드/네이버지도 실기기 호환성을 검증한 수치는 아닙니다.

## 먼저 20초 데모로 시험
1. 메인폰과 LG V30에 **동일 버전 2.6 APK**를 설치합니다(아래 빌드 필요).
2. 메인폰: HUD LINK → 메인폰 설정 → 네이버지도 알림 접근 허용 → 송신 시작 → IP와 8자리 PIN 기록.
3. LG V30: 같은 개인 Wi-Fi/핫스팟 연결 → 공기계 HUD 설정 → 메인폰의 IP/PIN 입력 → 연결 테스트 → HUD 시작.
4. 메인폰에서 **20초 데모 전송**. V30 HUD에 `DEMO` 표시/터널 분기/카메라가 나오는지 검사. 데모는 가짜 데이터입니다.
5. 네이버지도에서 실제 경로 안내를 시작해 **알림의 안내 내용이 송신 화면에 인식되는지** 검사. 네이버지도 알림에 방향/거리/카메라가 없으면 해당 HUD 정보도 없습니다.
6. 먼저 정차 상태에서 검사하고, 주행 중 휴대폰을 조작하지 마세요.

## 방법 A: GitHub Actions에서 APK 생성 시도 (권장)
1. https://github.com 에 로그인 → 새 **Private** 저장소 생성 (빈 저장소 권장).
2. ZIP 압축을 풀고 안쪽 `HUD_LINK_NAVER_V30_2_6/`의 **파일과 하위 폴더 전체**를 저장소 최상위에 올립니다. `.github/workflows/build-debug-apk.yml`이 반드시 들어가야 합니다. 폴더 `HUD_LINK_NAVER_V30_2_6` 자체를 한 단계 중첩해 올리지 마세요.
3. 저장소 **Actions** → `Build HUD LINK V30 debug APK` → `Run workflow` 실행.
4. 성공 시 해당 실행 화면의 Artifacts에서 `HUD-LINK-V30-NAVER-v2.6-debug`를 받습니다. 안에 `app-debug.apk`가 있어야 합니다.
5. 실패하면 해당 Actions 실행의 `Run source and parser tests` 또는 `Build debug APK` 로그를 확인합니다. **이 Workflow는 제공만 했으며 실제 원격 실행은 검증하지 않았습니다.** GitHub 사용량 제한이 있을 수 있습니다.

## 방법 B: 내 Windows PC에서 빌드
1. Android Studio https://developer.android.com/studio 를 설치하고 **SDK Platform 35**를 받습니다.
2. JDK 17 / **Gradle 8.7**로 프로젝트를 엽니다. Android Gradle Plugin 8.6.1 설정입니다.
3. 이 ZIP에 `gradlew`/`gradlew.bat`/`gradle-wrapper.jar`는 포함되어 있지 않습니다. 로컬 Gradle 8.7 CLI를 설치했으면 `BUILD_APK_Windows.cmd`를 실행하거나 프로젝트 루트에서 `gradle --no-daemon assembleDebug`를 입력합니다.
4. 성공 시 `app/build/outputs/apk/debug/app-debug.apk`가 생깁니다.
5. APK는 개인 개발용 **debug 빌드**입니다. 정식 배포 서명/스토어 검토/실기기 주행 QA가 완료된 앱이 아닙니다.

## 검증/제약
- `bash tests/run_parser_smoke.sh` → 164개 단위 검사 PASS.
- 여기 환경에는 Android SDK/Gradle이 없어서 APK 컴파일을 실제 수행하지 못함. APK 파일을 다운로드할 수 있다고 오인하지 마세요.
- `app/build.gradle`: minSdk 24 / compileSdk 35 / targetSdk 35 / JDK 17 / AGP 8.6.1.
- 네이버지도는 **정식 실시간 길안내 API 연결이 아니라 Android 알림 텍스트 분석 방식**입니다. 실제 알림이 안내문을 제공하지 않으면 정보가 비어 있습니다.
- GPS가 꺼져 있거나 정확하지 않으면 `--` 또는 지연된 값이 나올 수 있고, 표시된 속도/제한속도는 법적·운전 책임 판단에 쓰지 마세요.
- 로컬 HTTP + 8자리 PIN은 **암호화된 통신이 아닙니다.** 신뢰할 수 있는 개인 핫스팟만 사용하세요. PIN을 타인과 공유하지 마세요.
- HUD는 보조 참고용으로만 사용하세요. 반사 영상이 운전 시야를 가리거나 혼동을 주면 즉시 사용을 중지하세요.

## 문제 해결
- `Gradle not installed`: Gradle 8.7 CLI 설치 또는 GitHub Actions 방법 이용.
- `Android SDK not found`: Android Studio SDK Manager에서 API 35 설치 및 SDK 경로 지정.
- `알림 접근 확인 필요`: 메인폰의 설정 > 알림 접근에서 HUD LINK 활성화.
- `네이버지도 알림 미수신`: 알림 허용/네이버지도 경로 안내 실행 확인. 알림이 단순 알림이면 지도 데이터가 나오지 않음.
- `연결 실패`: 메인폰 서버 시작, 두 폰 같은 핫스팟, IP·PIN, 공유기 기기 간 통신 차단 확인.
- `사설 IP 형식 오류`: 예: 192.168.43.1 / 172.20.10.1. 공인 IP는 입력되지 않도록 했습니다.
- `버전이 다릅니다`: 두 기기 모두 v2.6 앱 설치.

공식 문서: https://developer.android.com/studio | https://developer.android.com/build/releases/agp-8-6-0-release-notes | https://developer.android.com/reference/android/service/notification/NotificationListenerService
