#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME="runnableai-android-builder"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker not found. Install Docker to build in container."
  exit 1
fi

docker build -t ${IMAGE_NAME} -f docker/Dockerfile .
docker run --rm -v "${PWD}":/workspace ${IMAGE_NAME}
