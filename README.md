# MLib

**MLib is an independent music library for Telegram.** It is being developed from the Spotygram codebase, with native Telegram-bot music acquisition inspired by the MCat and MCat Player projects.

<blockquote>
<strong>Development status:</strong> MLib is not yet ready for general use. The Android application has a separate package identity and private storage from Spotygram. Builds, Telegram sign-in, playback, acquisition and data isolation must pass the documented validation gates before a first functional release.
</blockquote>

## Product

MLib aims to let users collect, organise, discover, complete and play music:

- Connect a Telegram account and select personal chats, channels and profile sources.
- Maintain a searchable local music library, favourites and playlists.
- Play music with queue, background playback and offline storage.
- Search the Telegram music bot for tracks, albums and playlists, then import acquired music into the library.
- Optionally create a local playlist from an acquired MCat playlist.
- Identify likely albums in the existing library and offer to find missing tracks.
- Review ambiguous matches; never silently delete or overwrite existing media.

The application interface is intended to support **Portuguese (Portugal) and English**.

## Project boundaries

| Project | Role |
| --- | --- |
| MLib | The independent application and library |
| Spotygram | Third-party technical starting point; must coexist with MLib |
| MCat | Reference for the original bot-acquisition protocol; no runtime dependency |
| MCat Player | Main functional reference for search, acquisition, deduplication and collection workflows; not a runtime dependency |

MLib implements the bot workflow directly through its own authenticated TDLib session and forwards received media to the destination selected in the app. It does not call the MCat service/API, share its sessions, or require an MCat server. The MCat production system remains unchanged.

## Platforms

1. **Android phone/tablet** — first priority.
2. **Web** — after Android acquisition and library workflows stabilise.
3. **Android Auto and Android TV / Google TV** — later evaluation.
4. **Native iOS** — out of scope; Web is the intended access path for Apple devices.

The inherited desktop module is being kept buildable as a development target. A production desktop installer and updater are not enabled yet.

## Documentation

- [Product specification](docs/mlib/PRODUCT-SPEC.md)
- [Source-project feature matrix](docs/mlib/SOURCE-FEATURE-MATRIX.md)
- [Architecture](docs/mlib/ARCHITECTURE.md)
- [MCat integration assessment](docs/mlib/MCAT-INTEGRATION.md)
- [Implementation roadmap](docs/mlib/ROADMAP.md)
- [Decision record](docs/mlib/DECISIONS.md)
- [Implementation audit and validation status](docs/mlib/IMPLEMENTATION-AUDIT.md)
- [Independent application identity](docs/mlib/INDEPENDENT-IDENTITY.md)
- [Telegram and MCat onboarding design](docs/mlib/TELEGRAM-ONBOARDING.md)
- [Build instructions](docs/build.md)

The inherited [Spotygram user guide](docs/usage.md) and [architecture guide](docs/architecture.md) are reference material only. They do not establish that the corresponding features have already been validated in MLib.

## Build and release status

The project has a separate Android application ID:

- Release: `pt.ginjeira.mlib`
- Debug: `pt.ginjeira.mlib.dev`

The Android release workflow is prepared but requires MLib-owned Telegram application credentials and a new MLib signing key. Desktop release packaging remains disabled until its installer and update trust chain are migrated.

Do not install an upstream Spotygram APK as MLib. Do not use the Spotygram signing key, data directory, Telegram session or CloudSync channel for MLib.

No production MLib release is available yet. Follow the [implementation audit](docs/mlib/IMPLEMENTATION-AUDIT.md) and [roadmap](docs/mlib/ROADMAP.md) for the actual status and acceptance evidence.

## Licence

The inherited source repository is distributed under the MIT licence; third-party dependencies and artwork retain their own notices. See [LICENSE](LICENSE) and [third-party notices](docs/third-party.md).
