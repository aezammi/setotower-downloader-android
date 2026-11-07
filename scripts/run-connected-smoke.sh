#!/usr/bin/env bash
set -u

wait_for_device() {
  for _ in $(seq 1 60); do
    if [ "$(adb get-state 2>/dev/null || true)" = "device" ]; then
      return 0
    fi
    adb reconnect offline >/dev/null 2>&1 || true
    sleep 2
  done
  return 1
}

for attempt in 1 2; do
  echo "Connected smoke attempt ${attempt}/2"
  if ! wait_for_device; then
    echo "Android emulator did not become ready before attempt ${attempt}"
    exit 1
  fi

  echo "Removing any previous Setotower installation before attempt ${attempt}"
  adb uninstall com.setotower.downloader >/dev/null 2>&1 || true

  if timeout --foreground 10m ./gradlew --no-daemon --max-workers=2 connectedGithubDebugAndroidTest --warning-mode all; then
    echo "Verifying clean uninstall after successful smoke"
    if ! adb uninstall com.setotower.downloader; then
      echo "Setotower uninstall verification failed"
      exit 1
    fi
    exit 0
  fi

  if [ "$attempt" -lt 2 ]; then
    echo "Smoke attempt ${attempt} failed; restarting the emulator before retry"
    adb reboot || true
    sleep 12
  fi
done

exit 1
