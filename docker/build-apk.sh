#!/usr/bin/env bash
set -euo pipefail

cd /workspace

export GRADLE_USER_HOME=/workspace/.gradle

echo "Building APK inside container..."
gradle --no-daemon clean assembleDebug

echo "APK ready: /workspace/app/build/outputs/apk/debug/app-debug.apk"
