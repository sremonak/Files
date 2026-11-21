# Files Pro

Android file manager built with Kotlin, Jetpack Compose and Material 3. It supports file browsing, filtered search, numbered batch rename with preview, copy/move progress, ZIP creation/extraction, storage analysis, trash, a text editor and an optional app lock. The UI is partly localized in English and Bengali. The app requires Android 8.0 or later.

## Build

Install JDK 17, Android SDK Platform 36, and an Android Studio version compatible with AGP 8.10.1. Open the project and sync it, or run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`. Build output is under `app/build/outputs/apk/`. Local release builds are unsigned unless the signing environment variables below are configured; unsigned APKs cannot be installed directly.

## GitHub Actions release signing

The **Build Android APK** workflow runs unit tests and uploads release and debug APK artifacts. It uses the owner's signing key only when all four GitHub Actions repository secrets exist:

- `RELEASE_KEYSTORE_BASE64`: the complete keystore encoded as base64 on one line.
- `RELEASE_STORE_PASSWORD`: the keystore password.
- `RELEASE_KEY_ALIAS`: the signing key alias.
- `RELEASE_KEY_PASSWORD`: the signing key password.

Add these privately under **GitHub repository → Settings → Secrets and variables → Actions → New repository secret**. Do not commit the keystore or passwords and do not paste them into chat. On Linux, `base64 -w 0 path/to/your-keystore.jks` produces the encoded value; on macOS use `base64 -i path/to/your-keystore.jks | tr -d '\n'`. Use the same key for future updates and keep secure backups of it. If no secrets are configured, the workflow creates a fresh temporary signing key and produces an installable `Files-Pro-release-temporary-key` artifact for testing. The key is discarded after the run: a later build has a different signature and cannot update this APK, and uninstalling deletes app-private data including trash. Never use this temporary key for a production release. If only some secrets are configured, the workflow fails rather than silently using the wrong key. After configuring all secrets, manually run the workflow on the working branch and download `Files-Pro-release-owner-key`. The debug artifact remains separately available.

For local signing, set `RELEASE_STORE_FILE` to an absolute keystore path, and set the other three environment variables listed above (no base64 variable needed).

## Important limitations

- All-files access is needed for the full shared-storage file manager on Android 11+. Google Play restricts this permission; review its policy and file the required declaration. Android still protects other apps' private files. Removable volume support depends on device permissions and must be tested on the target hardware.
- Trash is app-private, subject to the configured auto-purge interval, and is deleted on uninstall. Keep independent backups.
- App lock is not file encryption. An encrypted vault is not implemented. Existing PIN hashes are upgraded after successful unlock; the lock now reappears when the app returns from the background.
- ZIP extraction is limited to 10,000 entries and 2 GiB. Search is currently limited to internal storage, eight folder levels and 500 matches. Network/cloud integration, full document previews, background transfers, full Bengali translation and tablet two-pane view are not yet implemented.
- Automated unit tests cover some ZIP, formatting and PIN behavior, but device testing is still needed, especially before using the app on important files or publishing.
