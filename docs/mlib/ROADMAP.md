# MLib — implementation roadmap (histórico)

> **Plano de execução corrente:** [`roadmap.md`](../../roadmap.md), dividido em waves. Este documento mantém o histórico detalhado e a rastreabilidade das fases anteriores.

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

**Status (15 Nov 2026):** native TDLib search/acquisition is implemented with bot-lane serialization, message cursoring, paginated history (up to 2,000 messages per option), destination refresh before every acquisition, same-response/cross-option deduplication, conservative metadata-gated fuzzy title matching, and limited FloodWait retry. Rule tests cover parsing, sanitisation, exact/ambiguous/fuzzy/reversed-metadata matching and adjacent transposition. Baseline CI do commit `0fde3c2031acbc3d11d245e42be193ef6f22e3d7` **PASS** (Android ARM64/x86_64, testes Desktop/shared e imagem Desktop Linux; [run #43736017616](https://github.com/ginjeira/MLib/actions/runs/43736017616)). A validação Telegram real continua pendente.

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

**Status (15 Nov 2026):** playlist acquisition/create-update with preserved order, configurable automatic playlist creation, a distinct album model with SQLite migration, album CRUD/order/deduplication, CloudSync representation, album UI, local metadata-based detection, and iTunes/Deezer cover suggestions are implemented. Album matching now keeps different known release years separate; local detection groups by album/artist/year. Existing-album completion targets the selected album, reuses matching library tracks and appends missing tracks. Acquisition jobs are persisted in MLib preferences; resume rebuilds the bot search, refreshes the destination and retries selected results idempotently. Artist semantics are defined by the bot's result options, each of which may return one or more tracks; this is not full discography acquisition. CI for latest commits and live bot validation remain pending.

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

**Status (15 Nov 2026):** imported/local audio metadata can populate distinct album records; local-file album detection groups by normalised album/artist/release year, and duplicate merging will not merge different known years. Album editing supports metadata and cover suggestions from iTunes/Deezer. A separate per-track preview/proposal of missing tracks, confidence-scored grouping and explicit edition selection remain incomplete; the current completion action searches the bot for the album, reuses existing tracks and appends the rest.

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

**Status (15 Nov 2026):** the MLib Android Auto browse tree now exposes all tracks, favourites, playlists, albums and album tracks through the existing MediaLibraryService/PlaybackQueue. Album folders expose artist/year metadata and artwork. Physical head-unit validation remains outstanding.

**Goal:** complete and validate dedicated experiences after Android stabilisation.

Android Auto:
- MediaLibraryService, MediaSession and Media3 integration — implemented.
- Car-appropriate browse tree for tracks, favourites, playlists, albums and album tracks — implemented.
- Playback controls and resume through the existing PlaybackQueue — inherited/integrated; runtime acceptance pending.
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
6. Validate track, playlist and album acquisition end-to-end; exercise persisted job recovery, FloodWait and artist track semantics against the live bot.
7. Extend library album identification with confidence scoring, explicit edition review and a per-track missing-track proposal screen.
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


## Implementation update — 15 Nov 2026

The following source changes are now present on `feat/mlib-independent-identity`:

- Automatic playlist creation after playlist acquisition can be switched on/off in the acquisition screen; the preference is stored locally and defaults to enabled to preserve the previous behaviour.
- Deduplication now also compares pending tracks within the same bot response, so equivalent recordings with different Telegram file IDs are not both forwarded.
- Imported/reused totals are calculated from distinct track IDs across all selected results.
- Fuzzy title matching is deliberately conservative: it requires the same known artist, a known duration within two seconds, and title similarity of at least 0.92. Similar titles alone are never sufficient. Regression tests cover positive and negative cases.
- Album model, persistence, CloudSync, UI, local metadata detection and external cover suggestions were verified by source inspection; these are implementation facts, not live acceptance evidence.

Still outstanding:
- Persist acquisition jobs and recover interrupted work without falsely reporting completion or repeating already-forwarded audio.
- Define and implement artist acquisition semantics (artist metadata versus albums versus tracks).
- Complete edition-aware album matching and missing-track proposals/completion.
- Add robust handling for Telegram FloodWait, reconnects and long-running collection completeness.
- Run CI against the latest branch head; GitHub API commits made during this session have not yet produced a new workflow run.
- Validate real Telegram search/callbacks, track and playlist acquisition, forwarding, retry/deduplication and playback on the MLib account.
- Validate Android installation/update/coexistence, CloudSync isolation and Android Auto album browsing on devices.

Do not mark the port complete until the source changes pass current CI and the live Telegram acceptance flow has been exercised.


## Implementation update — 15 Nov 2026 (continuation)

The following acquisition/album gaps were addressed in source:

- Acquisition journal persisted in MLib preferences. After process restart, the UI offers **Retomar aquisição**; the bot search is rebuilt, the destination is refreshed, and selected results are retried. This is designed to recover a crash after forwarding by re-indexing the destination before deduplication.
- Destination history is refreshed before every acquisition, even when previously marked fully indexed, so newly forwarded tracks are visible to deduplication.
- Telegram history collection pages backwards in batches of 100 (bounded to 2,000 messages per selected result) rather than silently considering only the latest 100 messages.
- Telegram 429/FloodWait responses are retried with bounded waits; waits longer than 15 minutes or repeated failures leave the job pending for manual resume.
- Multi-track jobs that hit the 240-second deadline without completion/quiet confirmation are marked partial and remain resumable.
- Completing an existing album now carries its album ID through search and acquisition, adds the returned tracks to that album, and preserves its title/artist/year/cover metadata. Multiple bot editions cannot be merged into one target album in a single selection.
- Album matching and local-file grouping now include known release year so different known years remain separate. Bot album titles are parsed into artist and album title while preserving edition suffixes.
- Artist search is explicitly defined as a list of individual track options; selected tracks are imported individually. No false artist album or playlist is created.
- Regression tests were added for title transposition, artist/title parsing, collection edition suffixes and release-year separation.

Still outstanding before the port can be called complete:

- Run current CI on the latest branch head. The last observed CI run predates these fixes and failed two fuzzy-title tests; the algorithm and regression tests have since been corrected, but the new head has not yet been verified by CI.
- Live MLib-account acceptance: search, type/provider callbacks, track, playlist, album, artist, forwarding, repeat acquisition, FloodWait, restart/resume and playback.
- Validate that Telegram's actual history pagination and bot response format match the assumptions above, including collections exceeding 100 messages.
- Exercise Android process death/restart and confirm the persisted resume journal behaves as intended.
- Verify Android Auto album browsing and installed-app upgrade/data retention on devices.

Do not mark the port complete until CI and live Telegram acceptance have passed. A source-level implementation is not runtime acceptance.


## Post-port enhancements (not blockers for MCat behaviour parity)

- Add a per-track missing-track preview/approval screen before album completion. Current behaviour matches the MCat Player flow: retrieve the selected album, reuse recordings already in the library, and append/forward only missing recordings.
- Add confidence-scored local album grouping and a richer edition-selection UI.

## MCat Player portability — controlled wave plan (16 Nov 2026)

The detailed, binding gap register and acceptance criteria are maintained in `docs/mlib/PORTABILITY-GAPS-REPORT.md`. Wave 0 baseline: MLib `7231aa08022863a5a3f22138ff881fcf4045c340` on `feat/mlib-independent-identity`; MCat Player `a41cf5ceeea023d7b86484cf5c7bf5a7ae7b2132`; SQLite schema **11** (confirmed in `Library.kt`). The old schema-8 statement in persistence documentation is superseded by the 16-Nov addendum.

Do not interpret earlier roadmap statements that acquisition is “implemented” as proof of MCat parity. Record separately: code present, source parity (golden tests), automated test result, and live Telegram/device acceptance. ACQ-01/02 remain P0 and block Wave 2+ progression until Wave 1 is closed. The report's Wave 0–7 sequence is authoritative for the MCat Player port; this broader product roadmap remains historical context.


## Wave 1 — protocolo de pesquisa e recepção (em execução)

- ACQ-01: critérios MCat de contagem, sinais textuais, faixa única, silêncio de 9 segundos e deadlines de 45/240 segundos.
- ACQ-02: `artist` só é multi-faixa quando a contagem esperada é superior a uma; playlists/álbuns sem contagem continuam multi-faixa.
- Áudio recebido sem critério terminal até ao deadline é preservado e reportado como parcial; ausência de áudio é falha.
- Gate pendente: testes automatizados, CI e validação funcional live Telegram. A retoma idempotente de resultados parciais permanece na Wave 6.
