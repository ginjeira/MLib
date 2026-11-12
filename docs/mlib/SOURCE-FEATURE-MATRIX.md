# MLib — source projects and feature-porting matrix

**Status:** initial functional inventory, 2026-11-12  
**Purpose:** make explicit that both MCat and MCat Player are knowledge and feature sources for MLib, while keeping their roles distinct.

## 1. Source-project roles

MLib draws on two separate projects:

| Source | Role in MLib | What it contributes |
| --- | --- | --- |
| Spotygram | Technical foundation | Kotlin/Compose application, TDLib integration, SQLite library, Media3 playback, local files, playlists, downloads, device playback controls |
| MCat | Acquisition system and operational knowledge | Telegram bot interaction, track/album/playlist search and collection, media forwarding to GinjMusic, playlist lifecycle, destination indexing, deduplication, job orchestration, current Web MVP |
| MCat Player | Functional reference and product knowledge | User-facing music acquisition workflows, multiple Telegram sources, source/history indexing, unified library concepts, playlists/albums, duplicate review, background processing, streaming/cache lessons, MediaSession/Android Auto lessons |

These are different repositories and different systems. MCat is not MCat Player. Neither replaces Spotygram as the initial Android codebase.

## 2. Candidate functionality to carry into MLib

| Capability | Spotygram baseline | MCat knowledge/capability | MCat Player reference | MLib direction |
| --- | --- | --- | --- | --- |
| Telegram user sign-in and personal sources | Existing TDLib integration | Telethon user session is used by MCat service | Telegram authentication and multiple sources | Preserve Spotygram account/source behaviour; do not share its user session with MCat service |
| Search tracks | Not the primary acquisition path | Existing bot-backed search | Implemented workflow reference | Integrate through MCat |
| Search albums | Library metadata may exist | Existing bot-backed search | Album workflows/reference | Integrate through MCat; compare against local library |
| Search playlists | Local playlist management | Existing bot-backed search | Playlist acquisition workflow | Integrate through MCat |
| Acquire/forward media | Existing Telegram source and local import | Forwards Telegram media to GinjMusic | Acquisition and processing reference | Reuse MCat delivery; import through Spotygram/MLib pipeline |
| Multiple Telegram sources | Existing source browsing | Configured service bot/destination/dedup sources | Multiple source support | Preserve source selection; distinguish personal sources from shared MCat destination |
| Unified library | SQLite catalogue | MCat stores acquisition/job/dedup metadata, not the MLib library | Unified library concept | Keep Spotygram SQLite library authoritative for MLib tracks |
| Playlist management | Existing playlists | Playlist collection, ordering and forwarding lifecycle | Playlist and album workflows | Preserve native playlists; optionally create playlist from MCat playlist acquisition |
| Album grouping/completion | Must be inspected in current fork | Search can return album results | Library/album concepts and workflows | Add local album analysis and missing-track completion |
| Deduplication | Existing behaviour must be audited | Document ID plus metadata comparison against destination | Document identity and destination-aware duplicate handling | Combine source identity and metadata matching; no silent deletion |
| Duplicate audit/cleanup | Not assumed | MCat avoids forwarding duplicates; destination index | Review-before-delete concepts and selective duplicate management | Provide review/audit only; no automatic destructive cleanup |
| Background jobs | Existing download/background behaviour | Single worker, queued jobs, interruption marked on restart | Background acquisition and recovery lessons | Persist MLib jobs and recover safely; MCat service limitations must be respected |
| Audio playback | Media3 on Android | Telegram media remains in destination | Player and streaming/cache lessons | Reuse Media3 and library; do not introduce a second streaming engine without need |
| Streaming/cache | Existing playback/storage implementation to inspect | Telegram-forwarded media | Cache and streaming implementation reference | Inspect inherited mechanisms; retain only what improves reliability |
| MediaSession / Android Auto | Android playback integration to verify | Not the primary role | Previous Android Auto attempt/reference | Implement/validate native Media3 media browsing after core Android stability |
| Synchronisation | Existing Telegram-based library sync | Shared service destination | Cross-device sync concepts | Preserve Spotygram sync; design future Web separately |
| Web client | Not in current Android/desktop target | FastAPI Web MVP exists | Web/product interaction reference | Web phase later; MCat Web API is a candidate control plane, not yet a stable public contract |
| Android TV / Google TV | Shared application/core potential | Not the primary role | Player UX reference only | Dedicated remote-first UI after Android core is stable |

