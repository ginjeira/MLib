# MLib — project documentation

**Status:** project definition and planning  
**Product:** MLib  
**Repository:** https://github.com/ginjeira/MLib  
**Technical starting point:** https://github.com/Krablante/spotygram

MLib retains the existing Spotygram application as its initial technical base and adds a separate MCat acquisition capability. The initial goal is to preserve inherited Spotygram behaviour while introducing music, album and playlist discovery and acquisition through the MCat Telegram-bot system.

These documents describe the intended product. Planned features are not represented as implemented or validated.

## Documents

- [Product specification](PRODUCT-SPEC.md) — scope, user capabilities and requirements.
- [Architecture](ARCHITECTURE.md) — current baseline, proposed MCat boundary and technical constraints.
- [Roadmap](ROADMAP.md) — phases, dependencies and acceptance gates.
- [Decision record](DECISIONS.md) — accepted decisions and unresolved questions.

## Status vocabulary

- **Inherited:** present in the Spotygram fork at inspection; not independently revalidated in MLib.
- **Planned:** agreed direction, not implemented yet.
- **Under investigation:** requires source inspection, protocol discovery or a proof of concept.
- **Validated:** demonstrated by a recorded test against an identified build and environment.

## Project boundaries

- MCat means the separately developed Telegram-bot-based music acquisition system. It is not another name for MLib.
- MCat Player was a prototype and source of feature ideas, not a production system or required code dependency.
- Spotygram is the technical starting point. Preserve its capabilities unless a deliberate, documented change is approved.
- Do not modify the existing MCat production system as part of MLib work without separate explicit authorisation.
- Initial focus is Android. Web follows Android stabilisation. Android Auto and Android TV / Google TV are deferred until then.
- Native iOS is out of scope. Web is the intended access route for Apple devices.

Start with the product specification, then architecture and decisions. Use the roadmap to track implementation. Update these documents when decisions change; do not silently turn assumptions into requirements.
