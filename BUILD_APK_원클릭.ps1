$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$gradleVersion = '8.7'
$expectedSha = '544c35d6bd849ae8a5ed0bcea39ba677dc40f49df7d1835561582da2009b961d'
$tools = Join-Path $PSScriptRoot '.local-build-tools'
$unpacked = Join-Path $tools "gradle-$gradleVersion"
$zip = Join-Path $tools "gradle-$gradleVersion-bin.zip"
$gradle = Join-Path $unpacked 'bin/gradle.bat'
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
  throw 'Java not found. Install JDK 17 and set PATH/JAVA_HOME. See the PDF guide.'
}
if (-not (Test-Path $gradle)) {
  New-Item -ItemType Directory -Path $tools -Force | Out-Null
  Write-Host "Downloading official Gradle $gradleVersion (about 130 MB)..."
  Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $zip
  $actual = (Get-FileHash $zip -Algorithm SHA256).Hash.ToLowerInvariant()
  if ($actual -ne $expectedSha) {
    Remove-Item $zip -ErrorAction SilentlyContinue
    throw "Gradle checksum mismatch; downloaded archive deleted."
  }
  Expand-Archive -LiteralPath $zip -DestinationPath $tools -Force
}
if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
  Write-Host 'Android SDK not in environment. Android Studio may configure SDK through local.properties.'
  Write-Host 'If build fails with SDK location not found, create local.properties with sdk.dir=...'
}
Write-Host 'Running HUD LINK compile...'
& $gradle --no-daemon assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit $LASTEXITCODE). See errors above." }
$apk = Join-Path $PSScriptRoot 'app/build/outputs/apk/debug/app-debug.apk'
if (-not (Test-Path $apk)) { throw 'Gradle finished but app-debug.apk was not found' }
Write-Host "SUCCESS: $apk" -ForegroundColor Green
Write-Host "SHA256: $((Get-FileHash $apk -Algorithm SHA256).Hash)"
