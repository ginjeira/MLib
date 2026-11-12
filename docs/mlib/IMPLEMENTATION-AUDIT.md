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


## Upgrade work started on `upgrade/mlib-phase-0`

The first implementation slice introduces the MLib name in the Android launcher label, shared user-facing wording and desktop runtime/window/tray labels.

The following identities were deliberately left unchanged in this slice: Android namespace/application ID and debug suffix, signing alias and external keystore paths, database/keyring identifiers, desktop bundle/installer identities, per-user data directories, SQLite schema and TDLib session storage. The existing Takopi artwork and its separate notice were also left untouched pending a properly licensed MLib identity asset.

**Not yet migrated:** release endpoint, release asset names, desktop installer branding, root Gradle project name, internal Kotlin package names, build-state directory and all remaining repository/docs branding. In particular, updater-related strings still mention Spotygram because the update channel itself has not yet been migrated; this is an intentional temporary gate, not the final product state.

**Validation:** GitHub branch comparison confirms only the audit document and the three intended branding files differ from `main`. No Gradle build, APK build, desktop package build, device test or existing-install update test has been run. The branch is therefore a draft implementation, not a validated release.

## Deeper source inventory

### Gradle and platform composition
- Root Gradle project name is spotygram; version is 0.28.0.
- Two Gradle modules exist: :app (Android) and :desktop (Compose Desktop). Shared Kotlin source is compiled into both; there is no independent :shared Gradle module.
- Android compiles shared source at JVM 17, min SDK 26, target SDK 36, compile SDK 37, with arm64-v8a and x86_64 APK outputs.
- Desktop compiles shared source at JVM 21 and packages native dependencies per OS/architecture.
- Android debug uses app.spotygram.dev; release uses app.spotygram. Telegram API credentials are build-time application credentials, not the user's Telegram account session.
- Build state defaults to ~/.local/state/spotygram via SPOTYGRAM_STATE. Caches, generated output, native TDLib libraries, credentials and signing material live outside the checkout.

### Database and account state
- Both platforms use the same logical library.db SQLite schema, currently version 8, through platform-specific adapters.
- Tables include tracks, sources, playlists, playlist_tracks, downloads, temporary_audio, playback_progress, profiles, sync_meta and sync_values.
- Migrations are incremental and version-gated in Library.onUpgrade.
- Android database encryption uses Android Keystore plus an encrypted preference value; desktop uses the OS credential store keyed to the canonical data directory.
- TDLib data is below the platform private files directory at telegram/db and telegram/files. The database key is retrieved before those directories are created, distinguishing a new profile from an existing profile with a missing key.
- On open, stale TDLib numeric file bindings are cleared because they belong to a client lifetime. This does not delete catalogue records or media copies.

### Telegram and library ownership
- Telegram owns one TDLib client per application instance, authorization, connection state, chat/file caches, request correlation, message updates and confirmed message sends.
- MLib must reuse this session and must not create a second login/session for MCat.
- SpotygramApp coordinates library, Telegram, sync, profiles, downloads, cache, sharing and updates.
- Library owns catalogue persistence and immutable snapshots. Selected chats are sources; profile music has separate profile-owned identities.
- Existing chat discovery supports loading main/archive lists, local and server search, resolving public chats by username and adding chats as sources.
- Existing sharing code inspects group/channel type, membership, posting rights, audio/document permissions, send-as identities, forum topics and protected-content restrictions. This is groundwork for onboarding checks, not an MCat setup wizard.
- No existing code path was identified that creates a Telegram group, joins a bot to a group, or configures an MCat backend destination. Do not assume these operations exist.

### Telegram library sync is not the MCat music destination
- CloudSync uses the authenticated TDLib client and discovers/maintains private, muted storage channels in the user's Telegram archive.
- Current sync identifiers are spotygram-sync/3 and spotygram-storage/1; channels are discovered using protocol markers and titles beginning spotygram-.
- LibrarySync format is 3. It validates portable records for tracks, favorites, sources, profiles, playlists and selected settings. Playback queue and progress are local-only.
- These identifiers, channel titles, protocol markers and persisted format are compatibility data. Renaming them or changing discovery without dual-read/dual-write migration risks making libraries appear empty or creating duplicate storage channels.
- The MCat destination group is a separate operational resource and needs its own configuration and identity.

