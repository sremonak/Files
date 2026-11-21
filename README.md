# Files Pro

An Android file manager project written in Kotlin with Jetpack Compose and Material 3. The source includes file browsing, search, file details, a text editor, ZIP tools, a recycle bin, a storage analyzer, favorites, theme settings and optional app locking. Some UI text remains in English when Bengali is selected.

## Build a debug APK

1. Install Android Studio, JDK 17 and the Android SDK Platform 34. Ensure the machine can access Google Maven, Maven Central and the Gradle distribution service.
2. Open this repository in Android Studio and allow Gradle to sync. Install any SDK components requested by Android Studio.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**. The debug APK is normally created at `app/build/outputs/apk/debug/app-debug.apk`.

Alternatively, on macOS/Linux run `./gradlew assembleDebug` from the project root. A release APK needs your own signing configuration.

## Permissions and limitations

- Supports Android 8.0 (API 26) and later. On Android 11 and later, browsing shared storage requires the user to grant **All files access** in system settings. Google Play restricts this permission to qualifying apps; review its policies before publishing. On older Android releases the app requests storage permissions. Access to protected system and other apps' private directories is still restricted by Android.
- The recycle bin stores files in app-private storage; uninstalling the app removes its contents. Trash can be automatically purged according to the configured retention period. Keep independent backups of important files.
- App lock is a convenience gate, **not encryption**; it does not secure files against other apps with storage access. The PIN is stored as a hash, and the lock is applied when the app starts rather than serving as a secure vault.
- Ringtone setting may require system permission to modify settings; functionality depends on Android version and device. Dynamic colors require Android 12 or later.
- This project has not been built or tested on a device in the current environment. Treat it as unverified source, test on expendable files first, and verify file operations and permissions before relying on it.

## Build with GitHub Actions

The **Build Android APK** workflow runs for pushes to this project's working branch, pull requests, and manual runs. To run it manually, open **Actions → Build Android APK → Run workflow**, select the branch, and run it. When the job succeeds, download **Files-Pro-debug-apk** from the run's Artifacts section. This is a debug-signed APK, not a Play Store release. Artifacts are kept for 30 days.
