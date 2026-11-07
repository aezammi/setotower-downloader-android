#!/usr/bin/env bash
set -u

for attempt in 1 2; do
  echo "Connected smoke attempt ${attempt}/2"
  if ./gradlew --no-daemon --max-workers=2 connectedGithubDebugAndroidTest --warning-mode all; then
    exit 0
  fi

  if [ "$attempt" -lt 2 ]; then
    echo "Smoke attempt ${attempt} failed; restarting the emulator before retry"
    adb reboot || true
    sleep 12
  fi
done

exit 1
