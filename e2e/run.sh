#!/usr/bin/env bash
# End-to-end smoke test on an emulator for the 3.0 features (settings, player sheets,
# side A/B, stats, folders, mixtape sharing). Fails loudly on crashes.
# Screenshots, UI dumps, MediaStore rows and logcat go to e2e-out/.
set -u
APP=com.retro.cassetteplayer
# Never hang on a dead emulator
adb() { timeout 60 command adb "$@"; }
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


# scroll (down, then back up) until a node with this text shows up, then tap it
scroll_tap() {
  for dir in down up; do
    for i in $(seq 1 8); do
      dump tmp
      if pos=$(center_of "$1" tmp); then adb shell input tap $pos; echo "tapped '$1' at $pos"; return 0; fi
      if [ $dir = down ]; then adb shell input swipe $((W/2)) $((H*3/4)) $((W/2)) $((H/3)) 400
      else adb shell input swipe $((W/2)) $((H/3)) $((W/2)) $((H*3/4)) 400; fi
      sleep 1
    done
  done
  echo "NOT FOUND after scrolling: '$1'"
}
alive() { adb get-state >/dev/null 2>&1 || { echo "EMULATOR GONE at $1" | tee -a "$OUT/crash.txt"; finish; exit 0; }; }
finish() {
  adb logcat -d > "$OUT/logcat.txt"
  [ -f "$OUT/crash.txt" ] || echo "no crashes" > "$OUT/crash.txt"
  cat "$OUT/crash.txt"
}
back() { adb shell input keyevent KEYCODE_BACK; sleep 2; }
crashed() {
  if adb logcat -d | grep -q "FATAL EXCEPTION"; then
    echo "CRASH after: $1" | tee -a "$OUT/crash.txt"
    adb logcat -d | grep -A 40 "FATAL EXCEPTION" | head -80 >> "$OUT/crash.txt"
    adb logcat -c
    adb shell am start -n $APP/.MainActivity; sleep 6
  fi
}
SIZE=$(adb shell wm size | grep -oE "[0-9]+x[0-9]+" | tail -1)
W=${SIZE%x*}; H=${SIZE#*x}
echo "screen $W x $H"

adb logcat -c
adb shell am start -n $APP/.MainActivity
sleep 12
shot 01_home; dump 01_home; crashed home

# --- Settings ---
tap_text "Library"; sleep 3
tap_text "Settings"; sleep 3
shot 02_settings; dump 02_settings; crashed settings
scroll_tap "Listening stats"; sleep 3
shot 03_stats_empty; crashed stats
back
scroll_tap "Track transitions"; sleep 2
shot 04_crossfade_dialog
tap_text "4 s fade"; sleep 1
scroll_tap "Cassette type"; sleep 2
shot 05_models
tap_text "Metal (Type IV)"; sleep 1
scroll_tap "Label color"; sleep 2
shot 06_label_colors
tap_text "Blue"; sleep 1
scroll_tap "Tape hiss"; sleep 1
scroll_tap "Wow & flutter"; sleep 1
scroll_tap "Key sounds"; sleep 1
scroll_tap "Side A / Side B"; sleep 1
shot 07_settings_after; crashed toggles; alive settings
back

# --- Library: folders filter ---
tap_text "Folders"; sleep 3
shot 08_folders; dump 08_folders; crashed folders
tap_text "E2E"; sleep 3
shot 09_folder_page; crashed folder_page
back
tap_text "Folders"; sleep 2

# --- Play an album, open the player ---
tap_text "Albums"; sleep 3
tap_text "E2E Lossless Album"; sleep 3
shot 10_album; dump 10_album; crashed album
tap_text "Play"; sleep 4
# open the full player from the mini player (just above the bottom bar)
dump 11_album_playing
adb shell input tap $((W/2)) $((H*82/100)); sleep 4
shot 12_player; dump 12_player; crashed player; alive player
tap_text "Speed and pitch"; sleep 2
shot 13_speed_sheet; crashed speed_sheet
tap_text "1.25×"; sleep 2
shot 14_speed_125
back
tap_text "Sleep timer"; sleep 2
shot 15_sleep_sheet
tap_text "15 minutes"; sleep 2
shot 16_sleep_set; crashed sleep; alive sleep
sleep 20
shot 17_side_b; crashed side_b
sleep 15
back

# --- Share a mixtape from the album page ---
tap_text "Share mixtape"; sleep 5
shot 18_share_sheet; crashed share; alive share
back; back

# --- Stats after listening (the service saves them when playback pauses) ---
adb shell input keyevent KEYCODE_MEDIA_PAUSE; sleep 2
tap_text "Library"; sleep 3
tap_text "Settings"; sleep 3
scroll_tap "Listening stats"; sleep 3
shot 19_stats; crashed stats_after
back; back

# --- Search inside lyrics ---
tap_text "Search"; sleep 2
tap_text "Lyrics"; sleep 2
shot 20_lyrics_search; crashed lyrics_search

adb shell dumpsys media_session > "$OUT/dumpsys_media_session.txt" 2>&1 || true
finish
echo done
