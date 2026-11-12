#!/usr/bin/env bash
set -euo pipefail
ROOT=$(dirname "$(dirname "$(realpath "$0")")")
: "${MLIB_STATE:?Set MLIB_STATE to a directory outside the source tree}"
MLIB_STATE=$(realpath -m "$MLIB_STATE")
export MLIB_STATE
case "$MLIB_STATE/" in "$ROOT/"*) printf 'MLIB_STATE must be outside source.\n' >&2; exit 2 ;; esac
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
NDK="$ANDROID_HOME/ndk/28.2.13676358"
JOBS=${MLIB_JOBS:-2}
TD_COMMIT=d1085f9cebc5a62379991ae1652673954f229c1f
mkdir -p "$MLIB_STATE/native" "$MLIB_STATE/jniLibs"
if [ ! -d "$MLIB_STATE/tdlib-src/.git" ]; then
  git init "$MLIB_STATE/tdlib-src"
  git -C "$MLIB_STATE/tdlib-src" remote add origin https://github.com/tdlib/td.git
  git -C "$MLIB_STATE/tdlib-src" fetch --depth 1 origin "$TD_COMMIT"
fi
if ! git -C "$MLIB_STATE/tdlib-src" cat-file -e "$TD_COMMIT^{commit}" 2>/dev/null; then
  git -C "$MLIB_STATE/tdlib-src" fetch --depth 1 origin "$TD_COMMIT"
fi
git -C "$MLIB_STATE/tdlib-src" checkout "$TD_COMMIT"
if [ ! -d "$MLIB_STATE/native/openssl/.git" ]; then
  git clone --depth 1 --branch openssl-3.5.6 https://github.com/openssl/openssl.git "$MLIB_STATE/native/openssl"
fi
export ANDROID_NDK_ROOT="$NDK"
export PATH="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH"
TD="$MLIB_STATE/tdlib-src/example/android"
cmake -S "$TD" -B "$MLIB_STATE/native/generate" -GNinja -DTD_ANDROID_JSON_JAVA=ON -DTD_GENERATE_SOURCE_FILES=ON
cmake --build "$MLIB_STATE/native/generate" -j "$JOBS"
for ABI in ${MLIB_ABIS:-arm64-v8a x86_64}; do
  case "$ABI" in
    arm64-v8a) SSL_TARGET=android-arm64 ;;
    x86_64) SSL_TARGET=android-x86_64 ;;
    *) exit 2 ;;
  esac
  PREFIX="$MLIB_STATE/native/openssl-$ABI"
  if [ ! -f "$PREFIX/lib/libcrypto.a" ]; then
    mkdir -p "$MLIB_STATE/native/ssl-build-$ABI"
    (cd "$MLIB_STATE/native/ssl-build-$ABI"
      "$MLIB_STATE/native/openssl/Configure" "$SSL_TARGET" no-shared no-tests -D__ANDROID_API__=26 --prefix="$PREFIX" --libdir=lib
      make -j"$JOBS"
      make install_sw)
  fi
  cmake -S "$TD" -B "$MLIB_STATE/native/td-$ABI" -GNinja \
    -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
    -DOPENSSL_ROOT_DIR="$PREFIX" -DCMAKE_BUILD_TYPE=RelWithDebInfo \
    -DANDROID_ABI="$ABI" -DANDROID_STL=c++_static -DANDROID_PLATFORM=android-26 \
    -DCMAKE_SHARED_LINKER_FLAGS="-Wl,--thinlto-jobs=$JOBS -Wl,--threads=$JOBS" \
    -DTD_ANDROID_JSON_JAVA=ON
  cmake --build "$MLIB_STATE/native/td-$ABI" --target tdjni -j "$JOBS"
  mkdir -p "$MLIB_STATE/jniLibs/$ABI"
  cp "$MLIB_STATE/native/td-$ABI/libtdjsonjava.so" "$MLIB_STATE/jniLibs/$ABI/"
done
printf 'TDLib %s built. Libraries: %s/jniLibs\n' "$TD_COMMIT" "$MLIB_STATE"
