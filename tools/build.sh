#!/usr/bin/env bash
set -euo pipefail
ROOT=$(dirname "$(dirname "$(realpath "$0")")")
export MLIB_STATE=${MLIB_STATE:-${XDG_STATE_HOME:-$HOME/.local/state}/mlib}
MLIB_STATE=$(realpath -m "$MLIB_STATE")
export MLIB_STATE
case "$MLIB_STATE/" in "$ROOT/"*) printf 'MLIB_STATE must be outside source.\n' >&2; exit 2 ;; esac
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK directory}"
MODE=${1:-debug}
case "$MODE" in debug) TASK=assembleDebug ;; release) TASK=assembleRelease ;; *) printf 'Usage: %s [debug|release]\n' "$0"; exit 2 ;; esac
mkdir -p "$MLIB_STATE"
if [ ! -f "$MLIB_STATE/jniLibs/arm64-v8a/libtdjsonjava.so" ] || [ ! -f "$MLIB_STATE/jniLibs/x86_64/libtdjsonjava.so" ]; then
  bash "$ROOT/tools/build-native.sh"
fi
if [ "$MODE" = release ] && [ ! -f "$MLIB_STATE/signing.properties" ]; then
  printf 'MLib release signing is missing. Create the MLib signing key before a release build.\n' >&2
  exit 1
fi
exec "$ROOT/gradlew" -p "$ROOT" --project-cache-dir "$MLIB_STATE/gradle" \
  -Pkotlin.project.persistent.dir="$MLIB_STATE/kotlin" ":app:$TASK" --console=plain --no-daemon
