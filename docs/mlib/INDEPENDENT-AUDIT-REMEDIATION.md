# Independent audit remediation — 20 Nov 2026

This record maps the independent audit findings to the changes made in the source tree. It is implementation evidence, not device acceptance.

## Release model

Automatic publication from `main` is intentional: the owner is the only user and uses each patch as an evaluation build. The automatic workflow was not removed. Its documentation now states explicitly that these are evaluation releases, not a stable production channel, and that releases are produced after a successful baseline.

## Fixes

| Finding | Status | Change |
| --- | --- | --- |
| A2 — destructive fuzzy album merge | Fixed | Album cleanup now uses the same exact normalized title/artist/year rule as album creation. Track-overlap and fuzzy title heuristics were removed. `autoDetectAlbums()` no longer calls cleanup automatically. The UI asks for confirmation and explains that the operation is synced and cannot be undone in the app. |
| A3 — exported MediaLibraryService accepted every controller | Fixed | `PlaybackService.onConnect` now accepts only trusted controllers, the MLib package and an explicit Android Auto / Automotive / system allowlist. Other applications receive `ConnectionResult.reject()`. |
| M1 — hardcoded Portuguese UI | Fixed | User-facing literals in acquisition, albums, playlists, settings, downloads, playback folders and errors were moved to Android/desktop string resources. The English and Portuguese bundles remain key- and placeholder-compatible. |
| M2 — legacy Spotygram desktop trust chain | Fixed by fail-closed removal | The inherited `desktop/src/main/resources/update-key.pub` was deleted. `SignedBundle` now refuses to verify any bundle until an MLib key is provisioned. Packaging remains disabled. Documentation states the migration requirement. |
| M3 — release signer not pinned | Fixed | `android-release.yml` pins the MLib signer certificate SHA-256 and fails unless every APK matches it. |
| M4 — Android never compiled before merge | Fixed | `mlib-baseline.yml` now has an Android debug Kotlin/Compose compile job using the pinned SDK components. Full native APK packaging remains in the release workflow. |
| M5 — mutable Action tags / OpenSSL tag | Fixed | Workflows use commit SHAs for Actions. `tools/build-native.sh` pins OpenSSL by commit instead of a movable tag. |
| M6 — unbounded/large sync payload and unrecoverable storage | Mitigated | Snapshot limits increased to 32 MiB compressed / 128 MiB expanded with explicit bounds. Settings gains a confirmed repair action that can forget the lost channel and, optionally, the device key. |
| M7 — documentation contradicted code | Fixed | README, build, architecture, identity and persistence documents now describe format 5, AES-256-GCM, the automatic evaluation releases, debug signing and the disabled desktop updater consistently. |
| M8 — no tests for update/archive hardening | Fixed | Added `SignedBundleTest` for ZIP path traversal and for fail-closed behavior when the update key is absent. Added an album-deduplication regression test for similar titles. |
| M9 — unbounded audio import | Fixed | Audio import is capped at 1 GiB and reports a localized error. |
| B1 — catalogue encryption wording | Fixed | Documentation now states that `library.db` is sandbox-protected but not separately encrypted; only TDLib's database and remote CloudSync payloads have their own encryption. |
| B2 — production API credential override | Fixed | `api_id`/`api_hash` preference overrides are debug-only; release builds always use the compiled credentials. |
| B3 — stale notices and missing attribution | Fixed | The APK notice now identifies MLib and the upstream Spotygram attribution, includes libphonenumber, ZXing and Fluent Emoji, and no longer ships the historical Takopi artwork notice. |
| B4 — absolute runner paths in checksums | Fixed | `SHA256SUMS.txt` is generated from filenames inside the output directory. |
| B5 — no private security channel | Partly fixed | Added `SECURITY.md` with private GitHub reporting guidance. Closing stale PRs and enabling the repository issue tracker are remote repository settings and remain an owner action. |
| B6 — undocumented 32-bit limitation | Fixed | README states that only arm64-v8a and x86_64 are built. |
| B7 — privacy / third-party bot transparency | Documented | README now states the third-party bot relationship, cover-search requests to iTunes/Deezer, CloudSync metadata visibility and the absence of analytics/advertising SDKs. |

## Validation status

- Local validation passed: `:desktop:test` ran 59 tests with 0 failures; `:app:compileDebugKotlin` and `:app:assembleDebug` succeeded with Android SDK 37/build-tools 36. The generated debug APK is signed by the Android Debug certificate, confirming the release key is not used for debug variants.
- The desktop image build and the signed release build still need to run in the repository CI. The signer fingerprint pinned in CI is the existing MLib release certificate: `6e8fc1d09b55e70c6183b8cbbebad96d935f01a24558af356b697a942541e200`.
- No physical-device, live Telegram acquisition, two-device CloudSync, installation or update test was performed in this remediation pass.
- Desktop packaging remains intentionally disabled and fails closed.
- Post-audit branch `fix/playlist-repair` adds stale-message revalidation, playlist/album repair via an available copy, arbitrary destination selection, and a destination confirmation before acquisition; it was merged into main and is included in v0.1.11.
- `v0.1.18` was published on 21-11-2026 at 19:38:00. Both APK hashes match GitHub digests and the published SHA256SUMS; both are signed by the pinned certificate. It includes groups-only destinations, Android Auto Offline/Recent, duplicate review/merge, persistent jobs, album-completion confirmation and playlist cover search. Device testing remains pending.
