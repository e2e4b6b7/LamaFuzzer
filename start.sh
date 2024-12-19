#!/usr/bin/env bash

# Exit immediately if a command exits with a non-zero status.
set -e

# Define the image and container names
IMAGE_NAME="lama-fuzzing-image"
CONTAINER_NAME="lama-fuzzing-container"

cd lama

# Build (or rebuild) the Docker image
echo "Building Docker image '${IMAGE_NAME}'..."
docker build -t "${IMAGE_NAME}" .

# Check if a container with the specified name already exists
CONTAINER_EXISTS=$(docker ps -a --format '{{.Names}}' | grep -w "${CONTAINER_NAME}" || true)

if [ -n "${CONTAINER_EXISTS}" ]; then
    # The container exists. Check if it's running.
    CONTAINER_RUNNING=$(docker ps --format '{{.Names}}' | grep -w "${CONTAINER_NAME}" || true)

    if [ -n "${CONTAINER_RUNNING}" ]; then
        # Container is already running, so we just continue and use it.
        echo "Container '${CONTAINER_NAME}' is already running."
    else
        # Container exists but is stopped. Restart it.
        echo "Container '${CONTAINER_NAME}' exists but stopped. Starting it..."
        docker start "${CONTAINER_NAME}"
    fi
else
    # No container with this name exists, so create and run a new one.
    echo "Creating and running a new container '${CONTAINER_NAME}' from image '${IMAGE_NAME}'..."
    docker run -d --name "${CONTAINER_NAME}" "${IMAGE_NAME}"
fi

cd ..
./gradlew fatJar
java -jar build/libs/LamaFuzzer-fat.jar "${CONTAINER_NAME}"
