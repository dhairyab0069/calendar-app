#!/usr/bin/env bash
# Build a macOS .dmg of the Calendar app with jpackage (bundles its own Java runtime).
# Usage: packaging/build-installer.sh dmg [version] [output-dir]
#   version     defaults to the `version '...'` line in archive/java/build.gradle
#   output-dir  defaults to ./dist
#
# The app is unsigned. jpackage ad-hoc signs the bundle by default on macOS 12+ runners,
# but it is not notarized, so Gatekeeper blocks the first launch: right-click the app,
# choose Open (or run `xattr -dr com.apple.quarantine /Applications/Calendar.app`).
set -euo pipefail

TYPE="${1:-}"
if [[ "$TYPE" != "dmg" ]]; then
  echo "usage: $0 dmg [version] [output-dir]   (only 'dmg' is implemented)" >&2
  exit 2
fi
if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "the dmg can only be built on macOS" >&2
  exit 2
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_DIR="$ROOT/archive/java"

JAR_VERSION="$(sed -nE "s/^version[[:space:]]+'([^']+)'.*/\1/p" "$JAVA_DIR/build.gradle" | head -n1)"
if [[ -z "$JAR_VERSION" ]]; then
  echo "could not read version from archive/java/build.gradle" >&2
  exit 1
fi
VERSION="${2:-$JAR_VERSION}"
mkdir -p "${3:-$ROOT/dist}"
OUT="$(cd "${3:-$ROOT/dist}" && pwd)"

(cd "$JAVA_DIR" && ./gradlew jar --console=plain)

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
mkdir "$WORK/input" "$WORK/dest"
cp "$JAVA_DIR/build/libs/calendar-$JAR_VERSION.jar" "$WORK/input/"

jpackage \
  --name Calendar \
  --app-version "$VERSION" \
  --vendor "Dhairya Bhatia" \
  --description "Multi-calendar scheduler with a GUI, a prompt and scripting" \
  --input "$WORK/input" \
  --main-jar "calendar-$JAR_VERSION.jar" \
  --main-class CalendarRunner \
  --add-modules java.desktop,java.logging,java.prefs,java.base \
  --icon "$ROOT/packaging/macos/AppIcon.icns" \
  --mac-package-name Calendar \
  --mac-package-identifier io.github.dhairyab0069.calendar \
  --type dmg \
  --dest "$WORK/dest"

mv "$WORK/dest/Calendar-$VERSION.dmg" "$OUT/Calendar-$VERSION-macos.dmg"
echo "built $OUT/Calendar-$VERSION-macos.dmg"
