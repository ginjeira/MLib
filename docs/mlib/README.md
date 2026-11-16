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
| [Telegram and MCat onboarding](TELEGRAM-ONBOARDING.md) | Proposed guided setup reusing the local TDLib session |

The product specification and feature matrix define the functional target. The implementation audit and CI records define what is actually implemented and validated. A planned feature must not be described as complete until its acceptance criteria have evidence.

MLib is an independent product. Spotygram is the third-party technical starting point; MCat is the acquisition service; MCat Player is a functional reference only. They remain distinct systems.