This table is a candidate porting inventory. It does not assert that every MCat Player feature is present in MLib or should be copied unchanged.

## 3. Important MCat capabilities to preserve or reuse

The MCat source inspection confirms these behaviours in the existing acquisition system:

- Bot-backed searches for tracks, albums, playlists and artists.
- Selection of numbered search results through Telegram inline buttons.
- Collection of media messages after a selected result is started.
- Playlist lifecycle detection using start/finish messages.
- Track metadata capture, including Telegram document ID, title, performer, duration and file size.
- Destination indexing and duplicate checks.
- Duplicate recognition by Telegram document ID and a normalised metadata tuple.
- Forwarding of non-duplicate messages to the configured destination.
- Web login, search, queued jobs and job status endpoints.
- A single serialised Telegram worker and a shared installation-level Telegram session/destination.

The MCat integration assessment explains the current API and its limitations. Do not fork or rewrite this workflow inside MLib before proving that the existing service can be reused safely.

## 4. Important MCat Player lessons and candidate features

The MCat Player audit and earlier product work identify these candidate features:

- Telegram authentication.
- Search and acquisition of music.
- Multiple Telegram sources and source/history indexing, including the music-check workflow.
- Unified music library.
- Playlists and albums.
- Deduplication by Telegram document identity, with separate treatment of duplicate tracks within a playlist and tracks already present in the destination GinjMusic.
- Duplicate audit and selective cleanup, with review and confirmation before deletion.
- Background processing and recovery requirements.
- Audio format handling, streaming and cache behaviour.
- MediaSession and Android Auto integration lessons.

The exact MCat Player code paths are not automatically reusable. Its audit identified architectural and security limitations, including in-memory state, weak job isolation, shared Telegram conversation concurrency, cache isolation concerns and incomplete native Android Auto integration. MLib should carry forward useful behaviour and lessons, not reproduce those weaknesses.

## 5. Deduplication model

MLib must keep three identity layers separate:

1. **Telegram object identity:** stable Telegram document/message identity. This identifies the same delivered object.
2. **Recording identity:** whether two different media objects contain the same recording. Metadata/audio comparison may be needed and can be uncertain.
3. **Library identity:** one MLib track record and its album/playlist relationships.

For playlist acquisition:
- Avoid duplicate entries within the imported playlist.
- Reuse a library track already present when the match is sufficiently reliable.
- Distinguish a track already in GinjMusic from a duplicate inside the source playlist.
- Preserve the audit trail of why a result was treated as present/duplicate.
- Send ambiguous cases to review.
- Never delete an existing track or Telegram message automatically.

If a future cleanup feature is added, it must first show the full duplicate set, identify the retained item, explain the matching evidence and require explicit confirmation for each destructive action.

## 6. Confirmed destination access

The user has confirmed that the Telegram account intended for MLib has access to the GinjMusic group, where MCat forwards acquired music.

This satisfies the user-side access precondition for the proposed integration route. It does **not** yet prove:
- that the account can read every required message/media type;
- that TDLib sees new forwarded messages with the required metadata;
- that MLib can correlate a destination message to a specific MCat job;
- that the deployed MCat revision matches the inspected source.

These points require a controlled end-to-end test.

## 7. Next technical proof

1. Identify the deployed MCat revision/configuration without exposing credentials.
2. Verify the MLib TDLib account can enumerate and read the GinjMusic group.
3. Submit one controlled track acquisition through the current MCat Web API.
4. Record the job ID, selected result, status transitions and destination message(s).
5. Verify the same message(s) are observable through the MLib/TDLib account.
6. Test a reliable correlation strategy between job and destination messages.
7. Import one test track into the Spotygram library pipeline and verify playback.
8. Repeat the same import to confirm idempotency.
9. Only then finalise the MCat adapter and expand to albums/playlists.

No production MCat configuration or code should be changed for this proof unless separately approved.
