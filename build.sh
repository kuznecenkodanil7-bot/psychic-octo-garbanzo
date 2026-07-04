#!/usr/bin/env bash
set -euo pipefail
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle не найден. Проще всего собрать через GitHub Actions: Build MiniBaritone 1.21.11."
  exit 1
fi
gradle build --stacktrace
