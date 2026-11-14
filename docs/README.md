# Documentation

[MLib](../README.md) · **English**

MLib is an independent development fork. The user and architecture guides below are inherited Spotygram reference material; they are not yet a validated MLib user manual. The build guide documents the current MLib development workflow.

| Guide | What you will find |
| --- | --- |
| [Inherited Spotygram user guide](usage.md) | Reference for inherited sign-in, playlists, offline files, storage and player behaviour; not yet validated for MLib |
| [Build and release](build.md) | Required tools, API credentials, signing and GitHub releases |
| [Inherited architecture guide](architecture.md) | Technical reference for the source application; MLib differences are recorded under `mlib/` |
| [Verification record](verification.md) | What was actually checked, how, and what remains unverified |
| [Third-party notices](third-party.md) | Dependencies and licenses |

The project documentation is maintained in English. The application interface supports Portuguese (Portugal) and English; Portuguese strings live in the platform resource bundle and English is the fallback.

There are currently no production MLib releases. Do not use the upstream Spotygram releases as MLib installers. MLib release assets, when available, will be published on the [MLib releases page](https://github.com/ginjeira/MLib/releases).
Detailed earlier verification evidence remains in the tagged source history,
linked from the verification record.

## MLib upgrade work

The independent MLib implementation audit, application identity, CloudSync isolation and Telegram/MCat onboarding design are documented in the [MLib work index](mlib/README.md). The implementation is reviewed separately from the product-foundation documents.
