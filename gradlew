#!/usr/bin/env sh
# Lightweight Gradle bootstrap so the project can build from Termux without a preinstalled Gradle.
set -eu
GRADLE_VERSION="8.11.1"
GRADLE_HOME_BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/nurquran-distribution"
GRADLE_DIR="$GRADLE_HOME_BASE/gradle-$GRADLE_VERSION"
if [ ! -x "$GRADLE_DIR/bin/gradle" ]; then
    mkdir -p "$GRADLE_HOME_BASE"
    ZIP="$GRADLE_HOME_BASE/gradle-$GRADLE_VERSION-bin.zip"
    if [ ! -s "$ZIP" ]; then
        echo "Mengunduh Gradle $GRADLE_VERSION..."
        if command -v curl >/dev/null 2>&1; then
            curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
        elif command -v wget >/dev/null 2>&1; then
            wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
        else
            echo "Perlu curl atau wget untuk mengunduh Gradle." >&2
            exit 1
        fi
    fi
    command -v unzip >/dev/null 2>&1 || { echo "Perlu unzip. Jalankan: pkg install unzip" >&2; exit 1; }
    unzip -oq "$ZIP" -d "$GRADLE_HOME_BASE"
fi
exec "$GRADLE_DIR/bin/gradle" "$@"
