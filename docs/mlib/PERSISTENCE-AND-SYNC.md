# MLib — persistence and multi-device synchronisation

**Status:** persistence and sync source audit completed. Encrypted protocol v4 and recovery-key pairing are implemented in source. Automated CI and real two-device acceptance remain pending.

## Decision

MLib remains local-first. Android and desktop keep independent SQLite databases and independent TDLib sessions. Telegram is the remote transport and storage for MLib-owned synchronisation records. No central MLib server, Firebase, Supabase or shared SQLite file is introduced.

The synchronisation channel is not a music source, an MCat destination, or the Spotygram CloudSync channel.

## Local persistence

- The library is stored in the local SQLite database named `library.db`, currently schema version 8.
- Tracks, sources, profiles, playlists and playlist membership are represented in the catalogue.
- Playback progress has a local `playback_progress` table.
- Android and desktop maintain independent databases.
- TDLib file identifiers are cleared when a client lifetime changes; durable Telegram message/document references remain in the catalogue.

## CloudSync protocol

- `LibrarySync` stores account-scoped records in SQLite tables `sync_meta` and `sync_values`.
- Local changes are journalled transactionally. Pending records remain pending until Telegram confirms the send and the matching local revision is acknowledged.
- A stable per-installation writer UUID and logical clock support deterministic conflict resolution.
- `CloudSync` uses the already authenticated TDLib client. It does not create a second Telegram client or copy a TDLib session to another device.
- The remote store is an MLib-owned private broadcast supergroup, identified by `mlib-storage/1` and the `mlib-sync/4` protocol. It is muted, archived and configured without message auto-deletion.
- New snapshots and deltas are gzip-compressed and then encrypted with AES-256-GCM. A fresh 96-bit nonce is generated for each payload and protocol metadata (account, payload kind and snapshot cursor) is authenticated as additional data.
- The encrypted envelope is size-bounded and checked with SHA-256 before decryption. GCM authentication is mandatory; failed decryption is an error and is never treated as an empty library.
- Protocol versions 1–3 remain readable for migration. All new writes use encrypted version 4. Once an encrypted snapshot is confirmed, normal snapshot compaction can remove covered legacy messages.
- The receiver merges records into its own SQLite database and materialises the resulting library state. It does not copy or replace the sender's database.

## Encryption key and device pairing

- The first trusted device creates a random 256-bit key from a cryptographically secure random generator.
- MLib displays a Base64URL recovery code. The user must transfer it manually to every additional device through a trusted channel. It is not posted to Telegram and is not included in the sync payload.
- Android wraps the key with a dedicated AES key held by Android Keystore. Desktop stores it in the operating-system keyring. The raw shared key is not written to ordinary preferences or the SQLite database.
- A device without a key is held in `key_required` state and does not discover/create a storage channel or upload plaintext/new-format data.
- Importing a recovery code validates its length and refuses to silently replace a different key already installed on that device.
- Losing the recovery code and all devices holding the key means the encrypted remote data cannot be recovered. Export and store the code safely before adding devices or replacing hardware.
- Every device must run a build that understands protocol v4 and import the same recovery code before encrypted sync is enabled. Older MLib builds cannot consume v4 messages.
- The shared key provides confidentiality and integrity against parties that do not possess it. All paired devices share the same key; this is not per-device sender attribution or protection against a compromised paired device.
- Telegram can still observe channel membership and message metadata such as timing, size, account identifier, payload kind and cursor. The payload contents are encrypted.

## Data synchronised

- Track descriptors and Telegram references needed to identify remote media.
- Library source enablement and titles.
- Selected Telegram profile metadata.
- Favourites.
- Playlist identity, name, membership and ordering.
- The supported shared application settings.

Actual audio files, local filesystem paths and TDLib authorisation/session data are not part of the sync records.

## Data deliberately kept per device

- Playback queue and current playback position/progress.
- Playback speed, order and repeat mode.
- Download/cache files and temporary audio.
- TDLib client state, local database encryption/key material and device-specific runtime state.

A second device reconstructs the catalogue and can resolve Telegram media references, but it does not inherit the first device's local audio cache or active playback session.

## Conflict behaviour

- Records are keyed by logical identity and merged using logical clock, then writer UUID as a deterministic tie-break.
- Playlist membership is represented as independent per-track boolean records.
- Playlist order is a separate last-writer-wins value. Concurrent reorder operations do not currently have a user-facing conflict resolver.
- Deletion tombstones take precedence over live values. Track tombstones do not delete a locally downloaded file; a local track with a non-empty path is retained.
- The receiver applies a batch transactionally and reloads the library after material changes.

These rules still require concurrent real-device validation.

## Automated validation

The JVM test suite covers:
- Record validation, canonical encoding, duplicate/unsupported keys, legacy-field exclusion, deterministic tie-breaking and tombstones.
- Populated schema-7 to schema-8 database migration and preservation of library and sync journal.
- AES-GCM round-trip, fresh nonces, wrong-key rejection, ciphertext tampering, AAD mismatch, malformed envelope and invalid key length.

These tests do not replace Telegram integration or physical-device acceptance.

## Two-device acceptance plan

Use the same Telegram user account but separate MLib installations, TDLib sessions and local databases.

1. Update both devices to a v4-capable build. Do not pair a device running an older protocol.
2. On device A, connect Telegram, open Settings → Chave de cifragem e recuperação, create the first key and securely save the recovery code outside Telegram.
3. On device B, connect Telegram and import the exact same recovery code before sync starts.
4. Confirm both devices discover the same MLib storage and neither discovers or changes Spotygram's channel.
5. Add/enable a source, favourite a track and create a playlist on A; confirm the records appear on B.
6. Rename the playlist, reorder it and change membership on B; confirm the result returns to A without duplicate entries.
7. Make independent changes on both devices while offline; reconnect in both orders and confirm deterministic convergence.
8. Interrupt connectivity during an upload, force-stop the app and restart; confirm pending changes are retried and not duplicated or lost.
9. Restart both devices and confirm library, favourites, playlists and settings remain present.
10. Confirm local queue, playback progress and downloaded files remain local and are not deleted or overwritten by sync.
11. Test sign-out/account switch and verify that one Telegram account's records are not materialised into another account's library.
12. Verify that a device without a recovery key remains blocked and that an incorrect recovery code fails without changing its existing key.

## Release gate

Do not declare multi-device support complete until CI passes, the encrypted protocol has passed review, and the two-device acceptance plan has been completed with recorded evidence. No merge or release is authorised by this implementation work.
