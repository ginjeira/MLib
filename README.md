# MLib

**MLib is an independent music library for Telegram.** It is being developed from the Spotygram codebase, with native Telegram-bot music acquisition inspired by the MCat and MCat Player projects.

<blockquote>
<strong>Development status:</strong> MLib is not yet ready for general use. Automatic evaluation releases were used through <code>v0.1.25</code> only. New versions now require explicit owner agreement; automatic publication is disabled unless the repository variable <code>MLIB_AUTO_RELEASE=true</code> is set. These builds are not a stable production channel. The Android application has a separate package identity and private storage from Spotygram.
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

1. **Android phone/tablet (arm64-v8a and x86_64)** — first priority. 32-bit-only Android devices are not supported by the current native build.
2. **Web** — after Android acquisition and library workflows stabilise.
3. **Android Auto and Android TV / Google TV** — later evaluation.
4. **Native iOS** — out of scope; Web is the intended access path for Apple devices.

The inherited desktop module is being kept buildable as a development target. A production desktop installer and updater are not enabled yet.

## Documentation

- [Product specification](docs/mlib/PRODUCT-SPEC.md)
- [Source-project feature matrix](docs/mlib/SOURCE-FEATURE-MATRIX.md)
- [Architecture](docs/mlib/ARCHITECTURE.md)
- [MCat integration assessment](docs/mlib/MCAT-INTEGRATION.md)
- [MCat Player port status and remaining work](docs/mlib/MCAT-PLAYER-PORT-STATUS.md)
- [Product, UX and interface proposal](docs/mlib/PRODUCT-UX-PROPOSAL.md)
- [Execution roadmap by waves](roadmap.md)
- [Persistence and CloudSync](docs/mlib/PERSISTENCE-AND-SYNC.md)
- [Implementation history](docs/mlib/ROADMAP.md)
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

The Android release workflow requires MLib-owned Telegram application credentials and the MLib signing key. Each successful baseline run on <code>main</code> bumps the patch version, creates a tag and publishes a public evaluation release automatically. The workflow pins the expected signer certificate SHA-256 so a wrong keystore fails instead of silently breaking updates.

Desktop release packaging remains disabled until its installer, manifest and update trust chain are migrated to the MLib identity. The inherited Spotygram update public key has been removed, so desktop updates fail closed.

Do not install an upstream Spotygram APK as MLib. Do not use the Spotygram signing key, data directory, Telegram session or CloudSync channel for MLib. Security reports should follow [SECURITY.md](SECURITY.md).

There is no stable production release. Evaluation builds are published under the automatic release workflow and may change without notice. Follow the [implementation audit](docs/mlib/IMPLEMENTATION-AUDIT.md), [independent audit remediation](docs/mlib/INDEPENDENT-AUDIT-REMEDIATION.md) and [roadmap](docs/mlib/ROADMAP.md) for status and acceptance evidence.

## Third-party bot and privacy

Music acquisition uses the third-party Telegram bot workflow referenced by MCat. MLib is not affiliated with that bot, Telegram or the music providers, and users are responsible for complying with their terms and applicable copyright law. Album-cover suggestions send the album title and artist to Apple iTunes Search and Deezer over HTTPS. CloudSync stores encrypted library payloads in a private Telegram channel; Telegram can still observe channel and message metadata. Desktop update checks contact GitHub. No analytics or advertising SDK is included.

## Licence

The inherited source repository is distributed under the MIT licence; third-party dependencies and artwork retain their own notices. See [LICENSE](LICENSE) and [third-party notices](docs/third-party.md).
