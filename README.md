# Batch Video Compressor V2

## New in V2
- Foreground service for long-running compression.
- Persistent Android notification with progress.
- Continue while the user leaves the app / screen is off (subject to Android/device power restrictions).
- Pause, resume, and stop controls.
- Overall batch progress.
- Current video progress.
- Elapsed time.
- Estimated remaining time.
- Remembers the selected folder.
- Original videos remain untouched.
- Output goes to `Compressed/`.

## Build
Open the project in Android Studio, allow Gradle sync, then:
Build > Build APK(s)

Debug APK:
`app/build/outputs/apk/debug/app-debug.apk`

## Notes
This is a local/offline compressor. It does not upload videos.

The app processes direct video files in the selected folder, not recursive subfolders.

Android vendors can still restrict background work with aggressive battery optimization. The foreground service and persistent notification substantially improve reliability, but no Android app can guarantee uninterrupted execution if the OS or user force-stops it.


## GitHub Cloud Build

See `GITHUB_PHONE_BUILD.md`. The included GitHub Actions workflow builds an installable debug APK automatically and uploads it as a workflow artifact.
