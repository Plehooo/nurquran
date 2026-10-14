#!/usr/bin/env sh
# Bootstrap launcher for this archive. To create the standard Gradle Wrapper JAR, run:
#   gradle wrapper --gradle-version 8.13 --distribution-type bin
# In CI, gradle/actions/setup-gradle installs the pinned Gradle version before this script runs.
set -eu
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "Gradle is not installed. Install Gradle 8.13 or run: gradle wrapper --gradle-version 8.13 --distribution-type bin" >&2
exit 127
