# MLib — persistence and multi-device synchronisation

**Status:** source audit completed against `feat/mlib-independent-identity` at `e5719091c00c171d6777dba8748872ac84f34466`. Runtime multi-device acceptance is still pending.

## Decision

MLib remains local-first. Android and desktop keep independent SQLite databases and independent TDLib sessions. Telegram is the remote transport and storage for MLib-owned synchronisation records. No central MLib server, Firebase, Supabase or shared SQLite file is introduced.

The synchronisation channel is not a music source, an MCat destination, or the Spotygram CloudSync channel.

## What already exists

### Local persistence

- The library is stored in the local SQLite database named `library.db`.
- The current library schema version is 8.
- Tracks, sources, profiles, playlists and playlist membership are represented in the existing catalogue.
- Playback progress has a local `playback_progress` table.
- Android opens the database through its private application storage. Desktop uses a local SQLite file with WAL and a busy timeout.
- TDLib file identifiers are cleared when a client lifetime changes; durable Telegram message/document references remain in the catalogue.

### CloudSync

- `LibrarySync` stores account-scoped records in SQLite tables `sync_meta` and `sync_values`.
- Local changes are journalled transactionally with the user operation. Pending records remain pending until Telegram confirms the send and the matching local revision is acknowledged.
- A stable per-installation writer UUID and logical clock support deterministic conflict resolution.
- `CloudSync` uses the already authenticated TDLib client. It does not create a second Telegram client or copy a TDLib session to another device.
- The remote store is an MLib-owned private broadcast supergroup, identified by `mlib-storage/1` and `mlib-sync/3`. It is muted, archived and configured without message auto-deletion.
- Snapshots and incremental records are gzip-compressed, size-bounded and checked with SHA-256 before being accepted.
- The receiver merges records into its own SQLite database and materialises the resulting library state. It does not copy or replace the sender's database.

### Data currently synchronised

- Track descriptors and Telegram references needed to identify remote media.
- Library source enablement and titles.
- Selected Telegram profile metadata.
- Favourites.
- Playlist identity, name, membership and ordering.
- The supported shared application settings.

Actual audio files, local filesystem paths and TDLib authorisation/session data are not part of the sync records.

### Data deliberately kept per device

- Playback queue and current playback position/progress.
- Playback speed, order and repeat mode.
- Download/cache files and temporary audio.
- TDLib client state, local database encryption/key material and device-specific runtime state.

This separation is intentional: a second device reconstructs the catalogue and can resolve Telegram media references, but it does not inherit the first device's local audio cache or active playback session.

## Conflict behaviour observed in source

- Records are keyed by logical identity and merged using logical clock, then writer UUID as a deterministic tie-break.
- Playlist membership is represented as independent per-track boolean records, so additions/removals of different tracks can merge independently.
- Playlist order is a separate last-writer-wins value. Concurrent reorder operations do not currently have a user-facing conflict resolver.
- Deletion tombstones take precedence over live values. Track tombstones do not delete a locally downloaded file; a local track with a non-empty path is retained.
- The receiver applies a batch transactionally and reloads the library after material changes.

These rules are source observations, not yet proof of correct behaviour under concurrent real-device operation.

## Gaps and risks

1. **No end-to-end two-device validation has been recorded.** A successful build and a successful Telegram login do not prove remote channel discovery, snapshot transfer, incremental sync, conflict handling or recovery.
2. **No dedicated sync unit tests existed at the audited revision.** This change adds JVM tests for record validation, canonical encoding, legacy-field exclusion, deterministic tie-breaking and tombstones. They do not replace integration tests.
3. **Remote payloads are not encrypted by MLib.** Gzip and SHA-256 provide compression and integrity checking, not confidentiality or sender authentication. The Telegram channel is private to the account, but Telegram cloud-channel content is not end-to-end encrypted.
4. **Local playback state is not cross-device state.** Queue and playback progress are deliberately excluded. They remain local and must be verified for persistence across app restart/upgrades.
5. **Account switching and first-run races need real-device tests.** In particular, simultaneous first login on two devices may create more than one MLib storage channel; the implementation contains discovery logic for this case, but it has not been exercised here.
6. **Schema upgrades need regression testing.** The current database is version 8 and has upgrade paths that install/migrate sync tables. Existing user databases must survive upgrades without data loss.
7. **The supported settings set must remain aligned with the product locales and current UI.** Sync validation must reject unsupported or obsolete values rather than applying invalid preferences.

## Encryption decision to implement before production use

Do not put a shared encryption key in the same Telegram channel as the encrypted payload and call that end-to-end encryption.

The no-server design needs an explicit device-pairing/recovery mechanism. Candidate direction:

- Generate a random MLib sync key on the first trusted device.
- Let the user transfer it to another device through a QR code or a high-entropy recovery code, with an explicit confirmation step.
- Store the key locally wrapped by each platform's protected keystore/keyring.
- Encrypt and authenticate each snapshot/delta with an authenticated cipher (for example AES-256-GCM), using a fresh nonce and authenticated protocol metadata.
- Version the envelope and support a deliberate migration from the current plaintext format. Never silently interpret a failed decrypt as an empty library or overwrite remote state.
- Provide a recovery/export path before the user changes or loses all paired devices.

This design is not implemented by this change. It needs its own review, format version and compatibility tests.

## Acceptance plan

### Automated

- Validate record schema, size limits, unsupported keys and duplicate keys.
- Verify canonical JSON encoding is independent of object field order.
- Verify deterministic conflict tie-breaks and deletion-tombstone precedence.
- Verify legacy queue/progress/playback fields are ignored.
- Add database migration tests for a populated pre-sync database and an existing sync-enabled database.
- Add protocol tests for snapshot/delta envelope parsing, compression limits, digest/authentication failure and interrupted send/ack behaviour.

### Two physical Android devices

Use the same Telegram user account but separate MLib installations, TDLib sessions and local databases.

1. Install the same MLib build on devices A and B and sign in on both.
2. Confirm each discovers the same MLib sync storage and neither discovers or changes Spotygram's channel.
3. Add/enable a source, favourite a track and create a playlist on A; confirm the records appear on B.
4. Rename the playlist, reorder it and change membership on B; confirm the result returns to A without duplicate entries.
5. Make independent changes on both devices while offline; reconnect in both orders and confirm deterministic convergence.
6. Interrupt connectivity during an upload, force-stop the app and restart; confirm pending changes are retried and not duplicated or lost.
7. Restart both devices and confirm library, favourites, playlists and settings remain present.
8. Confirm local queue, playback progress and downloaded files remain local and are not deleted or overwritten by sync.
9. Test sign-out/account switch and verify that one Telegram account's records are not materialised into another account's library.
10. Repeat first-run setup with both devices initially offline, then connect them in turn; verify that duplicate storage channels are detected and reconciled safely.

### Release gate

Do not declare multi-device support complete until the automated checks pass, the encryption/pairing decision is implemented and reviewed, and the two-device acceptance plan has been completed with recorded evidence.
