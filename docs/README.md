# Documentation

[MLib project documents](mlib/README.md) · [Spotygram inherited application guides](#inherited-spotygram-guides) · **English** · [Русский](README.ru.md)

## MLib project

The [MLib project documentation](mlib/README.md) defines the new product scope, architecture, decisions and implementation roadmap. MLib is being developed from this fork while retaining the inherited Spotygram application as its initial technical base.

| MLib document | Purpose |
| --- | --- |
| [Product specification](mlib/PRODUCT-SPEC.md) | Scope, platforms and functional requirements |
| [Architecture](mlib/ARCHITECTURE.md) | Existing baseline and proposed design |
| [MCat integration assessment](mlib/MCAT-INTEGRATION.md) | Source inspection of the existing MCat repository and integration options |
| [Roadmap](mlib/ROADMAP.md) | Phases and acceptance criteria |
| [Decision record](mlib/DECISIONS.md) | Accepted decisions and open questions |

## Inherited Spotygram guides

The guides below describe the inherited Spotygram application and its current implementation. They are reference material for the MLib baseline; they must not be read as proof that MLib-specific features have been implemented.

To install the inherited player and listen to music, start with the user guide. The development guides explain how to build Android and desktop packages and where each part of the application belongs.

| Guide | What you will find |
| --- | --- |
| [Using Spotygram](usage.md) | Installation, sign-in, playlists, offline files, storage, updates and troubleshooting |
| [Build and release](build.md) | Required tools, API credentials, signing and GitHub releases |
| [Architecture](architecture.md) | Telegram access, the library, playback and update checks |
| [Verification record](verification.md) | What was actually checked, how, and what remains unverified |
| [Third-party notices](third-party.md) | Dependencies and licenses |

Each inherited guide has a language switch at the top. English uses the plain file name; translations add a language code before .md, such as usage.ru.md or usage.ua.md. To add a language, translate the five guides and both entry pages, then add them to their navigation. Keep topics and current facts aligned across editions; wording can differ. The application interface has its own XML translations.

The inherited Spotygram downloads and release history remain at the [upstream releases page](https://github.com/Krablante/spotygram/releases). MLib release assets must be established separately when the project is ready to publish.
