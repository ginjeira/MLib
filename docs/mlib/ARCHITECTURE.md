# MLib — architecture

**Status:** baseline observations plus proposed boundaries. This is not an implementation claim.

## 1. Starting point

MLib is a fork of Spotygram. The repository contains a Kotlin / Compose application with Android and desktop targets:

- **app:** Android application and Android-specific integration.
- **desktop:** desktop application and platform integration.
- **shared/src/main/kotlin:** shared application, screens, Telegram access, library, queue and playback rules.
- **shared/src/main/res:** shared resources.
- **SQLite:** local music catalogue and related state.
- **TDLib:** Telegram client integration.
- **Media3 / libmpv:** Android / desktop playback engines.

The checked-in Gradle settings still name the root project **spotygram** and include the desktop module and, unless desktop-only mode is selected, the Android app module. The Android module declares namespace and application ID **app.spotygram**. These are inherited identifiers, not the final MLib identity.

The upstream architecture describes a local-first application with no central backend, local HTTP proxy or WebView. Android directly uses TDLib. Library synchronisation uses the user's Telegram account. This is the baseline to preserve, not a reason to add a server.

## 2. Proposed logical architecture

The intended dependency direction is:

- Compose UI calls application use cases.
- Application use cases coordinate Library, Playback, Telegram Sources and MCat Acquisition.
- Library persists catalogue and relationships in the existing SQLite database.
- Telegram Sources continue to use the existing TDLib integration.
- Playback continues to use the platform engine (Media3 on Android).
- MCat Acquisition calls a dedicated MCat adapter.
- Adapter results are normalised, matched and imported into the existing library.

These are target boundaries, not claims that separate modules already exist. Exact boundaries must follow source inspection.

## 3. Architectural rules

1. Preserve the current Spotygram data model and flows unless a migration is justified.
2. MCat is a separate domain/service boundary. UI screens must not issue bot commands directly.
3. Keep bot transport and protocol details behind an adapter.
4. Convert bot responses into explicit acquisition results before library import.
5. Keep acquisition-job state separate from playlist, track and playback-queue state.
6. Make imports idempotent: processing the same result twice must not create duplicate library rows or playlist memberships.
7. Keep long-running work observable and recoverable. Persist only necessary state with clear ownership and migration rules.
8. Do not share a Telegram TDLib client/session with the MCat bot unless the existing design explicitly requires and safely supports it. Bot identity and a user's personal Telegram account are separate security boundaries.
9. Never put bot tokens, Telegram API credentials or full user sessions in logs, URLs, browser storage or ordinary preferences.
10. Avoid a backend until a concrete Web or cross-device requirement demonstrates the need.

## 4. MCat adapter — discovery required

The adapter should expose application-level operations rather than leaking Telegram message details into UI code. Its eventual responsibilities may include:
- Search tracks, albums and playlists.
- Start an acquisition request.
- Observe progress and results.
- Cancel where supported.
- Retry/resume where supported.
- Resolve acquired media into importable items.

This is not yet a Kotlin interface. The real contract depends on inspection of the existing bot and its interaction model.

Document before implementation:
- Bot identity and command/request format.
- Destination chat and request routing.
- Response correlation.
- Multi-message results and pagination.
- Media delivery and references.
- Out-of-order response behaviour.
- Concurrency limits and isolation between jobs.
- Timeout, retry, cancellation and failure semantics.
- Whether albums/playlists are first-class results or collections of tracks.
- Stable identifiers and metadata returned.

A Telegram bot conversation is not automatically equivalent to a synchronous HTTP API. Correlation, concurrency and recovery need explicit handling.

## 5. Acquisition pipeline

Proposed stages:

1. **Request:** user starts a track, album or playlist search/acquisition.
2. **Resolve:** adapter communicates with MCat and correlates responses.
3. **Normalise:** convert results into a stable internal representation.
4. **Match:** compare with the local library using Telegram identity and metadata.
5. **Plan:** classify items as present, missing, ambiguous or failed.
6. **Review:** present ambiguous matches; do not delete automatically.
7. **Acquire:** receive/download media using the supported mechanism.
8. **Import:** validate media and metadata, then persist library records transactionally.
9. **Relate:** create/update playlist and album relationships idempotently.
10. **Complete:** persist outcome and report complete, partial or failed status.