### Playback, imports, downloads and duplicates
- Android playback uses Media3 through PlaybackService and PlayerConnection; the shared queue handles selection, ordering, repeat, speed and progress.
- Desktop playback uses libmpv through a native adapter, with desktop media controls and single-instance/window lifecycle.
- Downloads are persisted in the library queue and executed by a platform-owned service/lifecycle. Temporary playback cache is separate from explicitly saved or imported copies.
- Local imports are copied into app-owned storage; catalogue records file keys, remote IDs, size and path.
- Duplicate hiding is recording-level grouping and non-destructive. Telegram message/file identity and recording identity must remain distinct when adding MCat acquisition.
- Explicit sharing/forwarding to Telegram recipients confirms send results, but this is not the same as importing an MCat job result.

### CI, release and update observations
- The only workflow in .github/workflows is a manually dispatched desktop release-assets workflow. No Android build/test workflow was found in the current tree.
- Desktop workflow requires Telegram API credentials and the desktop Ed25519 release private key as repository secrets.
- Android release signing uses an external JKS and properties file in build state; neither is available for inspection from repository source.
- Android update checking is hard-coded to Krablante/spotygram and uses Spotygram release tags and ABI-specific APK asset names. Desktop updates also use Spotygram assets and a trusted Ed25519 public key.
- Desktop package metadata, installer scripts, artifact names and notices remain Spotygram-specific.
- docs/verification.md contains historical evidence for Spotygram 0.28.0, including Android 16 update retention from 0.17.0 to 0.28.0, certificate continuity, desktop package verification and UI checks. This is not validation of the current MLib branch.

### Brand migration inventory
Brand references fall into distinct classes: user-visible name/descriptions; OS-visible labels/notifications/installer metadata; resource names/artwork; internal package/application/keyring/database identifiers; cloud-sync protocol markers; update endpoints/assets/trusted keys; build-state environment variables and filesystem paths.
Only user-visible wording and selected runtime labels have changed in the initial MLib slice. Compatibility/security-sensitive identifiers must not be globally replaced. Packaging changes require a coordinated release migration. Artwork requires a rights review. Build-state paths need compatibility fallbacks.

### Conclusions and validation gates
- The architecture supports in-place MLib evolution; a rewrite or parallel Android package is not required for the successor strategy.
- Static inspection confirms persistence/platform boundaries, but cannot prove that actual signing keys are available or that an upgrade APK works.
- Android debug/release and desktop image builds still need to run in an environment with required SDK/NDK/native dependencies and external state.
- First upgrade validation must use a copy of an existing Spotygram installation/profile and prove same-signer update, retained TDLib login, database readability, cloud-sync discovery and local media/playback continuity.
- Do not change package ID, signing identity, storage paths, SQLite version, cloud-sync markers or release endpoints before this proof.
## MLib update-channel transition (implemented in this branch; not yet release-tested)

- Android latest-release lookup and release-page links now target ginjeira/MLib rather than Krablante/spotygram.
- The shared UpdateAsset download URL now resolves against the MLib repository.
- Desktop update requests identify as MLib; Android update download notifications use MLib.
- English and Russian user-facing update labels have been aligned with the MLib channel.
- Transitional release asset filenames remain Spotygram-specific because the existing platform assetName methods, desktop packaging workflow, signed installer manifest text and installed desktop directory are still Spotygram-specific. This is deliberate until a coordinated packaging identity migration is tested.
- The MLib repository currently has no validated MLib release pipeline/assets. Until the first compatible release is published, manual update checks from an MLib build will not find a usable update. Automatic checks are non-blocking. Do not publish or distribute this branch as a production build.
- Existing cached update metadata is retained, but the download URL is derived from the new repository. An old cached Spotygram version is therefore not intentionally fetched from the upstream Spotygram repository.
- Desktop Ed25519 verification key and Android signing identity were not changed. Availability of their private keys has not been verified.
## Baseline CI added (not yet executed/observed)

