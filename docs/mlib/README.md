# MLib — project documentation

**Status:** project definition and planning  
**Product:** MLib  
**Repository:** https://github.com/ginjeira/MLib  
**Technical starting point:** https://github.com/Krablante/spotygram

MLib retains the existing Spotygram application as its initial technical base and integrates the separate MCat acquisition system. Both **MCat** and **MCat Player** are knowledge and functional sources for MLib: MCat contributes the existing acquisition workflow and operational logic; MCat Player contributes user-facing features and lessons to evaluate and selectively reimplement. They are distinct projects and must not be conflated.

## Documents

- [Product specification](PRODUCT-SPEC.md) — scope, user capabilities and requirements.
- [Architecture](ARCHITECTURE.md) — current baseline, proposed MCat boundary and technical constraints.
- [MCat integration assessment](MCAT-INTEGRATION.md) — source inspection of the existing MCat repository.
- [Source projects and feature matrix](SOURCE-FEATURE-MATRIX.md) — how Spotygram, MCat and MCat Player contribute to MLib.
- [Roadmap](ROADMAP.md) — phases, dependencies and acceptance gates.
- [Decision record](DECISIONS.md) — accepted decisions and unresolved questions.

## Status vocabulary

- **Inherited:** present in the Spotygram fork at inspection; not independently revalidated in MLib.
- **Planned:** agreed direction, not implemented yet.
- **Candidate:** feature/behaviour identified in MCat or MCat Player for evaluation and possible adoption.
- **Under investigation:** requires source inspection, protocol discovery or a proof of concept.
- **Validated:** demonstrated by a recorded test against an identified build and environment.

## Project boundaries

- Spotygram is the technical starting point and its existing capabilities should be preserved unless a deliberate, documented change is approved.
- MCat is the existing Telegram-bot-based acquisition system.
- MCat Player is a separate functional reference and source of features/lessons; it was not a production system.
- Both MCat projects must inform MLib requirements and implementation. Their useful functionality should be carried forward selectively, without copying known architectural/security weaknesses.
- Do not modify the existing MCat production system without separate explicit authorisation.
- Initial focus is Android. Web follows Android stabilisation. Android Auto and Android TV / Google TV are deferred until then.
- Native iOS is out of scope. Web is the intended access route for Apple devices.

Start with the product specification, source feature matrix and architecture. Use the roadmap to track implementation. Update these documents when decisions change; do not silently turn assumptions into requirements.
