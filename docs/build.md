# Building MLib

[Documentation](README.md) · **English** · [Русский](build.ru.md)

MLib is built as an independent application. It does not update or share private data with the third-party Spotygram application.

## Build state

Keep generated data outside the checkout. The only supported state variable is `MLIB_STATE`; it holds native sources, build outputs, caches, credentials and private signing material.

Default: `~/.local/state/mlib`. The Android build wrapper also honors `XDG_STATE_HOME`. Do not set this to Spotygram's state directory and do not use `SPOTYGRAM_STATE`.

Both Gradle modules use the same MLib state directory. Build scripts reject state inside the checkout.

## Android build machine

Use Linux, JDK 21, Android SDK platform 37, build-tools 36.0.0, NDK `28.2.13676358`, CMake, Ninja, gperf, Perl, a C++ compiler, Git and Make. The Gradle wrapper pins Gradle 9.3.1 with Android Gradle plugin 9.1.1 and Kotlin 2.4.20. MLib targets Android 36 and supports Android 8+.

Set the SDK and private state:

```sh
export ANDROID_HOME=/path/to/android-sdk
export MLIB_STATE="$HOME/.local/state/mlib"
```

Create `$MLIB_STATE/telegram.env` with your own application credentials from my.telegram.org:

```dotenv
TELEGRAM_API_ID=YOUR_NUMERIC_API_ID
TELEGRAM_API_HASH=YOUR_API_HASH
```

These identify the application, not a Telegram user account or bot. They are embedded in the APK and must not be treated as unrecoverable secrets after distribution. Never put a user session or bot token here.

## Development APK

Run from the repository root:

```sh
bash tools/build.sh debug
```

The first native build downloads official TDLib at `d1085f9cebc5a62379991ae1652673954f229c1f` and OpenSSL `openssl-3.5.6`, then builds ARM64 and x86-64 JNI libraries. Later builds reuse them.

Debug APKs are written to `$MLIB_STATE/build/app/outputs/apk/debug/`. The installed application ID is `pt.ginjeira.mlib.dev`; the release ID is `pt.ginjeira.mlib`. Both are independent from Spotygram's `app.spotygram` ID.

## Android release signing

MLib must have its own release key. Never use or request Spotygram's private keystore.

Create a new keystore once and back it up securely:

```sh
keytool -genkeypair -keystore "$MLIB_STATE/mlib-release.jks" \
  -alias mlib -keyalg RSA -keysize 3072 -validity 10000
```

Create private `$MLIB_STATE/signing.properties`:

```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=mlib
keyPassword=YOUR_KEY_PASSWORD
```

Restrict both files to your user and keep them outside source control. Every future MLib Android release must use this same MLib key. Then:

```sh
bash tools/build.sh release
```

The ARM64 release APK is written to `$MLIB_STATE/build/app/outputs/apk/release/`. Verify it with Android SDK `apksigner verify --verbose`, calculate SHA-256 and publish only through the MLib release workflow. The workflow is prepared in `.github/workflows/android-release.yml`; it is manually dispatched with an existing `vMAJOR.MINOR.PATCH` tag. It requires repository Actions secrets `TELEGRAM_API_ID`, `TELEGRAM_API_HASH`, `MLIB_ANDROID_KEYSTORE_BASE64`, `MLIB_ANDROID_KEYSTORE_PASSWORD`, `MLIB_ANDROID_KEY_ALIAS` and `MLIB_ANDROID_KEY_PASSWORD`. Store the keystore as base64 (`base64 -w0 mlib-release.jks` on Linux) and never commit the keystore or signing configuration. The workflow verifies package ID, version, ABI and APK signer, then uploads an artifact; setting `publish_draft=true` additionally creates a GitHub draft release. This workflow cannot be dispatched until its file is present on the repository's default branch.

## Desktop development image

Build on the target operating system with JDK 21, Python 3.12+ and the OpenSSL 3 CLI (on macOS, `brew install openssl@3`). Windows and Linux use x86-64; macOS has x86-64 and ARM64 builds.

```sh
python3 tools/build-desktop.py --image-only
```

On Windows use `python` instead of `python3`. The MLib application image is written below `$MLIB_STATE/build/desktop/distributions/main/app/`. It uses MLib's own runtime data paths and keyring identity.

TDLib comes from `prebuilt-tdlib` 0.1008067.0. The LGPL libmpv 0.41.0 runtime comes from the pinned `jason-yau/mpv-prebuilt` release. Downloads, unpacking and native binaries stay under `MLIB_STATE`.

## Desktop release status

**Native desktop installer and signed release packaging are intentionally disabled.** The inherited package scripts, installed-root detection, signed bundle manifest and updater still contain Spotygram-specific assumptions. Before publishing a desktop release, migrate those identities as one change, generate a new MLib Ed25519 key pair, replace the trusted public key in the repository, and test portable and installed updates. The old Spotygram private key is unavailable and must not be used.

Do not run the old Spotygram release workflow or upload its artifacts as MLib. The desktop image-only build is for development and validation, not a production installer.
