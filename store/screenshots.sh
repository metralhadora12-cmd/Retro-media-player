#!/usr/bin/env bash
# Takes the Google Play screenshots on a 1080x1920 emulator with sample music.
set -u
APP=com.retro.cassetteplayer
OUT=store-shots
mkdir -p "$OUT"
ADB=$(command -v adb)
adb() { timeout 60 "$ADB" "$@"; }
shot() { adb exec-out screencap -p > "$OUT/$1.png"; }
dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/tmp.xml" >/dev/null 2>&1; }
center_of() {
  python3 - "$1" "$OUT/tmp.xml" <<'PY'
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
tap_text() { dump; if pos=$(center_of "$1"); then adb shell input tap $pos; else echo "NOT FOUND: $1"; fi; }
scroll_tap() {
  for i in $(seq 1 8); do
    dump; if pos=$(center_of "$1"); then adb shell input tap $pos; return 0; fi
    adb shell input swipe $((W/2)) $((H*3/4)) $((W/2)) $((H/3)) 400; sleep 1
  done
  echo "NOT FOUND after scrolling: $1"
}
back() { adb shell input keyevent KEYCODE_BACK; sleep 2; }
# tap the lowest node with this text (e.g. the song title in the mini player)
tap_last_text() {
  dump
  pos=$(python3 - "$1" "$OUT/tmp.xml" <<'PY2'
import re, sys, xml.etree.ElementTree as ET
best = None
for node in ET.parse(sys.argv[2]).getroot().iter("node"):
    if node.get("text") == sys.argv[1] or node.get("content-desc") == sys.argv[1]:
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        if best is None or y1 > best[1]:
            best = ((x1 + x2) // 2, (y1 + y2) // 2)
# the mini player's pause key: tap its title area, left of the key
print(f"{best[0] // 3} {best[1]}" if best else "")
PY2
)
  if [ -n "$pos" ]; then adb shell input tap $pos; else echo "NOT FOUND: $1"; fi
}

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell mkdir -p /sdcard/Music/Samples
for f in store-media/*; do adb push "$f" /sdcard/Music/Samples/ >/dev/null; done
adb shell pm grant $APP android.permission.READ_MEDIA_AUDIO || true
adb shell pm grant $APP android.permission.POST_NOTIFICATIONS || true
n_files=$(ls store-media | wc -l)
for i in $(seq 1 40); do
  n=$(adb shell content query --uri content://media/external/audio/media --projection is_music 2>/dev/null | grep -c "is_music=1")
  [ "$n" -ge "$n_files" ] && break; sleep 3
done
# Clean status bar (demo mode)
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1030 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null

SIZE=$(adb shell wm size | grep -oE "[0-9]+x[0-9]+" | tail -1); W=${SIZE%x*}; H=${SIZE#*x}
echo "screen $W x $H"

declare -A PT=([library]="Biblioteca" [albums]="Álbuns" [play]="Tocar" [speed]="Velocidade e tom" [sleep]="Timer de sono" [home]="Início" [clear]="Limpar filtro" [settings]="Configurações" [tape]="EFEITOS DE FITA" [stats]="Estatísticas de escuta" [pause]="Pausar")
declare -A EN=([library]="Library" [albums]="Albums" [play]="Play" [speed]="Speed and pitch" [sleep]="Sleep timer" [home]="Home" [clear]="Clear filter" [settings]="Settings" [tape]="TAPE EFFECTS" [stats]="Listening stats" [pause]="Pause")

pass() {
  local lang=$1; local -n L=$2
  mkdir -p "$OUT/$lang"
  adb shell am force-stop $APP
  adb shell cmd locale set-app-locales $APP --locales "$3"
  adb shell am start -n $APP/.MainActivity; sleep 10
  tap_text "${L[library]}"; sleep 3
  tap_text "${L[albums]}"; sleep 3
  scroll_tap "Midnight Drive"; sleep 3
  shot "$lang/04_album"
  tap_text "${L[play]}"; sleep 4
  tap_last_text "${L[pause]}"; sleep 4
  shot "$lang/01_player"
  tap_text "${L[sleep]}"; sleep 2
  shot "$lang/06_sleep_timer"
  back
  sleep 35
  back
  tap_text "${L[home]}"; sleep 4
  shot "$lang/02_home"
  tap_text "${L[library]}"; sleep 3
  tap_text "${L[clear]}"; sleep 2
  shot "$lang/03_library"
  tap_text "${L[settings]}"; sleep 3
  scroll_tap "${L[tape]}"; sleep 1
  adb shell input swipe $((W/2)) $((H/2)) $((W/2)) $((H/4)) 400; sleep 1
  shot "$lang/05_settings"
  adb shell input keyevent KEYCODE_MEDIA_PAUSE; sleep 2
  for i in 1 2 3 4; do adb shell input swipe $((W/2)) $((H/3)) $((W/2)) $((H*9/10)) 300; sleep 1; done
  tap_text "${L[stats]}"; sleep 3
  shot "$lang/07_stats"
}
pass pt-BR PT pt-BR
pass en-US EN en-US
rm -f "$OUT/tmp.xml"
echo done
