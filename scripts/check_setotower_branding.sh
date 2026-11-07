#!/usr/bin/env bash
set -euo pipefail

# Dependency coordinates, translator credits, and license notices may retain
# their upstream attribution. This check is intentionally limited to files
# that users read or that render the app's own visible identity.
patterns='YTDLnis|ytdlnis|deniscerri|Denis Çerri|github\.com/deniscerri|JunkFood02/Seal'
targets=(README.md docs app/src/main/java/com/setotower/downloader/ui app/src/main/java/com/setotower/downloader/util app/src/main/res/values)

if matches=$(grep -RInE --exclude='*.png' --exclude='*.jpg' --exclude='*.webp' "$patterns" "${targets[@]}" 2>/dev/null); then
  printf '%s\n' "$matches" >&2
  echo 'stale upstream branding found in user-facing Android files' >&2
  exit 1
fi

# Keep the public repository preview aligned with the installed app icon.
# The source mark is the same asset used by the Setotower web favicon.
expected_mark='e7f9a4ca6e5f240e83329ada9e2ad81f1be1a63e74adad075aa858b8f9cc0e9c'
metadata_icon='fastlane/metadata/android/en-US/images/icon.png'
actual_mark=$(sha256sum "$metadata_icon" | awk '{print $1}')
if [[ "$actual_mark" != "$expected_mark" ]]; then
  echo "public metadata icon is not the Setotower mark: $metadata_icon" >&2
  exit 1
fi

echo 'Setotower branding check passed'
