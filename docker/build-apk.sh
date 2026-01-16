#!/usr/bin/env bash
set -euo pipefail

cd /workspace

export GRADLE_USER_HOME=/workspace/.gradle
export GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx1g"

echo "Building APK inside container..."
gradle --no-daemon assembleDebug

echo "APK ready: /workspace/app/build/outputs/apk/debug/app-debug.apk"