Support partial success. A failure for one track must not roll back successful independent tracks.

## 6. Persistence and recovery

Spotygram already persists its library and pending download state in SQLite. Inspect the current schema and migration conventions before adding tables.

Potential new persistent concepts, subject to protocol discovery:
- Acquisition job and requested item.
- Job progress/state and last processed response cursor or message identity.
- Mapping between MCat result identity and imported library track.
- Optional playlist source metadata and import provenance.

Do not duplicate information already represented by tracks, playlists or pending downloads. Prefer a migration of the existing schema over a second catalogue.

A job state machine should define legal transitions. A provisional example is:

**CREATED → SEARCHING → READY → ACQUIRING → IMPORTING → COMPLETED**

with explicit **PARTIAL**, **FAILED**, **CANCELLED** and **RETRYABLE** outcomes. Final states must match the actual bot workflow.

## 7. Album identification

Keep album grouping/completion independent of MCat transport. The library should identify likely album groups even when MCat is unavailable.

Separate metadata normalisation, candidate grouping, release identity/confidence, track-list completeness and MCat search/acquisition. Do not treat album title as a unique release identifier. Support confidence levels and manual correction for ambiguous metadata.

## 8. Web architecture boundary

No Web client is part of the checked-in Android/desktop Gradle target list. Shared Kotlin/Compose code must not be assumed to be a production-ready browser application.

After Android stabilisation, evaluate a separate Web client and narrowly scoped service/API. Address authentication, per-user authorisation, bot credentials, media delivery, job status, storage, rate limiting and auditability. Browser code must never receive privileged Telegram or MCat credentials.

Do not introduce the Web service into the Android critical path. Android must remain functional if the future Web service is offline.

## 9. Android Auto and TV

These are deferred platform clients:
- Android Auto should use Android media browsing/session APIs and Media3, with a restricted driver-appropriate interface.
- Android TV / Google TV needs a dedicated remote-first Compose experience and explicit focus/D-pad handling.
- Both should reuse library and playback use cases rather than duplicate data or acquisition logic.

## 10. Identity, packaging and migration

Inventory inherited namespace, application ID, signing configuration, update channels, package names, release assets and stored-data paths before changing product identity.

Changing **app.spotygram** may make Android treat MLib as a different app and prevent an in-place update from the Spotygram package. Do not change application ID, signing identity, update endpoint or data paths casually. Decide whether MLib is a separate install or a migration/continuation before the first distributable MLib build.

Review inherited artwork and notices. The upstream third-party notice states that the Takopi character/photo is not covered by the software's MIT licence. A differentiated MLib identity must not reuse artwork without confirming rights.

## 11. Security and privacy

- Treat the MCat bot as a privileged integration and keep credentials in a trusted execution environment.
- Do not log full Telegram sessions, bot tokens, private message contents or unnecessary personal data.
- Validate media type, size, metadata and file paths before import.
- Scope job ownership and result visibility to the initiating user/account.
- Do not expose private Telegram media through public/shared caches.
- Define retention and cleanup for temporary media and failed jobs.
- Keep acquisition/import auditable without recording secrets.


## 12. Existing MCat implementation — source inspection

The separate repository [ginjeira/MCat](https://github.com/ginjeira/MCat) currently contains a Python/Telethon Telegram workflow and a FastAPI Web MVP. The inspected code is documented in [MCat integration assessment](MCAT-INTEGRATION.md).

The important distinction is that MCat currently forwards Telegram media messages to a configured destination chat; it does not return audio bytes through its HTTP API. The Web API exposes authenticated search and job control/status. MLib may therefore use MCat as a control plane and the configured Telegram destination as the media delivery/source channel.

The Web service has a shared server-side Telegram user session and destination, serialises Telegram work with one lock and processes queued jobs with one worker. Web job records are associated with Web usernames, but the Telegram session and destination are installation-wide. This is not an independent Telegram account/session per MLib user.

The API does not currently return a per-track destination message-ID list. Robust MLib import correlation will need a verified strategy or a separately approved MCat API enhancement. The current integration assessment is static only; live operation and deployed configuration remain unverified.
