# MLib — decision record

Record decisions and unresolved questions here. Add new decisions rather than silently rewriting history.

## D-001 — Product identity

**Status:** Accepted  
**Decision:** The new product is named **MLib**.  
**Rationale:** Library describes the product more broadly than a catalogue or player and gives it an identity separate from Spotygram and MCat Player.

## D-002 — Technical starting point

**Status:** Accepted as initial direction  
**Decision:** Retain the forked Spotygram application as the initial technical base. Preserve existing functionality and change visual identity only where appropriate.  
**Rationale:** Kotlin/Compose, TDLib, SQLite and playback provide a substantial base. A rewrite adds risk without an established need.

## D-003 — MCat meaning and boundary

**Status:** Accepted  
**Decision:** MCat is the separately developed system for obtaining music through a Telegram bot. It is a capability integrated into MLib, not the application name.  
**Rationale:** Separates product/library responsibilities from acquisition.

## D-004 — MCat Player status and role

**Status:** Accepted  
**Decision:** MCat Player was never in production, so it has no production history to preserve and is not a runtime dependency. It is nevertheless an explicit functional and knowledge source for MLib. Its useful workflows, features and lessons must be inventoried and selectively reimplemented.  
**Rationale:** MLib should benefit from the work already done in MCat Player without inheriting its architectural/security weaknesses or treating it as an operational system.

## D-005 — Platform scope and sequence

**Status:** Accepted  
**Decision:** Initial focus is Android. Web follows Android stabilisation. Android Auto and Android TV / Google TV are evaluated afterwards. Native iOS is out of scope; Web is the intended Apple-device access path.  
**Rationale:** Focuses initial delivery and allows platform-specific requirements to be designed deliberately.

## D-006 — Preserve Android local-first operation

**Status:** Accepted  
**Decision:** Do not make Android dependent on a new central MLib backend.  
**Rationale:** Spotygram operates directly with Telegram and local storage. A future Web service must not become a critical Android dependency.

## D-007 — Automatic playlist creation

**Status:** Accepted requirement; implementation details pending  
**Decision:** MLib should optionally create a local playlist when a playlist is obtained through MCat. The option must be configurable.  
**Rationale:** Organises imported collections while allowing users to opt out.

## D-008 — Album identification and completion

**Status:** Accepted requirement; matching design pending  
**Decision:** MLib should identify likely albums from the existing library and offer to search MCat for missing tracks.  
**Rationale:** Makes the library an active collection-management tool. Matching confidence and release identity require care.

## D-009 — No automatic destructive deduplication

**Status:** Accepted  
**Decision:** Matching may reuse existing tracks or flag duplicates, but must not automatically delete or overwrite user media. Ambiguous matches require review.  
**Rationale:** Telegram object, recording and library-entry identity are not interchangeable.

## D-010 — Existing MCat production system

**Status:** Accepted  
**Decision:** MLib development must not modify the existing MCat production system without separate explicit authorisation.  
**Rationale:** Protects the working acquisition system.

## D-011 — Application identity and update compatibility

**Status:** Open — blocking before first distributable MLib APK  
**Question:** Should MLib install alongside Spotygram as a separate app, or replace/migrate an existing Spotygram installation?  
**Impact:** Application ID, signing identity, update channel, package naming and data migration depend on this. Do not change inherited identity/signing until decided.

## D-012 — MCat bot protocol and service surface

**Status:** Partially investigated; live validation still required  
**Finding:** Static inspection of ginjeira/MCat confirms a Telethon-based Telegram user session, bot-driven search, a FastAPI Web API, SQLite job records and forwarding of acquired Telegram media to a configured destination chat. The Web API does not return audio bytes or per-track destination message IDs. The Telegram session and destination are shared by the MCat installation; a single lock and worker serialise its Telegram operations.  
**Remaining work:** Verify the deployed revision and live request/response behaviour, confirm destination access from MLib's Telegram account, and establish reliable per-job media correlation.  
**Impact:** A client adapter must respect the shared execution lane and cannot assume direct media delivery or independent Telegram sessions per Web user.

## D-013 — Desktop product support

**Status:** Open  
**Question:** Will MLib ship inherited Windows, Linux and macOS builds, or focus on Android plus Web?  
**Impact:** Branding, packaging, update channels and regression scope.

## D-014 — Album metadata source

**Status:** Open  
**Question:** Should initial album grouping rely on local tags and MCat results, or should an external metadata catalogue be considered?  
**Impact:** Determines confidence in missing-track identification and release distinction. Do not require an external service without demonstrated need.

## D-015 — Web service boundary

**Status:** Open — decide after Android stabilisation  
**Question:** Which Web operations require a service, and how will authentication, user isolation, MCat credentials and media delivery work?  
**Impact:** Web must not expose privileged credentials or make Android dependent on a central service.

## D-016 — Visual identity and inherited artwork

**Status:** Open  
**Question:** Which Spotygram visual assets will be retained, adapted or replaced?  
**Impact:** Upstream notice says the Takopi character/photo is not covered by the software's MIT licence. The code licence does not grant rights to that artwork.


## D-017 — Both MCat projects as knowledge sources

**Status:** Accepted  
**Decision:** Both the MCat acquisition system and the separate MCat Player project are sources of functionality and technical knowledge for MLib. Spotygram remains the initial codebase.  
**Rationale:** MCat supplies the working acquisition workflow; MCat Player supplies additional user-facing capabilities and lessons. The feature matrix records candidate carry-forward decisions.

## D-018 — Candidate MCat integration path

**Status:** Candidate; requires end-to-end proof  
**Decision direction:** Investigate using the existing MCat FastAPI service for search/job orchestration and the configured Telegram destination as the media delivery channel. MLib would ingest forwarded messages through its existing TDLib/Spotygram source pipeline.  
**Rationale:** Reuses both existing systems and avoids implementing a second Telegram bot workflow or media-transfer engine.  
**Conditions:** Confirm the MLib Telegram account can read the destination, secure native-client authentication, establish per-job message correlation, and verify partial/failure/retry behaviour. The current API is an MVP and lacks per-track destination message IDs. Do not modify the production MCat service or rely on this path as final architecture until the proof succeeds.
