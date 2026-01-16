# RunnableAI (Option C: llama.cpp + ONNX + ExecuTorch)

RunnableAI is an Android app scaffold that follows the Option C architecture:
- **GGUF (llama.cpp)** for LLM + GGUF backbones
- **ONNX Runtime** for codecs and ASR
- **ExecuTorch** for PyTorch-native TTS models exported to `.pte`

The app ships only runtimes. Models are downloaded post-install using a catalog JSON and stored in app-private storage.

Status: scaffold with placeholder TTS tone and model-specific pre/post-processing still to be wired.

## Prerequisites

- Docker (for reproducible builds)
- Android Studio (optional, for debugging and device install)

## Quick start

1. Update `app/src/main/assets/catalog.json` with your model URLs and hashes.
2. Build the APK with Docker.
3. Install `app/build/outputs/apk/debug/app-debug.apk` on a device or emulator.

## Build (Docker only)

```bash
./scripts/build-in-docker.sh
```

APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Model catalog

Edit `app/src/main/assets/catalog.json` to point to your model files. Each entry supports:
- `task`: `CHAT`, `ASR`, `TTS`, `CODEC`
- `runtime`: `LLAMA_CPP`, `ONNX`, or `EXECUTORCH`
- `artifacts`: list of named files (each stored in its own model folder)
- `depends_on`: optional dependencies (e.g., codec decoder)

## Storage layout (on device)

```
<filesDir>/models/
  <modelId>/
    model.gguf
    model.onnx
    model.pte
    installed.json
```

## Downloads

Downloads are handled by WorkManager via `ModelDownloadWorker`, supporting:
- resume (`.part` files)
- sha256 verification
- atomic rename to final artifact name
- dependency downloads

## Notes

- ExecuTorch is integrated via Maven (`org.pytorch:executorch-android:1.0.0`).
- llama.cpp is built from source via CMake + FetchContent (pinned commit).
- ONNX Runtime is integrated via Maven (`com.microsoft.onnxruntime:onnxruntime-android`).
- The TTS pipeline uses a placeholder tone until you wire model-specific pre/post-processing.

## Next steps

1. Replace catalog URLs and sha256 values with your hosted model files.
2. Implement TTS/ASR pre/post-processing for your specific models.
3. Consider adding a dedicated inference process and Binder interface for large models.
