#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$(mktemp -d)"; trap 'rm -rf "$OUT"' EXIT
cd "$OUT"
javac -encoding UTF-8 -d "$OUT" \
  "$ROOT/app/src/main/java/com/example/phonehud/NavData.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/CameraData.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/NaverCameraParser.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/DiagramClassifier.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/HudDemo.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/HudWire.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/LinkHealth.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/LanAddress.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/NavStore.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/NavParser.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/NaverNavParser.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/NaverChipParser.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/HudHardwareProfile.java" \
  "$ROOT/app/src/main/java/com/example/phonehud/NotificationProbe.java" \
  "$ROOT/tests/NaverChipParserTest.java" "$ROOT/tests/ParserSmokeTest.java" "$ROOT/tests/StateSmokeTest.java" "$ROOT/tests/V30CompatibilitySmokeTest.java" "$ROOT/tests/NaverSmokeTest.java" "$ROOT/tests/HudWireTest.java" "$ROOT/tests/CameraSmokeTest.java" "$ROOT/tests/LinkHealthTest.java" "$ROOT/tests/LanAddressTest.java" "$ROOT/tests/NotificationProbeTest.java"
java -cp "$OUT" ParserSmokeTest
java -cp "$OUT" StateSmokeTest
java -cp "$OUT" V30CompatibilitySmokeTest
java -cp "$OUT" NaverSmokeTest
java -cp "$OUT" HudWireTest
java -cp "$OUT" CameraSmokeTest

java -cp "$OUT" LinkHealthTest

java -cp "$OUT" LanAddressTest

java -cp "$OUT" NotificationProbeTest

java -cp "$OUT" NaverChipParserTest
