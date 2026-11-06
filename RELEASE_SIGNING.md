# Android production signing

The public CI workflow intentionally produces `debug-keystore-ci-only` APKs
until the maintainer configures the production key. Never commit the keystore,
`keystore.properties`, or either secret value.

## Create and back up the key

Run this on a private machine and store the keystore in an encrypted backup:

```sh
keytool -genkeypair -v \
  -keystore setotower-release.jks \
  -alias setotower \
  -keyalg RSA -keysize 4096 -validity 10000
```

The key must be retained permanently; losing it prevents updates to installed
production builds.

## Configure GitHub Actions

Create a temporary properties file locally. Replace the placeholders with the
passwords selected when the key was created:

```properties
signingConfig.storeFile=app/release_keystore.jks
signingConfig.storePassword=REPLACE_ME
signingConfig.keyAlias=setotower
signingConfig.keyPassword=REPLACE_ME
```

Upload the two repository secrets without printing their values:

```sh
base64 -w0 setotower-release.jks > signing-key.b64
gh secret set BASE_64_SIGNING_KEY < signing-key.b64
gh secret set LOCAL_PROPERTIES < keystore.properties
```

After uploading, securely delete the temporary base64 and properties files.
Confirm only the secret names are visible:

```sh
gh secret list --repo aezammi/setotower-downloader-android
```

## Release gate

Version-tag builds fail unless both secrets exist. After configuring them, push
a version tag and verify that every artifact's `BUILD-METADATA.txt` says
`signing=private-keystore` before publishing the release.
