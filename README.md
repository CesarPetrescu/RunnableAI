<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Min%20SDK-24-blue" alt="Min SDK 24"/>
  <img src="https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Compose-1.5-4285F4?logo=jetpack-compose&logoColor=white" alt="Compose"/>
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License"/>
</p>

<h1 align="center">🤖 RunnableAI</h1>

<p align="center">
  <strong>On-device AI inference for Android</strong><br/>
  Chat • Speech-to-Text • Text-to-Speech
</p>

---

## ✨ Features

| Capability | Runtime | Model Format |
|------------|---------|--------------|
| 💬 **LLM Chat** | llama.cpp | `.gguf` |
| 🎤 **Speech Recognition (ASR)** | ONNX Runtime | `.onnx` |
| 🔊 **Text-to-Speech (TTS)** | ExecuTorch / ONNX | `.pte` / `.onnx` |
| 🎵 **Audio Codecs** | ONNX Runtime | `.onnx` |

- 📦 **Zero bundled models** — download only what you need post-install
- 🔒 **100% on-device** — no cloud, no data leaves your phone
- ⚡ **Hardware acceleration** — GPU layers for llama.cpp, NNAPI for ONNX
- 📱 **Modern UI** — Jetpack Compose with Material 3

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐    │
│  │ ChatScreen  │  │ModelsScreen │  │ ModelDetailScreen│    │
│  └──────┬──────┘  └──────┬──────┘  └────────┬─────────┘    │
│         └────────────────┴──────────────────┘              │
│                          │                                  │
│                   MainViewModel                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────┴──────────────────────────────────┐
│                      Domain Layer                           │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              InferenceOrchestrator                    │  │
│  │  ┌──────────┐  ┌───────────┐  ┌───────────┐          │  │
│  │  │ChatHelper│  │ TtsHelper │  │ AsrHelper │          │  │
│  │  └──────────┘  └───────────┘  └───────────┘          │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────┴──────────────────────────────────┐
│                     Backend Layer                           │
│  ┌────────────────┐ ┌────────────────┐ ┌─────────────────┐  │
│  │  LlamaBackend  │ │   OnnxBackend  │ │ExecuTorchBackend│  │
│  │    (JNI)       │ │    (Maven)     │ │     (Maven)     │  │
│  └───────┬────────┘ └────────────────┘ └─────────────────┘  │
│          │                                                   │
│    ┌─────┴─────┐                                            │
│    │llama.cpp  │  Native C++ via CMake FetchContent         │
│    └───────────┘                                            │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites

- **Docker** (recommended for reproducible builds)
- Android device with **arm64-v8a** architecture

### Build

```bash
# Clone the repository
git clone https://github.com/your-org/RunnableAI.git
cd RunnableAI

# Build APK using Docker
./scripts/build-in-docker.sh

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 Model Catalog

Models are defined in [`app/src/main/assets/catalog.json`](app/src/main/assets/catalog.json):

```json
{
  "id": "llm-llama3-8b-q4",
  "name": "Llama 3 8B Instruct Q4",
  "task": "CHAT",
  "runtime": "LLAMA_CPP",
  "artifacts": [
    {
      "name": "model.gguf",
      "url": "https://huggingface.co/.../model.gguf",
      "sha256": "abc123...",
      "bytes": 4500000000
    }
  ],
  "requirements": {
    "min_ram_mb": 6000,
    "preferred_abi": "arm64-v8a"
  }
}
```

### Supported Fields

| Field | Values | Description |
|-------|--------|-------------|
| `task` | `CHAT`, `ASR`, `TTS`, `CODEC` | Model capability |
| `runtime` | `LLAMA_CPP`, `ONNX`, `EXECUTORCH` | Inference backend |
| `depends_on` | `["model-id"]` | Required dependencies |
| `artifacts` | `[{name, url, sha256, bytes}]` | Model files to download |

---

## 📁 Storage Layout

```
<app-private-storage>/models/
├── llm-llama3-8b-q4/
│   ├── model.gguf
│   └── installed.json
├── parakeet-tdt-0.6b-v2-onnx/
│   ├── encoder-model.int8.onnx
│   ├── decoder_joint-model.int8.onnx
│   ├── vocab.txt
│   └── installed.json
└── pocket-tts-executorch/
    ├── pocket_tts.pte
    ├── tokenizer.json
    └── installed.json
```

---

## ⚙️ Technical Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Kotlin | 1.9.22 |
| UI | Jetpack Compose | 1.5.14 |
| Build | Gradle (Kotlin DSL) | 8.7 |
| Min SDK | Android 7.0 | API 24 |
| Target SDK | Android 14 | API 34 |
| NDK | r26d | 26.3.11579264 |

### Runtimes

| Runtime | Source | Notes |
|---------|--------|-------|
| **llama.cpp** | CMake FetchContent | Pinned commit `cff777f` |
| **ONNX Runtime** | Maven | `com.microsoft.onnxruntime:onnxruntime-android:1.17.1` |
| **ExecuTorch** | Maven | `org.pytorch:executorch-android:1.0.0` |

---

## 🔧 Development

### Project Structure

```
app/src/main/
├── java/ai/runnable/local/
│   ├── backends/          # Runtime implementations
│   │   ├── llama/         # llama.cpp JNI wrapper
│   │   ├── onnx/          # ONNX Runtime wrapper
│   │   └── executorch/    # ExecuTorch wrapper
│   ├── data/              # Model catalog, downloads, storage
│   ├── domain/            # Business logic, orchestration
│   └── ui/                # Compose screens & components
├── cpp/                   # Native C++ (llama.cpp bindings)
├── assets/                # catalog.json
└── res/                   # Android resources
```

### Building Locally (without Docker)

Requires Android SDK + NDK installed:

```bash
./gradlew assembleDebug
```

### Running Lint

```bash
./gradlew lint
```

---

## 📋 Roadmap

- [ ] Model-specific TTS pre/post-processing
- [ ] Model-specific ASR pre/post-processing  
- [ ] Streaming token generation callback
- [ ] Dedicated inference process + Binder IPC
- [ ] Model quantization selector
- [ ] Voice activity detection (VAD)

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <sub>Built with ❤️ for on-device AI</sub>
</p>
