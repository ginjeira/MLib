# MLib — MCat integration assessment

**Assessment date:** 2026-11-12  
**Source inspected:** https://github.com/ginjeira/MCat, default branch main  
**Assessment type:** static source/documentation inspection. No MCat service was started and no live Telegram exchange was performed.

## 1. Executive finding

MCat is not currently a conventional media-download API. Its main workflow uses a Telegram user session through Telethon to interact with the Music Downloader bot, collect the resulting Telegram media messages and forward those messages to a configured Telegram destination.

The MCat repository also contains a FastAPI Web MVP. It exposes authenticated search and job endpoints, but the media itself is still delivered through Telegram to the configured destination. The API returns search options and job status/summaries; it does not currently expose an audio-file download endpoint or a stable per-track list of destination message IDs.

This creates a plausible integration route for MLib: use the MCat Web service as the search/acquisition control plane, then ingest the forwarded Telegram media through the existing Spotygram/MLib Telegram source and library pipeline. This is a candidate architecture, not yet validated end-to-end.

## 2. Inspected implementation

Relevant source files:

- [MCat README](https://github.com/ginjeira/MCat/blob/main/README.md)
- [Python package/dependencies](https://github.com/ginjeira/MCat/blob/main/pyproject.toml)
- [Console Telegram workflow](https://github.com/ginjeira/MCat/blob/main/src/mcat/__main__.py)
- [Playlist lifecycle and deduplication](https://github.com/ginjeira/MCat/blob/main/src/mcat/playlist_engine.py)
- [FastAPI routes and web authentication](https://github.com/ginjeira/MCat/blob/main/src/mcat/web.py)
- [Telegram Web service and job worker](https://github.com/ginjeira/MCat/blob/main/src/mcat/web_service.py)

The project uses Python, Telethon, FastAPI, Uvicorn and SQLite. It uses a Telegram user session named by configuration (default: mcat_user_session) and a configured bot username and destination chat.

## 3. Existing Web API

The inspected FastAPI application currently exposes these relevant routes:

| Route | Purpose |
| --- | --- |
| POST /api/login | Authenticate a Web user and issue a session cookie |
| GET /api/me | Return current authentication state and CSRF token |
| POST /api/search | Search the configured bot and return selectable options |
| POST /api/jobs | Queue selected search options for acquisition/forwarding |
| GET /api/jobs | List recent jobs for the authenticated Web user |
| GET /api/jobs/{job_id} | Retrieve one job and its item results |

Search accepts a query, content type and provider. The inspected implementation supports track, album, playlist and artist content types, with Deezer and SoundCloud providers. It interacts with the bot's inline filters, parses numbered options and stores the search for 20 minutes.

A job accepts a search ID and selected option numbers. The service clicks each selected result, observes the bot's Telegram messages, collects media metadata and forwards the messages to the configured destination. Job results include per-option status and summary counts such as forwarded, duplicates, missing and expected tracks.

The API uses a username/password login, an HttpOnly session cookie, a CSRF token and server-side session records. Passwords are stored as PBKDF2-SHA256 hashes. These are Web application credentials, not Telegram credentials.

## 4. Important operational constraints

The current Web service is multi-user at the Web login/job-record level, but its Telegram execution context is shared by the installation:

- One server-side Telethon user session is used.
- One configured bot is used.
- One configured TG_DESTINATION is used by all Web users.
- Optional TG_DEDUP_SOURCES are used for duplicate checks.
- A single asyncio lock serializes Telegram interactions.
- A single worker processes queued jobs sequentially.
- Search records and jobs are associated with the Web username.
- On service restart, queued/running jobs are marked interrupted; the code does not automatically resume them.

The README explicitly says not to run the console process and Web process simultaneously with the same Telegram session.

Consequently, MLib must not treat the current MCat service as an isolated Telegram session per MLib user, nor assume parallel acquisition jobs are supported. Any client integration must respect the single shared Telegram execution lane and the configured destination.

## 5. Media delivery semantics

The current MCat code does not download, decode, convert or process audio bytes. It forwards the Telegram media message itself from the bot conversation to TG_DESTINATION.

The playlist engine:
- Detects playlist start from a message containing Total Tracks.
- Detects completion from Finished.
- Records source message IDs, Telegram document IDs and audio metadata.
- Compares against configured destination/dedup-source indexes.
- Deduplicates by Telegram document ID or by normalised title, performer, duration and file size.
- Does not delete duplicate Telegram messages.

This means the destination Telegram chat is currently the delivery channel for media. The MCat API does not return the audio file or, in the inspected response shape, a per-track destination message ID list.

## 6. Destination access confirmed by user

The user has confirmed that the Telegram account intended for MLib has access to the GinjMusic group, where MCat forwards acquired music. This satisfies the user-side access precondition for investigating destination ingestion. It does not yet prove that TDLib can enumerate/read all required media or that MLib can correlate individual destination messages with an MCat job. Those remain end-to-end validation items.

## 7. Candidate MLib integration flow

The least disruptive first design to investigate is:

1. MLib authenticates to the existing MCat Web service using a dedicated MCat Web account.
2. MLib submits a search through the existing search endpoint.
3. MLib displays returned options and lets the user select one or more.
4. MLib creates a job and displays progress/status from the job endpoints.
5. MCat forwards acquired Telegram media to its configured destination.
6. MLib ingests those Telegram messages through the existing TDLib source/library machinery.
7. MLib reconciles the resulting tracks and, where requested, creates a local playlist or completes an album.

This reuses the existing MCat workflow and the Spotygram Telegram/library pipeline rather than adding a second media-transfer implementation.

### Preconditions to validate

- The Telegram account signed into MLib must be able to read the configured MCat destination chat.
- The destination must be suitable as a source for MLib and should not contain unrelated traffic that makes job reconciliation ambiguous.
- The MLib client must be able to securely authenticate to the MCat Web service over HTTPS.
- MLib must not store MCat passwords or session cookies in plain preferences/logs.
- Only one MCat acquisition operation should be active at a time until the service exposes reliable job correlation and capacity guarantees.
- The current API does not expose destination message IDs. A robust per-job import therefore needs a reliable correlation mechanism.

## 8. API gaps for a first-class MLib client

The current Web API is an MVP, not yet a versioned public integration contract. Before relying on it from MLib, evaluate:

- Versioned API contract and compatibility policy.
- Dedicated client authentication or token/session lifecycle suitable for native clients.
- Explicit per-item result identifiers.
- Destination chat ID and destination message IDs for successfully forwarded items.
- Per-item metadata and delivery outcome.
- Job cancellation and safe retry semantics.
- Idempotency keys for client retries.
- A way to distinguish media forwarded by a given job from unrelated messages in the destination.
- Service availability and maintenance reporting.
- Rate limits and a clear single-worker/concurrency contract.

Do not expose the MCat Telegram user session or bot credentials to MLib. The native client should only communicate with a narrowly scoped authenticated service interface.

## 9. What should not be done yet

- Do not replace the current MCat bot interaction with a new protocol.
- Do not change or deploy modifications to the existing MCat production service as part of this MLib documentation phase.
- Do not assume MCat returns audio bytes directly.
- Do not assume a successful MCat job automatically means MLib has imported every forwarded track.
- Do not run a second Telethon client using the same MCat session file.
- Do not assume multiple simultaneous searches/jobs are safe.
- Do not automatically delete or overwrite existing MLib tracks based on MCat duplicate summaries.

## 10. Validation still required

This assessment is based on source inspection only. It does not establish that the current deployed MCat service is running this exact revision, that the Web endpoints are reachable from MLib, or that the configured destination is accessible to the MLib Telegram account.

Next technical proof:
1. Identify the actual deployed MCat revision and configuration without exposing secrets.
2. Confirm the MLib Telegram account can read the MCat destination.
3. Exercise one small search and one small acquisition through the existing MCat Web API in a controlled test.
4. Record the job response and the corresponding destination messages.
5. Verify MLib can ingest those messages through the existing Spotygram source flow without duplicates.
6. Only then define the stable adapter contract.
