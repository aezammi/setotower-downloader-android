#!/usr/bin/env bash
set -u

recover_adb() {
  adb kill-server >/dev/null 2>&1 || true
  sleep 2
  adb start-server >/dev/null 2>&1 || true
  adb reconnect offline >/dev/null 2>&1 || true
}

wait_for_device() {
  for tick in $(seq 1 60); do
    if [ "$(adb get-state 2>/dev/null || true)" = "device" ]; then
      return 0
    fi
    if [ "$((tick % 8))" -eq 0 ]; then
      recover_adb
    else
      adb reconnect offline >/dev/null 2>&1 || true
    fi
    sleep 2
  done
  return 1
}

wait_for_android_services() {
  for tick in $(seq 1 90); do
    if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && \
       timeout 10s adb shell cmd package list packages >/dev/null 2>&1; then
      return 0
    fi
    if [ "$((tick % 8))" -eq 0 ]; then
      recover_adb
    else
      adb reconnect offline >/dev/null 2>&1 || true
    fi
    sleep 2
  done
  return 1
}

remove_installation() {
  if adb uninstall com.setotower.downloader >/dev/null 2>&1; then
    return 0
  fi
  if adb shell pm uninstall --user 0 com.setotower.downloader 2>/dev/null | grep -q 'Success'; then
    return 0
  fi
  ! adb shell pm list packages 2>/dev/null | grep -q '^package:com\.setotower\.downloader$'
}

for attempt in 1 2; do
  echo "Connected smoke attempt ${attempt}/2"
  if ! wait_for_device; then
    echo "Android emulator did not become ready before attempt ${attempt}"
    exit 1
  fi
  if ! wait_for_android_services; then
    echo "Android package service did not become ready before attempt ${attempt}"
    exit 1
  fi

  echo "Removing any previous Setotower installation before attempt ${attempt}"
  remove_installation || true

  if timeout --foreground 10m ./gradlew --no-daemon --max-workers=2 connectedGithubDebugAndroidTest --warning-mode all; then
    echo "Verifying clean uninstall after successful smoke"
    if ! remove_installation; then
      echo "Setotower uninstall verification failed"
      exit 1
    fi
    exit 0
  fi

  if [ "$attempt" -lt 2 ]; then
    echo "Smoke attempt ${attempt} failed; restarting the emulator before retry"
    adb reboot || true
    recover_adb
    sleep 12
  fi
done

exit 1
