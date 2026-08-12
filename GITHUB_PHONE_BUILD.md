# Build the APK from your Android phone

You do NOT need Android Studio.

1. Create/sign in to a GitHub account.
2. Create a new repository. You can name it `BatchVideoCompressorV2`.
3. Upload all files/folders from this project ZIP to the repository.
4. Make sure `.github/workflows/build-apk.yml` exists exactly at that path.
5. Open the repository's **Actions** tab.
6. Select **Build Android APK**.
7. Tap **Run workflow**.
8. Wait for the green checkmark.
9. Open that completed workflow run.
10. Scroll to **Artifacts**.
11. Download `BatchVideoCompressor-V2-APK`.
12. If GitHub downloads an artifact ZIP, extract it and install the `.apk`.

The workflow uses GitHub Actions to build the debug APK and upload the APK as a workflow artifact. GitHub documents artifacts as files produced by a workflow and retained after the job completes. The upload-artifact action supports uploading a single file without an additional archive.

For a public repository, do not put passwords, signing keys, API keys, or private information in the repository.
