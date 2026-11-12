# MLib implementation baseline audit

**Audit date:** 2026-11-12  
**Scope:** static inspection of the `main` branch of `ginjeira/MLib`. No build, device test, authenticated Telegram session or release installation was performed.

## Executive finding

The repository is already a substantial Spotygram fork, not an empty application shell. It contains a shared Kotlin/Compose application compiled by Android and desktop targets, a TDLib integration, a SQLite-backed music catalogue, Telegram library synchronisation, Media3 playback on Android, libmpv playback on desktop, downloads/imports, playlists, profile music, sharing and a signed update mechanism.

The MLib upgrade should therefore be an **in-place product evolution of this codebase**. Replacing core modules, resetting the database or changing installation identity as an early branding step would introduce unnecessary risk.

## Verified build and identity baseline

| Area | Current observed value | Upgrade constraint |
| --- | --- | --- |
| Gradle root project | `spotygram` | Internal name can be changed later, independently of user data. |
| Modules | `:app` and `:desktop`; shared Kotlin source is compiled into both | Keep the shared application and platform boundaries. |
| Android namespace | `app.spotygram` | Preserve while the upgrade is intended to update an existing installation. |
| Android application ID | `app.spotygram`; debug suffix `.dev` | Do not change without an explicit migration/install strategy. |
| Android signing | Release alias `spotygram`, external `release.jks` and `signing.properties` | Reuse the existing private signing key; never generate a replacement for an update build. |
| Android version | `versionCode = 50`; version name inherited from root version `0.28.0` | Keep versionCode monotonic for upgrades. |
| Desktop identity | `app.spotygram` bundle/keyring identity; Spotygram installer/package naming | Preserve OS upgrade identities and credential lookup until migration is designed. |
| Build state | `SPOTYGRAM_STATE`, default `~/.local/state/spotygram` | Preserve/locate existing native and signing state; do not silently move it. |
| Android user data | Private app storage | Same application ID is required to update in place and retain it. |
| Desktop user data | Spotygram per-user profile paths | Keep paths stable during the first product rename. |
| Database | SQLite schema 8, incremental migrations on both adapters | Never reset the catalogue; migrations must be additive and recoverable. |
| Update source | GitHub latest release for `Krablante/spotygram` | Must be moved to the MLib release channel before publishing MLib updates; do not leave a branded MLib build installing upstream Spotygram packages. |

## Existing functionality to preserve

The architecture guide and inspected application entry point establish these existing responsibilities:

- `SpotygramApp` coordinates application lifecycle, Telegram, library, sync, profiles, sharing, downloads, cache and update services.
- `Telegram` owns a single TDLib client, authorization and request correlation.
- `Library` owns SQLite and immutable catalogue snapshots.
- `CloudSync` stores portable library state in a private, muted Telegram channel in the user's archive. It is account-bound and has durable pending edits.
- `PlaybackQueue` owns ordering, repeat, speed and listening positions.
- Android uses Media3 and native Android services; desktop uses libmpv and desktop media controls.
- Imports create app-owned copies. The code documents staged cleanup and transaction rules.
- Duplicate hiding is a reversible view, not destructive deletion.
- Android update installation verifies asset size, SHA-256, package, version and signer before handing the APK to Android.
- Desktop update installation verifies package digest and an Ed25519 release signature before invoking the OS installer or replacing a portable application.

These are preservation requirements, not features to rewrite merely to change the product name.

## High-risk coupling discovered

1. Product name, application identity and storage identity are currently coupled in several places, but they are not interchangeable.
2. Android `applicationId`, namespace, FileProvider authority, keyring identities, desktop bundle ID, installer IDs, data paths and release signer affect upgrades and access to existing data.
3. The update checker is hard-coded to `Krablante/spotygram`; Android and desktop asset names are also Spotygram-specific. A release-channel migration must be delivered as a coherent change, not a string replacement.
4. The release-signing material is intentionally outside the repository. The repository audit cannot prove that the existing private keys are available to the build operator.
5. Existing launcher artwork includes Takopi artwork with a separate notice. It must not be assumed to be freely reusable under the repository MIT licence. New MLib artwork or an explicitly licensed replacement is required before removing that notice or distributing changed artwork.
6. The current MCat integration is not present in the inspected Spotygram application. It must be introduced behind a dedicated adapter and must not bypass the existing TDLib session or library ownership boundaries.

## Initial upgrade policy

- Treat MLib as the successor product to this Spotygram fork, not as a parallel app.
- Preserve Android package ID, release signing identity, desktop bundle/installer identities, user-data paths, database and Telegram sync format during the first upgrade stages.
- Keep internal Kotlin packages and storage identifiers stable initially; user-visible branding can be migrated separately.
- Do not change release endpoints or asset names until the MLib release workflow and signing continuity have been verified. The MLib build must never offer an upstream Spotygram binary as its own update.
- Do not modify the MCat production service as part of this work.
- No database reset, automatic deletion, destructive deduplication or forced reauthentication.
- Make changes on an isolated upgrade branch and keep the documentation-foundation PR separate until its review/merge is decided.

## Next implementation gates

1. Complete the source-level inventory of Android/desktop platform adapters, storage paths, SQLite migrations, TDLib session handling, release workflows, signing and all user-visible brand references.
2. Establish a baseline build and test/verification record where a suitable build environment is available.
3. Implement a first MLib identity slice: visible product name and resources, while preserving package/signing/data identities.
4. Migrate the update channel and release asset naming only together with a tested MLib release pipeline and verified key continuity.
5. Add MCat integration as a separate adapter and onboarding flow, reusing Spotygram's existing Telegram authorization and chat discovery.
6. Revalidate upgrades over an existing Spotygram installation and confirm the existing catalogue, session, playlists, sync channel and downloads remain intact.

## Evidence inspected

- `README.md`
- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `desktop/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/app/spotygram/AndroidApplication.kt`
- `app/src/main/java/app/spotygram/MainActivity.kt`
- `shared/src/main/kotlin/app/spotygram/SpotygramApp.kt`
- `shared/src/main/kotlin/app/spotygram/AppUpdates.kt`
- `shared/src/main/res/values/strings.xml`
- `tools/build.sh`
- `tools/build-desktop.py`
- `tools/package-desktop.py`
- `docs/architecture.md`
- `docs/build.md`
- `docs/usage.md`

The evidence above is static source/document inspection only. It is not a claim that a build, update, migration or runtime behaviour has been validated.
