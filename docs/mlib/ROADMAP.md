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
- Review/replace inherited visual assets only where needed and rights are clear.
- Update product-facing text deliberately.
- Keep existing Spotygram screens, library and playback behaviour intact.

Acceptance:
- MLib identity is consistent in intentionally changed surfaces.
- Existing critical library, Telegram, playback, playlist and update flows pass regression checks.
- Upgrade/data-retention behaviour is tested for the chosen package identity.
- No unlicensed upstream artwork is shipped.

## Phase 2 — MCat protocol discovery and adapter design

**Goal:** understand the existing MCat workflow and reconcile it with the feature/lessons inventory from MCat Player before writing integration code.

Tasks:
- Inspect MCat bot implementation and current production interaction. (Initial static inspection completed against ginjeira/MCat.)
- Review MCat Player feature inventory and classify each item as reuse, reimplement, redesign or exclude.
- Validate the deployed revision and document commands, message formats, response correlation, media delivery, concurrency, limits, errors and retry behaviour.
- Verify the existing FastAPI endpoints and whether they can serve as a stable native-client integration surface.
- Prove how MLib can correlate an MCat job with the messages forwarded to the configured destination.
- Map track, album and playlist workflows.
- Identify stable IDs and metadata for deduplication.
- Define adapter contract and acquisition state machine.
- Define credential handling and operational boundaries.

Acceptance:
- Protocol document is based on source and/or observed exchanges, not assumptions.
- A request is correlated to its own result without mixing concurrent operations.
- Failure, timeout, partial-result and retry semantics are documented.
- No production MCat changes are required unless separately approved.

## Phase 3 — MCat track acquisition

**Goal:** obtain individual tracks into the existing library.

Tasks:
- Implement adapter behind a service boundary.
- Add track search and selection.
- Implement progress and error reporting.
- Import acquired media into existing library/download storage.
- Make imports idempotent and recoverable.
- Add focused tests for correlation, duplicates and failures.

Acceptance:
- User can search, select and import a track through MCat.
- Result appears and plays through existing library/player flows.
- Retrying the same result does not create duplicate library entries.
- Interrupted/failed work is accurately reported and safely retryable where supported.
- Existing Telegram-source and local-file workflows still work.

## Phase 4 — MCat album and playlist acquisition

**Goal:** acquire multi-track collections.

Tasks:
- Implement album and playlist search/acquisition.
- Preserve source ordering and metadata where available.
- Support partial success and per-track failure.
- Add optional automatic local playlist creation after playlist acquisition.
- Prevent duplicate playlist entries on retry.

Acceptance:
- Album and playlist requests complete with full or partial outcomes.
- Source ordering is retained when reliably supplied.
- Automatic playlist creation can be enabled/disabled in Settings.
- Failed tracks do not erase successful tracks or the playlist.
- Reprocessing the same results is idempotent.

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
- Exercise large libraries and long-running acquisition jobs.
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
2. Complete MCat live protocol validation and prove destination-message correlation.
3. Decide identity/package migration before creating distributable MLib builds.
4. Implement track acquisition end-to-end.
5. Add albums/playlists and automatic playlist creation.
6. Add library album identification and completion.
7. Stabilise Android.
8. Only then start Web and evaluate Auto/TV.

Update this document with dated evidence. Planned, in-progress and validated work must remain distinct. Code written does not by itself make a checklist item complete.
