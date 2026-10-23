@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo =======================================
echo HUD LINK NAVER V30 2.6 - Debug APK
 echo =======================================
echo Required: JDK 17, Android SDK Platform 35, Gradle 8.7
if exist gradlew.bat (
  call gradlew.bat --no-daemon assembleDebug
) else (
  where gradle >nul 2>&1
  if errorlevel 1 (
    echo [NOT BUILT] Gradle 8.7 is not installed.
    echo Install Android Studio and Gradle 8.7, or use a Gradle wrapper.
    echo See README_한국어.md and PDF for detailed setup.
    pause
    exit /b 1
  )
  call gradle --no-daemon assembleDebug
)
if errorlevel 1 (
  echo [FAILED] Check Android SDK, JDK, Gradle and network access.
  pause
  exit /b 1
)
echo [OK] app\build\outputs\apk\debug\app-debug.apk
pause