- Added `.github/workflows/mlib-baseline.yml` on the implementation branch.
- Android job provisions JDK 21, Android SDK platform 37, build-tools 36.0.0, NDK 28.2.13676358, CMake and native build tools; it builds pinned TDLib and OpenSSL and assembles debug APKs for ARM64 and x86_64.
- Desktop job builds the Linux application image only, using pinned TDLib/libmpv prebuilts. It does not generate installers, sign artifacts, or publish a release.
- Both jobs use isolated temporary build state outside the checkout. Telegram API build credentials are set to non-working placeholders (0/empty) because compile validation does not require a real Telegram account or production API credentials. No user session or signing key is used.
- The workflow has push, pull_request and workflow_dispatch triggers. The GitHub Actions run and commit status endpoints available during this work returned no run/status for the latest workflow commits. Therefore the workflow is committed but neither successful execution nor build output has yet been verified.
- The baseline workflow is deliberately separate from the existing signed desktop release workflow and does not modify the release pipeline.
## Baseline CI execution update (12 Nov 2026)

- Run `43132643507` (`fix(ci): install Android 17 API 37.0 SDK package`) is executing against the current implementation branch.
- Desktop Linux application image job completed successfully: `:desktop:createDistributable` finished and the workflow's launcher/runtime-image assertions passed. This validates Linux image assembly only; it does not validate installer packaging, signatures, desktop runtime behaviour or other operating systems.
- Android job successfully provisioned JDK 21, Android SDK platform 37.0, build-tools 36.0.0, NDK 28.2.13676358 and native build tools. Native build is running; logs show OpenSSL Android ARM64 compilation with non-fatal `__ANDROID_API__` macro-redefinition warnings. TDLib compilation and APK assembly/output assertions have not yet been reported complete.
- Android result remains NOT VALIDATED until the job completes and both ABI APK output assertions pass.
- No artifacts are uploaded by this baseline workflow. It is not a release or signing workflow.

## Superseding decision: independent MLib installation (12 Nov 2026)

The initial in-place successor assumption recorded above is superseded. The user confirmed that Spotygram is a third-party project, that the two original private release keys are unavailable, and that MLib must coexist with Spotygram. MLib must not replace or update Spotygram.

The new authoritative decision is documented in [INDEPENDENT-IDENTITY.md](INDEPENDENT-IDENTITY.md). This branch implements the first isolation slice:

- Android release application ID is now `pt.ginjeira.mlib`; debug uses `pt.ginjeira.mlib.dev`. The inherited source namespace `app.spotygram` is retained temporarily to avoid a broad source/R-class refactor and is not the Android install identity.
- MLib version starts at 0.1.0 / versionCode 1. It is not an update of Spotygram.
- Android database keystore alias is MLib-specific.
- Build state is now `MLIB_STATE`, defaulting to `~/.local/state/mlib`; no fallback to `SPOTYGRAM_STATE` is allowed.
- Desktop runtime paths, cache paths, keyring account and runtime app ID are MLib-specific. The desktop image is named MLib.
- Desktop release packaging is explicitly disabled until the inherited installer/updater/manifest identities are migrated and a new MLib Ed25519 key pair is provisioned. No Spotygram private key is requested or reused.

No Spotygram private data is imported. Android package sandboxing and the new desktop paths/keyring account provide separate storage boundaries. MLib may authenticate to the same Telegram account using its own TDLib database and session.

### Validation status for this branch

The identity and build configuration changes are committed, but a new baseline build on this branch has not yet completed. The earlier CI run on `upgrade/mlib-phase-0` does not validate this branch. APK manifest inspection, side-by-side installation, Telegram login, database, playback, CloudSync and desktop coexistence remain NOT VALIDATED. Do not distribute this branch as a production build.
