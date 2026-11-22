# MLib project documentation

This directory contains the product specification, architecture, source-project feature matrix, MCat integration assessment, decisions, roadmap and implementation records.

| Document | Purpose |
| --- | --- |
| [Product specification](PRODUCT-SPEC.md) | Agreed scope, core user workflows and platform sequence |
| [Architecture](ARCHITECTURE.md) | Target architecture and technical boundaries |
| [Source-project feature matrix](SOURCE-FEATURE-MATRIX.md) | Functional contribution of Spotygram, MCat and MCat Player |
| [MCat integration assessment](MCAT-INTEGRATION.md) | Inspected MCat API, Telegram delivery and integration constraints |
| [Decisions](DECISIONS.md) | Accepted decisions and open questions |
| [Roadmap](ROADMAP.md) | Phased implementation and acceptance criteria |
| [Implementation audit](IMPLEMENTATION-AUDIT.md) | Current source state, identity changes and validation evidence |
| [Independent identity](INDEPENDENT-IDENTITY.md) | Separate installation, storage, signing and release identity |
| [Persistence and sync](PERSISTENCE-AND-SYNC.md) | Local schema, CloudSync protocol and recovery-key model |
| [MCat Player port status](MCAT-PLAYER-PORT-STATUS.md) | Current parity matrix, remaining port work and release impact |
| [Product and UX proposal](PRODUCT-UX-PROPOSAL.md) | What to keep/change/reject, new features and Spotify-inspired UI direction |
| [Execution roadmap by waves](../../roadmap.md) | Ordered implementation plan for Android |
| [Waves closure](WAVES-CLOSURE.md) | What is admissible to close now and what stays open |
| [Independent audit remediation](INDEPENDENT-AUDIT-REMEDIATION.md) | Audit findings, source fixes and remaining validation |
| [Telegram and MCat onboarding](TELEGRAM-ONBOARDING.md) | Proposed guided setup reusing the local TDLib session |

The product specification and feature matrix define the functional target. The implementation audit and CI records define what is actually implemented and validated. A planned feature must not be described as complete until its acceptance criteria have evidence.

MLib is an independent product. Spotygram is the third-party technical starting point; MCat is the acquisition service; MCat Player is a functional reference only. They remain distinct systems.
