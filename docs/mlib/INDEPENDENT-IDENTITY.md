# MLib independent application identity

## Decision

MLib is a separate application that coexists with the third-party Spotygram installation. It is not an in-place successor update and must never read, migrate, overwrite or delete Spotygram's private application data.

The owner does not possess Spotygram's Android or desktop release-signing private keys. Those keys are neither required nor to be requested. MLib owns new application identifiers, storage locations and release-signing identities.

## Android identity (implemented in this branch)

- Release application ID: `pt.ginjeira.mlib`.
- Debug application ID: `pt.ginjeira.mlib.dev`.
- The inherited Kotlin/Android namespace remains `app.spotygram` temporarily. It is a source-code namespace, not the installed application identity; retaining it avoids a broad generated-`R` and manifest-class refactor in the same change.
- MLib starts with versionName `0.1.0`. Android `versionCode` is derived monotonically from `MAJOR.MINOR.PATCH` as `MAJOR * 1,000,000 + MINOR * 1,000 + PATCH` (minor and patch must each be below 1000). It has no update relationship with Spotygram.
- Android's per-package sandbox, SharedPreferences, databases, files, Android Keystore UID scope and FileProvider authority are now distinct from Spotygram.
- The database encryption alias is MLib-specific.
- Android release assets use `mlib-VERSION-arm64.apk` and `mlib-VERSION-x86_64.apk`; the updater and manual release workflow use the same naming contract.
- A new MLib release keystore is expected at `$MLIB_STATE/mlib-release.jks`, with properties in `$MLIB_STATE/signing.properties`; the alias defaults to `mlib`. No key is generated or checked into this branch. Debug builds use Android's debug signer and can be installed alongside Spotygram.

## Build and native state (implemented)

- The only supported build-state variable is `MLIB_STATE`.
- Default: `~/.local/state/mlib` (or `$XDG_STATE_HOME/mlib` for the Android build script).
- Gradle output, native TDLib/OpenSSL sources and libraries, credentials and signing material are stored there, outside the checkout.
- The former `SPOTYGRAM_STATE` variable is not read as a fallback. This is deliberate: an inherited environment variable must not silently point MLib at Spotygram's build state.

## Desktop runtime data (partially implemented)

- MLib's runtime data and cache paths are separate:
  - Windows: `%LOCALAPPDATA%/MLib`
  - macOS: `~/Library/Application Support/MLib` and `~/Library/Caches/MLib`
  - Linux: `$XDG_DATA_HOME/mlib` and `$XDG_CACHE_HOME/mlib` (with standard home-directory fallbacks)
- Desktop keyring account identity is `pt.ginjeira.mlib`; runtime app/AUMID identity is also MLib-specific.
- The desktop application image is now named MLib.
- The desktop image-only build now uses the MLib application image paths; the legacy Spotygram package generator and release workflow are disabled.
- **Desktop installer/release publication remains disabled.** The inherited packaging scripts, signed bundle manifest and updater still contain Spotygram assumptions. They must be migrated together, including a fresh MLib Ed25519 key pair and trusted public key, before a desktop installer can be distributed. The build script explicitly refuses to package/publish a desktop release until that work is complete.

## Visual identity (implemented 13 Nov 2026)

- MLib now has an original geometric audio-mark: a rounded waveform forming an M, in mint on a deep-navy field with a restrained coral pulse.
- The artwork is authored as MLib resources; Android uses vector foreground/background assets, a monochrome adaptive icon and a dedicated notification silhouette.
- The authentication screen and Desktop window use MLib artwork and wordmark.
- Desktop package icons are generated as MLib-named PNG, ICO and ICNS assets from the original MLib launcher image.
- The unused inherited Takopi/Spotygram launcher, mark and silhouette assets have been removed from active resources. The separate Takopi artwork notice remains in the repository for legal traceability.
- Desktop installers and signed release publication remain disabled as described above.

## Telegram sign-in (implementation; device validation pending)

- MLib reuses the inherited single TDLib client and authorization state machine; no second Telegram client or separate MCat login is introduced.
- The sign-in flow supports a phone number, the verification code delivered by the method selected by Telegram, and the Telegram two-step verification password when TDLib requests it. Existing email/QR fallback states remain supported.
- Phone numbers may be entered in the device's local region format or in international format. Local numbers are normalized to E.164 using the device locale country; international numbers retain their explicit country code.
- TDLib's database and file directories are rooted in MLib's own Android private files directory (telegram/db and telegram/files), while the package sandbox and MLib database-key alias remain independent of Spotygram.
- Build-time Telegram API ID/hash are application credentials, not the user's phone number, verification code or two-step password. The latter are supplied at sign-in and remain handled by TDLib.

## Compatibility boundaries

- Do not import, copy, open or mutate Spotygram's private database, TDLib files, keyring entries, preferences or media directories.
- MLib may use the same Telegram account, but it must establish its own TDLib session and private data directory.
- Keep the SQLite record/schema format compatible where practical, but use a distinct MLib CloudSync namespace: `mlib-sync/3`, `mlib-storage/1`, and `mlib-<id>` channel titles. MLib must never discover, parse, adopt, mute or write to Spotygram's `spotygram-sync/*` / `spotygram-storage/*` channel. It must not reuse that channel as an MCat destination.
- MCat production remains untouched.
- Release update checks target the MLib repository only. There must be no fallback to Spotygram releases.

## Validation still required

- Run the Android baseline workflow on this branch and confirm two non-empty ABI APKs.
- Inspect the APK manifest to verify release/debug application IDs.
- Install MLib debug alongside Spotygram and verify both launch independently.
- Verify separate app storage, Android Keystore key, TDLib login by local and international phone formats, verification code, optional two-step password, session persistence after force-stop/restart, SQLite catalogue, downloads, playback and CloudSync channel.
- Generate and securely retain the new Android release key before creating signed release APKs.
- Complete desktop installer identity, update manifest, trusted-key and package-name migration; then test MLib and Spotygram side by side on each supported OS.
- Do not claim runtime coexistence or release readiness until the relevant device/desktop tests pass.

## Explicit non-goals

- No Spotygram uninstallation, update, migration or backup deletion.
- No use of Spotygram's private signing keys.
- No changes to MCat production configuration.
- No destructive media/library operations.
