@echo off
chcp 65001 >nul
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0BUILD_APK_원클릭.ps1"
if errorlevel 1 (
  echo.
  echo APK 생성 실패. 위 오류를 보낸 뒤 수정해야 합니다.
) else (
  echo.
  echo APK 생성 확인: app\build\outputs\apk\debug\app-debug.apk
)
pause
