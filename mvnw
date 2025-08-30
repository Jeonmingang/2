#!/bin/sh
set -e
WRAPPER_DIR="$(cd "$(dirname "$0")" && pwd)/.mvn/wrapper"
PROPS="$WRAPPER_DIR/maven-wrapper.properties"
JAR="$WRAPPER_DIR/maven-wrapper.jar"

if [ ! -f "$JAR" ]; then
  if [ ! -f "$PROPS" ]; then
    echo "[ERROR] $PROPS not found."
    exit 1
  fi
  URL=$(grep -E '^wrapperUrl=' "$PROPS" | sed 's/^wrapperUrl=//')
  echo "Downloading Maven Wrapper Jar from $URL"
  mkdir -p "$WRAPPER_DIR"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$URL" -o "$JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$URL" -O "$JAR"
  else
    # fallback to powershell if available (on modern Windows bash)
    if command -v powershell >/dev/null 2>&1; then
      powershell -NoLogo -NoProfile -Command "Invoke-WebRequest -Uri '$URL' -OutFile '$JAR'"
    else
      echo "[ERROR] Need curl or wget to download Maven wrapper."
      exit 1
    fi
  fi
fi

JAVA_EXE="java"
exec "$JAVA_EXE" -cp "$JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
