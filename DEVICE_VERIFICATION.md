# Android device verification

This checklist is required before publishing a production APK. CI debug-signed
artifacts are for testing only; a version-tag build must use the maintainer's
private release keystore.

## Install a CI artifact

1. Download the universal APK for the intended flavor from the GitHub Actions
   run's full-folder artifact.
2. Enable USB debugging on a test phone and confirm it is visible:

```sh
adb devices
```

3. Install the APK:

```sh
adb install -r Setotower-Downloader-*-universal-*.apk
```

## Smoke checklist

- The launcher label is **Setotower Downloader** and the icon is the Setotower
  icon.
- The first launch completes without a crash and the app can request storage or
  notification permissions normally.
- A public YouTube URL can be inspected, a quality can be selected, and the
  resulting file contains both audio and video.
- A Cineb URL can be inspected and downloaded as MP4 with audio.
- A magnet can display its file list, download only selected files, and show
  progress and completion.
- Cancel, retry, rotate, background, and resume behavior do not leave a false
  completed file.
- Downloads are present in the selected device folder and can be opened by a
  normal media player.

## Clean-install gate

After the smoke checklist, uninstall and reinstall the same flavor:

```sh
adb uninstall com.setotower.downloader
adb install Setotower-Downloader-*-universal-*.apk
```

Repeat first-launch setup and confirm no prior downloads, cookies, pairing
tokens, or settings were carried into the clean install.
