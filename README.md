# Batch Video Compressor V2

A simple Android batch video compressor. Select multiple videos, start compression, and leave the screen while a foreground service processes the batch.

Output is saved to `Movies/BatchVideoCompressor`.

## Build
This repository includes a GitHub Actions workflow at `.github/workflows/build-apk.yml`. Use GitHub Actions to build the debug APK.

## Notes
- Originals are not modified.
- This version uses Media3 Transformer.
- Pause/resume is not implemented for an individual video; Stop cancels the current export.
