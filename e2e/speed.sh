#!/usr/bin/env bash
# Measures the real playback speed of many formats / sample rates on the emulator:
# for each track, how many seconds of song advance in ~12 s of wall-clock time.
# Run once with the tape effects off and once with hiss + wow & flutter on.
set -u
APP=com.retro.cassetteplayer
OUT=e2e-out
mkdir -p "$OUT"
ADB=$(command -v adb)
adb() { timeout 60 "$ADB" "$@"; }
dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/tmp.xml" >/dev/null 2>&1; }
# prints: "<title>|<position seconds>" read from the player screen
read_player() {
  dump
  python3 - "$OUT/tmp.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
titles = {"Rate 22k Mono MP3","Rate 44k MP3","FLAC 16-bit 44k","FLAC 24-bit 48k","FLAC 24-bit 96k","ALAC 16-bit 44k","AAC 44k","Opus 48k","Vorbis 44k","WAV 16-bit 22k"}
title, pos = "", ""
try:
    root = ET.parse(sys.argv[1]).getroot()
except Exception:
    print("|"); sys.exit()
for node in root.iter("node"):
    t = node.get("text") or ""
    if t in titles and not title: title = t
    m = re.fullmatch(r"(\d+):(\d\d)", t)
    if m and pos == "": pos = str(int(m.group(1)) * 60 + int(m.group(2)))
print(f"{title}|{pos}")
PY
}
center_of() {
  python3 - "$1" "$OUT/tmp.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
for node in ET.parse(sys.argv[2]).getroot().iter("node"):
    if node.get("text") == sys.argv[1] or node.get("content-desc") == sys.argv[1]:
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        print((x1 + x2) // 2, (y1 + y2) // 2); sys.exit(0)
sys.exit(1)
PY
}
tap_text() { dump; if pos=$(center_of "$1"); then adb shell input tap $pos; else echo "NOT FOUND: $1"; fi; }

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell mkdir -p /sdcard/Music/Speed
for f in speed-media/*; do adb push "$f" /sdcard/Music/Speed/ >/dev/null; done
adb shell pm grant $APP android.permission.READ_MEDIA_AUDIO || true
adb shell pm grant $APP android.permission.POST_NOTIFICATIONS || true
n_files=$(ls speed-media | wc -l)
for i in $(seq 1 40); do
  n=$(adb shell content query --uri content://media/external/audio/media --projection is_music 2>/dev/null | grep -c "is_music=1")
  [ "$n" -ge "$n_files" ] && break; sleep 3
done
SIZE=$(adb shell wm size | grep -oE "[0-9]+x[0-9]+" | tail -1); W=${SIZE%x*}; H=${SIZE#*x}

measure_pass() {
  local label=$1
  adb logcat -c
  adb shell am start -n $APP/.MainActivity; sleep 10
  tap_text "Library"; sleep 3
  adb shell input swipe $((W*9/10)) $((H*17/100)) $((W/10)) $((H*17/100)) 400; sleep 1
  tap_text "Songs"; sleep 3
  tap_text "AAC 44k"; sleep 3
  # open the player from the mini player (left of its pause key)
  dump
  pp=$(python3 - "$OUT/tmp.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
best = None
for n in ET.parse(sys.argv[1]).getroot().iter("node"):
    if n.get("content-desc") == "Pause":
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
        if best is None or y1 > best[1]: best = ((x1 + x2) // 2, (y1 + y2) // 2)
print(f"{best[0] // 3} {best[1]}" if best else "")
PY
)
  [ -n "$pp" ] && adb shell input tap $pp
  sleep 3
  for i in $(seq 1 10); do
    sleep 2
    t0=$(date +%s.%N); a=$(read_player)
    sleep 12
    t1=$(date +%s.%N); b=$(read_player)
    python3 - "$label" "$a" "$b" "$t0" "$t1" <<'PY' | tee -a "$OUT/speed.txt"
import sys
label, a, b, t0, t1 = sys.argv[1:]
ta, pa = a.split("|"); tb, pb = b.split("|")
wall = float(t1) - float(t0)
if ta and ta == tb and pa and pb:
    print(f"{label:12s} {ta:22s} advanced {int(pb) - int(pa):3d}s in {wall:4.1f}s  -> speed {((int(pb) - int(pa)) / wall):.2f}x")
else:
    print(f"{label:12s} could not read ({a} -> {b})")
PY
    tap_text "Next track"
  done
  adb exec-out screencap -p > "$OUT/player_$label.png"
  adb logcat -d | grep -iE "AudioTrack|DefaultAudioSink|AudioSink|ExoPlayer|retro" | tail -200 > "$OUT/logcat_$label.txt"
  adb shell am force-stop $APP; sleep 2
}

measure_pass effects-off
# Tape hiss + wow & flutter on (the app is debuggable, so run-as can write its settings)
cat > "$OUT/settings.xml" <<'XML'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="tape_hiss" value="true" />
    <boolean name="wow_flutter" value="true" />
</map>
XML
adb push "$OUT/settings.xml" /data/local/tmp/settings.xml >/dev/null
adb shell chmod 644 /data/local/tmp/settings.xml
adb shell run-as $APP mkdir -p shared_prefs
adb shell run-as $APP cp /data/local/tmp/settings.xml shared_prefs/settings.xml
adb shell run-as $APP cat shared_prefs/settings.xml
measure_pass effects-on
cat "$OUT/speed.txt"
echo done
