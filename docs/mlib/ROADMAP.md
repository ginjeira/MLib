# MLib — implementation roadmap

**Status:** planned sequence. A phase is complete only when its acceptance criteria have evidence.

## Working rules

- Preserve inherited Spotygram behaviour; avoid broad refactors during initial integration.
- Work in small, reviewable commits.
- Record baseline and regression evidence before changing shared behaviour.
- Keep the MCat production system unchanged.
- Do not describe a build, test or platform as validated unless actually executed.
- Update product, architecture and decision documents when scope or technical decisions change.

## Phase 0 — repository and baseline audit

**Goal:** understand the fork and establish a reproducible baseline.

Tasks:
- Inventory repository state, branches, upstream relationship and current commit.
- Inspect Android/desktop modules, build instructions, dependencies and CI.
- Identify app ID, namespace, signing, update mechanism, release workflow and user-data locations.
- Record existing feature surface and critical flows.
- Build inherited Android debug/release variants in a controlled environment.
- Run available lint and verification checks.
- Install and exercise baseline on a device/emulator where available.
- Identify upstream artwork, notices, release assets and external services.

Acceptance:
- Baseline report records commit SHA, toolchain and exact commands.
- Build/test outcomes are PASS, FAIL or NOT RUN with evidence.
- No MLib feature changes are mixed into the baseline.
- Update compatibility and user-data risks are documented.

## Phase 1 — MLib identity and preservation

**Goal:** establish product identity without breaking inherited behaviour.

Tasks:
- Decide whether MLib installs alongside Spotygram or replaces/migrates it.
- Decide application ID, signing and update-channel strategy before publishing an APK.
- Create and use original MLib logo/launcher artwork; keep inherited Takopi artwork and its notice out of MLib-branded surfaces unless rights are independently established.
- Update product-facing text, authentication branding, launcher, notification and Desktop window/package icons deliberately.
- Keep existing Spotygram screens, library and playback behaviour intact.

Acceptance:
- MLib identity is consistent across Android launcher/adaptive/monochrome/notification icons, authentication screen and Desktop window/package icons.
- Existing critical library, Telegram, playback, playlist and update flows pass regression checks.
- Upgrade/data-retention behaviour is tested for the chosen package identity.
- No unlicensed upstream artwork is shipped.

## Phase 2 — audit and native TDLib acquisition design

**Goal:** port the MCat / MCat Player acquisition behaviour into MLib without runtime dependency on either project.

Tasks:
- Audit the MCat bot protocol and the MCat Player user workflow/source code.
- Classify behaviours as port, adapt, redesign or defer.
- Map GramJS operations to TDLib equivalents: sendMessage, getChatHistory, getMessage, getCallbackQueryAnswer and forwardMessages.
- Define message cursoring, callback handling, audio detection, completion detection and bot conversation serialization.
- Define destination indexing, recording identity and conservative deduplication.
- Keep MCat production, its HTTP API, credentials and sessions outside MLib.
- Document all bot assumptions and live validation gates.

Acceptance:
- The acquisition protocol document identifies source files and TDLib equivalents.
- No MCat HTTP/API, Telethon or GramJS runtime dependency is introduced.
- Bot operations are serialized and results are correlated to the selected message cursor.
- No production MCat changes are required.

## Phase 3 — native track acquisition

**Status (14 Nov 2026):** first implementation is present in `MusicAcquisition.kt` and `AcquisitionSheet.kt`; automated rule tests have been added. CI and live Telegram validation are pending.

**Goal:** search and import individual tracks through MLib's own TDLib session.

Tasks:
- Implement bot resolution, search text, inline filter callbacks and numbered result parsing.
- Add search and selection UI.
- Capture the bot message cursor before selecting an option.
- Collect audio messages and forward them to the selected Telegram destination.
- Parse forwarded messages with the existing MLib parser and upsert into SQLite.
- Index the destination before duplicate checks where required.
- Add tests for parsing, callbacks, cursoring, duplicates and partial failures.

Acceptance:
- User can search and select a track without MCat service credentials or server.
- The forwarded message appears in the MLib library and plays through existing playback.
- Repeating the same acquisition reuses an existing track instead of creating a duplicate.
- Existing Telegram sources, local imports and playback still work.

## Phase 4 — native playlist, album and artist acquisition

**Status (14 Nov 2026):** initial playlist acquisition and create/update flow are present in the first implementation. A distinct album model, artist semantics, configurable playlist creation and persistent job recovery remain unimplemented. Playlist behaviour is not yet validated against the live bot.

**Goal:** support collection workflows while preserving collection semantics.

Tasks:
- Complete playlist acquisition, preserving message order and partial outcomes.
- Make automatic playlist creation configurable.
- Add a distinct album model/grouping layer; do not represent albums as playlists.
- Add album matching and missing-track completion using local metadata and MCat bot results.
- Validate artist search and define whether it means artist metadata, a set of albums or a set of tracks.
- Persist acquisition jobs and recover interrupted work safely.

Acceptance:
- Playlist acquisitions create/update local playlists without duplicate memberships.
- Album acquisitions populate a distinct album representation with edition-aware matching.
- Partial failures preserve successful tracks and report missing items.
- Retrying a job is idempotent.
- Interrupted work is accurately represented; no false completed state is shown.

