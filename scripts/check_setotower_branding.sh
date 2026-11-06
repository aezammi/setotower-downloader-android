#!/usr/bin/env bash
set -euo pipefail

# Dependency coordinates, translator credits, and license notices may retain
# their upstream attribution. This check is intentionally limited to files
# that users read or that render the app's own visible identity.
patterns='YTDLnis|ytdlnis|deniscerri|github\.com/deniscerri'
targets=(README.md docs app/src/main/java/com/setotower/downloader/ui app/src/main/java/com/setotower/downloader/util app/src/main/res/values)

if matches=$(grep -RInE --exclude='*.png' --exclude='*.jpg' --exclude='*.webp' "$patterns" "${targets[@]}" 2>/dev/null); then
  printf '%s\n' "$matches" >&2
  echo 'stale upstream branding found in user-facing Android files' >&2
  exit 1
fi

echo 'Setotower branding check passed'
