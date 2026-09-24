#!/usr/bin/env bash
# End-to-end check on an emulator: HQ badge in the Library and renaming a song.
# Screenshots, UI dumps, MediaStore rows and logcat go to e2e-out/.
set -u
APP=com.retro.cassetteplayer
OUT=e2e-out
mkdir -p "$OUT"

shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
dump() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$OUT/$1.xml" >/dev/null 2>&1
}
# center of the first node whose text or content-desc equals $1 (in dump $2)
center_of() {
  python3 - "$1" "$OUT/$2.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
target, path = sys.argv[1], sys.argv[2]
try:
    root = ET.parse(path).getroot()
except Exception:
    sys.exit(1)
for node in root.iter("node"):
    if node.get("text") == target or node.get("content-desc") == target:
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        sys.exit(0)
sys.exit(1)
PY
}
tap_text() {
  dump tmp
  if pos=$(center_of "$1" tmp); then adb shell input tap $pos; echo "tapped '$1' at $pos"; else echo "NOT FOUND: '$1'"; fi
}
longpress_text() {
  dump tmp
  if pos=$(center_of "$1" tmp); then
    set -- $pos
    adb shell input swipe "$1" "$2" "$1" "$2" 1200
    echo "long-pressed at $1 $2"
  else echo "NOT FOUND for long press"; fi
}

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell mkdir -p /sdcard/Music/E2E
for f in e2e-media/*; do adb push "$f" /sdcard/Music/E2E/; done
adb shell pm grant $APP android.permission.READ_MEDIA_AUDIO || true
adb shell pm grant $APP android.permission.POST_NOTIFICATIONS || true
# Wait until MediaStore has scanned the test files (is_music set)
for i in $(seq 1 40); do
  n=$(adb shell content query --uri content://media/external/audio/media --projection is_music 2>/dev/null | grep -c "is_music=1")
  echo "scanned: $n"
  [ "$n" -ge 3 ] && break
  sleep 3
done
adb shell content query --uri content://media/external/audio/media \
  --projection _id:_display_name:mime_type:title:is_music > "$OUT/mediastore_before.txt" 2>&1

adb logcat -c
adb shell am start -n $APP/.MainActivity
sleep 12
shot 01_home; dump 01_home

tap_text "Library"; sleep 4
shot 02_library; dump 02_library
shot 02b_library_grid
tap_text "Songs"; sleep 3
shot 03_songs; dump 03_songs
echo "HQ badges on songs screen: $(grep -o 'High quality (lossless)' "$OUT/03_songs.xml" | wc -l)" | tee "$OUT/hq_count.txt"

# --- rename ---
longpress_text "Lossless Test One"; sleep 2
shot 04_menu; dump 04_menu
tap_text "Rename"; sleep 2
shot 05_dialog; dump 05_dialog
adb shell input keyevent KEYCODE_MOVE_END
for i in $(seq 1 40); do adb shell input keyevent KEYCODE_DEL; done
adb shell input text "Renamed%sByE2E"
sleep 1
dump 06_dialog_filled
# confirm button of the dialog (the last "Rename" node on screen)
python3 - "$OUT/06_dialog_filled.xml" > "$OUT/confirm_pos.txt" <<'PY'
import re, sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
pos = None
for node in root.iter("node"):
    if node.get("text") == "Rename":
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        pos = f"{(x1 + x2) // 2} {(y1 + y2) // 2}"
print(pos or "")
PY
if [ -s "$OUT/confirm_pos.txt" ]; then adb shell input tap $(cat "$OUT/confirm_pos.txt"); fi
sleep 4
shot 07_consent; dump 07_consent
tap_text "Allow"; sleep 8
shot 08_after_rename; dump 08_after_rename

adb shell content query --uri content://media/external/audio/media \
  --projection _id:_display_name:mime_type:title > "$OUT/mediastore_after.txt" 2>&1
adb logcat -d > "$OUT/logcat.txt"
adb logcat -d | grep -iE "retro|cassette|AndroidRuntime|jaudiotagger|MediaProvider" | tail -400 > "$OUT/logcat_filtered.txt"
echo done