## Phase 5 — library intelligence and album completion

**Goal:** discover incomplete albums in the user's library.

Tasks:
- Normalise artist, album, title, disc and track-number metadata.
- Identify candidate album groups and confidence.
- Distinguish ambiguous editions/releases.
- Present album contents and likely missing tracks.
- Add action to search album through MCat.
- Compare MCat results with existing tracks and propose missing items.
- Import selected tracks and update album/playlist view.

Acceptance:
- Identification runs against a representative library without changing files.
- Ambiguous groupings are not silently merged.
- User can review missing tracks before acquisition.
- Existing tracks/files remain untouched.
- Completing an album updates the library without duplicate memberships.

## Phase 6 — Android stabilisation

**Goal:** make Android dependable before expanding platforms.

Tasks:
- Exercise phone-number login in local and international formats, verification-code submission, optional two-step password, invalid/expired code and password errors, force-stop/restart session recovery, large libraries and long-running acquisition jobs.
- Test offline/online transitions, process death, restart and account changes.
- Test concurrent/repeated requests within bot limits.
- Validate media formats, file integrity, playback, queue and storage cleanup.
- Review privacy, permissions, credentials and logs.
- Complete documentation and release/update strategy.

Acceptance:
- Critical Android flows have repeatable regression evidence.
- No known data-loss path remains in acquisition/import recovery.
- Performance and memory are measured on representative devices/libraries.
- Release signing, installation and update behaviour are verified.
- Remaining limitations are explicit.

## Phase 7 — Web feasibility and implementation

**Goal:** expose the largest safe and useful subset through a browser.

Tasks:
- Define Web user journeys and required parity.
- Design authentication and per-user authorisation.
- Decide how browser requests reach MCat without exposing bot credentials.
- Design media delivery, playback, job status and storage.
- Build a separate Web client/service boundary if justified.
- Test supported browsers and media formats.

Acceptance:
- Web exposes no Telegram sessions or MCat secrets to the browser.
- Android remains fully usable without the Web service.
- Supported library, playlist and playback flows have documented browser limits.
- Acquisition requests are authorised, isolated and observable.

## Phase 8 — Android Auto and TV evaluation

**Goal:** assess dedicated experiences after Android stabilisation.

Android Auto:
- MediaLibraryService, MediaSession and Media3 integration.
- Car-appropriate browse/search structure.
- Playback controls and resume.
- Physical head-unit validation.

Android TV / Google TV:
- Dedicated remote-first layout.
- D-pad navigation and visible focus.
- Readability at television distance.
- Playback/library browsing with a physical remote.

Acceptance:
- Separate technical assessment and scope proposal for each target.
- No platform is declared supported based only on compilation or emulator screenshots.

## Initial execution order

1. Finish Phase 0 audit and baseline.
2. Complete original MLib identity and branding resources (implementation started 13 Nov 2026).
3. Validate Android installation, coexistence, phone/code/2FA sign-in and session persistence on a physical device.
4. Validate CloudSync isolation and persistence.
5. Validate the native TDLib bot protocol live using the MLib account; no MCat HTTP/API, server or session is involved.
6. Complete track and playlist acquisition end-to-end, then implement a distinct album model, album completion and validated artist workflows.
7. Add library album identification and completion.
8. Stabilise Android; only then start Web and evaluate Auto/TV.

Update this document with dated evidence. Planned, in-progress and validated work must remain distinct. Code written does not by itself make a checklist item complete.


## Native acquisition implementation update — 14 Nov 2026

The MLib branch now contains a native acquisition implementation based on the MCat bot workflow and the MCat Player product flow. MLib uses its own existing TDLib session and communicates directly with the Telegram bot; neither MCat's service/API nor MCat Player's GramJS session is a runtime dependency.

Implemented in source:
- Search by term, content type (track/album/playlist/artist) and provider (Deezer/SoundCloud).
- Inline callback selection and numbered result parsing.
- Multiple result selection and Telegram destination selection, preferring GinjMusic without silently selecting an unrelated chat.
- Sequential acquisition, message cursoring, audio collection, completion/silence handling and TDLib forwarding.
- Reuse of the existing MLib parser and SQLite library; conservative duplicate matching and partial-result reporting.
- Initial playlist creation/update for playlist searches.
- Unit tests for result parsing, query sanitisation and duplicate matching.

Still NOT VALIDATED or incomplete:
- CI for the latest acquisition commits (in progress at the time of this update).
- Real Telegram search, callback execution, track/playlist acquisition, forwarding, duplicate retry and playback.
- Long playlist completeness, FloodWait/reconnect behaviour and process-death recovery.
- Persistent acquisition jobs and safe resume.
- Distinct album entity/model and album matching/completion.
- Semantics of artist acquisition.
- User setting for automatic playlist creation.

Do not treat source implementation or a successful compile as proof of live acquisition. The next acceptance gate is a real Android test with the MLib Telegram account, starting with one track, then a small playlist, followed by repeat-acquisition deduplication and playback.
