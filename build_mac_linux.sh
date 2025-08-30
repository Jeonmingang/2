#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
chmod +x mvnw
./mvnw -DskipTests clean package
echo
echo "=== Done ==="
echo "Output: target/SamSkyBridge-0.2.0.jar"
