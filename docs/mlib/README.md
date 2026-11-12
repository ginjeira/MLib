# MLib upgrade work

This directory contains the implementation audit and onboarding design for the in-place Spotygram-to-MLib transition.

| Document | Purpose |
| --- | --- |
| [Implementation audit](IMPLEMENTATION-AUDIT.md) | Current source architecture, persistence, release identity and migration constraints. |
| [Telegram and MCat onboarding](TELEGRAM-ONBOARDING.md) | Proposed automatic/manual-assisted setup flow that reuses the existing TDLib session. |

The product specification, architecture decisions, feature-source matrix and roadmap are maintained in the separate foundation documentation branch and PR #1. This implementation branch does not merge or replace that review.

All findings in the implementation audit are based on static source inspection unless a document explicitly says otherwise. No build, runtime or Telegram-live validation is implied.