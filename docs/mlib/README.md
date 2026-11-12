# MLib upgrade work

This directory contains the MLib implementation audit, independent-identity migration and Telegram/MCat onboarding design.

| Document | Purpose |
| --- | --- |
| [Implementation audit](IMPLEMENTATION-AUDIT.md) | Source architecture, persistence, release identity and migration constraints. |
| [Independent application identity](INDEPENDENT-IDENTITY.md) | Separate Android package, desktop runtime data, build state, signing and coexistence validation gates. |
| [Telegram and MCat onboarding](TELEGRAM-ONBOARDING.md) | Proposed automatic/manual-assisted setup flow that reuses the existing TDLib session. |

The product specification, architecture decisions, feature-source matrix and roadmap are maintained in the separate foundation documentation branch and PR #1. This implementation branch does not merge or replace that review.

All findings in the implementation audit are based on static source inspection unless a document explicitly says otherwise. No build, runtime or Telegram-live validation is implied.