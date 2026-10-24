#!/usr/bin/env python3
"""Fail the cloud build if only some of the HUD 2.8 files were uploaded."""
from pathlib import Path
import sys
root=Path(__file__).resolve().parent
required=[
    'settings.gradle','build.gradle','app/build.gradle',
    'app/src/main/AndroidManifest.xml',
    'app/src/main/java/com/example/phonehud/MainActivity.java',
    'app/src/main/java/com/example/phonehud/NavNotificationService.java',
    'app/src/main/java/com/example/phonehud/NaverNavParser.java',
    'app/src/main/java/com/example/phonehud/NaverChipParser.java',
    'app/src/main/java/com/example/phonehud/NaverNotificationText.java',
    'app/src/main/java/com/example/phonehud/NotificationProbe.java',
    'app/src/main/java/com/example/phonehud/HudWire.java',
    'tests/run_parser_smoke.sh',
    'tests/NaverChipParserTest.java','tests/NotificationProbeTest.java',
    '.github/workflows/build-debug-apk.yml',
]
missing=[x for x in required if not (root/x).is_file()]
if missing:
    print('ERROR: HUD 2.8 upload incomplete, missing:')
    for name in missing: print(' - '+name)
    print('Unzip HUD_LINK_V30_2_8_GitHub_업로드용.zip and upload folder contents at repository root.')
    sys.exit(1)
cfg=(root/'app/build.gradle').read_text(encoding='utf-8')
for marker in ('compileSdk 35','minSdk 24','targetSdk 35','versionCode 28', "versionName '2.8.0-naver-chip-probe'"):
    if marker not in cfg:
        print('ERROR: Incorrect or older app/build.gradle. Expected: '+marker)
        sys.exit(1)
main=(root/'app/src/main/java/com/example/phonehud/MainActivity.java').read_text(encoding='utf-8')
listener=(root/'app/src/main/java/com/example/phonehud/NavNotificationService.java').read_text(encoding='utf-8')
parser=(root/'app/src/main/java/com/example/phonehud/NaverNavParser.java').read_text(encoding='utf-8')
runner=(root/'tests/run_parser_smoke.sh').read_text(encoding='utf-8')
for ok,reason in [
    ('scanActiveNow()' in main,'MainActivity missing 2.8 diagnostic button'),
    ('scanActiveNow()' in listener,'NavNotificationService missing active notification probe'),
    ('NaverChipParser.parse' in parser,'NaverNavParser missing chip parser call'),
    ('NaverChipParserTest' in runner,'test script is still 2.6'),
]:
    if not ok: print('ERROR: '+reason);sys.exit(1)
print('PASS: HUD LINK NAVER 2.8 files complete, LG V30 minSdk 24, compileSdk 35')
