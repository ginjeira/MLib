# MLib — product specification

**Status:** initial agreed scope; requirements will be refined during implementation.

## 1. Product vision

MLib is a music library that lets users collect, organise, discover, complete and play music. It combines the existing Spotygram library/player experience with MCat, the separately developed Telegram-bot-based system for obtaining music, and selectively carries forward useful features and lessons from the separate MCat Player project.

The distinguishing workflow is not merely downloading a track. MLib should understand the user's collection, identify incomplete albums and make it straightforward to obtain missing music through MCat. Both MCat and MCat Player are functional/knowledge sources: MCat for the actual acquisition engine and its playlist/deduplication logic; MCat Player for user-facing workflows, source management, library concepts, duplicate review and playback/MediaSession lessons. See [Source projects and feature matrix](SOURCE-FEATURE-MATRIX.md).

## 2. Product principles

1. **Preserve before extending:** retain existing Spotygram functionality unless a change is intentional and documented.
2. **Local-first Android:** the app must remain useful without depending on a new central MLib backend.
3. **Separate acquisition from library management:** MCat obtains media; MLib manages tracks, albums, playlists and playback.
4. **Recoverable operations:** long-running acquisition/import work should survive ordinary lifecycle changes and recover safely from interruption where possible.
5. **No destructive automation:** matching and deduplication must not silently delete, overwrite or replace user files.
6. **Honest status:** distinguish requested, queued, downloading, imported, failed and completed work.
7. **Platform-aware parity:** define Web, Auto and TV capabilities against platform constraints.

## 3. Platforms and delivery order

| Platform | Scope | Priority |
| --- | --- | --- |
| Android phone/tablet | Primary application; preserve Spotygram and integrate MCat | Initial |
| Web browser | Access to supported library and acquisition functions | After Android stabilisation |
| Android Auto | Driver-appropriate media browsing and playback | Later evaluation |
| Android TV / Google TV | Remote-control-friendly library and playback UI | Later evaluation |
| Native iOS | Explicitly out of scope | Not planned |

The existing desktop modules are inherited from Spotygram. Their future as supported MLib targets is undecided; their presence does not imply a commitment to ship Windows, Linux or macOS.

## 4. Inherited capabilities to preserve

The fork's README and architecture documentation describe these existing capabilities. This is the preservation baseline, not a new MLib test result:

- Telegram sign-in through TDLib.
- Browsing selected Telegram chats and profile music.
- Searchable music library backed by SQLite.
- Local audio import and offline copies.
- Playback, queue management, background playback and Android media controls.
- Favorites and playlists.
- Downloads and on-device storage management.
- Library synchronisation through the user's Telegram account.
- Existing desktop application, platform playback, update mechanisms and packaging.

A baseline verification pass must confirm the fork builds and critical inherited flows still work before feature development.

## 5. MCat acquisition

Planned capabilities:

- Search individual tracks, albums and playlists.
- Request and receive selected music through the existing MCat Telegram-bot workflow.
- Import acquired media into the MLib library.
- Report progress, partial success and errors.
- Resume or safely retry interrupted work where the bot workflow supports it.

The bot protocol is not specified here. Command syntax, response structure, target chat, message correlation, media delivery, concurrency and retry semantics must be discovered from the existing MCat implementation before designing the adapter. Do not invent a protocol or change the production bot as part of this phase.

## 6. Optional playlist creation

When a playlist is obtained through MCat, MLib should optionally create a corresponding local playlist.

Requirements:
- Configurable in Settings and disabled by user choice when desired.
- Preserve source title and track order where available and reliable.
- Link entries to acquired or already-existing library tracks.
- Avoid duplicate entries when a job is retried.
- Keep playlist state separate from acquisition-job state: a failed track must not discard successful tracks or the playlist.
- Report missing or failed tracks instead of silently omitting them.

Whether the playlist is created at job start or after the first successful result, and how repeated imports update it, must be decided after inspecting MCat result semantics.

## 7. Album identification and completion

MLib should analyse the existing library and identify groups of tracks that appear to belong to the same album.

Candidate signals:
- Normalised album title and artist.
- Track number and disc number.
- Track title and artist.
- Duration and other available tags.
- External identifiers, if already present or supplied by a future trusted catalogue integration.

Similar album titles alone are not sufficient to merge releases. Different editions, remasters, deluxe editions, compilations and live recordings may be distinct.

For an identified album, MLib should:
1. Show tracks already present.
2. Estimate missing positions/tracks when a reliable track list exists.
3. Offer to search that album through MCat.
4. Compare MCat results with the current library.
5. Present proposed missing tracks for review.
6. Acquire selected missing tracks.
7. Update the library and associated playlist/album view without duplicate media or memberships.

The first implementation may group albums from local metadata. A reliable external track listing is a separate capability and must not be assumed.

## 8. Deduplication and identity

Distinguish:
- **Same Telegram media object:** same stable Telegram document/message identity.
- **Same recording:** potentially the same audio despite different Telegram messages or metadata.
- **Same library entry:** a track record and its playlist/album relationships.

A Telegram document identifier is a strong signal for the same delivered object, not a universal recording identity. Metadata similarity is probabilistic and must not trigger automatic deletion.

Reuse existing tracks when the match is sufficiently reliable. Present ambiguous matches for review. Never automatically delete or replace user media as a side effect of importing.

## 9. Settings

At minimum, MCat-related Settings should include the option to enable/disable automatic playlist creation after playlist acquisition. Add other settings only when a concrete behaviour requires user control.

## 10. Web phase

Web development starts after Android MLib acquisition and library workflows have stabilised. The client should expose the largest practical subset, potentially:
- Browse/search library.
- View albums and playlists.
- Manage playlist membership and favourites.
- Search/request MCat acquisition if the service and security model permit it.
- Play audio supported by the browser.

Browser limitations apply to background execution, local files, codecs, session security and long-running downloads. Never expose a Telegram full-account session or bot credentials to browser JavaScript. Web authentication and service architecture require a separate design.

## 11. Deferred platform evaluation

After Android stabilisation:
- **Android Auto:** evaluate MediaLibraryService / MediaSession / Media3 and permitted car UI; prioritise safe, driver-appropriate browsing and playback.
- **Android TV / Google TV:** design a ten-foot interface with D-pad navigation, visible focus, readable typography and remote interaction.

Neither target is part of the initial Android acceptance gate.

## 12. Out of scope

- Native iOS application.
- Replacing the MCat bot or changing production behaviour without separate approval.
- Rewriting the Spotygram core without demonstrated need.
- Mandatory central backend for Android.
- Automatic deletion/replacement of existing media.
- Guaranteed feature parity between Android and browser.
- Treating MCat Player as a production dependency or source of operational history.
