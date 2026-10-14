#!/usr/bin/env bash
set -euo pipefail

REPO_URL="${1:-}"
if [[ -z "$REPO_URL" ]]; then
  read -r -p "URL repo GitHub kosong (contoh https://github.com/USERNAME/nurquran.git): " REPO_URL
fi
if [[ "$REPO_URL" != https://github.com/* ]]; then
  echo "Masukkan URL repository GitHub HTTPS yang valid." >&2
  exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git init
fi
git branch -M main
if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REPO_URL"
else
  git remote add origin "$REPO_URL"
fi

git add .
if ! git diff --cached --quiet; then
  git commit -m "Build Nur Quran Android app"
else
  echo "Tidak ada perubahan baru untuk di-commit."
fi
git push -u origin main

echo "Selesai push. Buka tab Actions di repo GitHub untuk mengambil artifact NurQuran-debug-apk."
