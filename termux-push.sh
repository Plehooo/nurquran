#!/usr/bin/env bash
set -euo pipefail

REPO_URL="${1:-https://github.com/Plehooo/nurquran.git}"
if [[ "$REPO_URL" != https://github.com/* ]]; then
  echo "Masukkan URL GitHub HTTPS yang valid." >&2
  exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  cat >&2 <<'MSG'
Jalankan script dari folder hasil `git clone`, bukan dari ZIP yang belum memiliki .git.
Contoh aman:
  cd ~
  git clone https://github.com/Plehooo/nurquran.git NurQuran
  cd ~/NurQuran
  unzip -o ~/storage/downloads/NurQuran-Patch-Termux.zip -d .
  ./termux-push.sh https://github.com/Plehooo/nurquran.git
MSG
  exit 2
fi

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"
BRANCH="$(git branch --show-current)"
if [[ -z "$BRANCH" ]]; then
  BRANCH="main"
  git checkout -B "$BRANCH"
fi

if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REPO_URL"
else
  git remote add origin "$REPO_URL"
fi

git add \
  gradlew \
  app/src/main/java/com/ditzy/nurquran/MainActivity.kt \
  app/src/main/java/com/ditzy/nurquran/data/IslamicRepository.kt \
  app/src/main/java/com/ditzy/nurquran/data/QuranAudioController.kt \
  app/src/main/java/com/ditzy/nurquran/prayer/PrayerAlarm.kt \
  app/build.gradle.kts \
  .github/workflows/android.yml \
  docs/ADHAN_AUDIO_LICENSE.md \
  README.md \
  termux-push.sh

if ! git diff --cached --quiet; then
  git commit -m "Improve Quran recitation and recurring prayer alarms"
else
  echo "Tidak ada perubahan baru pada file peningkatan Nur Qur'an."
fi

# Rebase local commits onto the latest remote history; never force-push or delete remote files.
git fetch origin
if git ls-remote --exit-code --heads origin "$BRANCH" >/dev/null 2>&1; then
  git pull --rebase origin "$BRANCH"
fi
git push -u origin "$BRANCH"
echo "Push selesai. Periksa tab Actions untuk artifact APK."
