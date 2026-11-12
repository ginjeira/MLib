# MLib Telegram and MCat onboarding design

**Status:** proposed design; no onboarding code or live Telegram operation has been executed.

## Goal

Provide an automatic or manual-assisted setup journey that prepares the Telegram prerequisites for MCat acquisition while reusing the existing Spotygram/MLib Telegram implementation. The user should not need to understand TDLib, bot administration, Telegram group permissions or MCat internals.

## Non-negotiable boundaries

- Reuse the one existing TDLib client and the already authenticated user session.
- Never request a second Telegram login for MCat.
- Keep the MCat bot/service identity and the user's TDLib identity separate. The user's account interacts with Telegram; MCat's service account performs the existing acquisition and forwarding workflow.
- Keep the private muted library-sync channel separate from the MCat music destination group.
- Discover and validate existing resources before offering to create anything.
- Do not change the production MCat service or its configured TG_DESTINATION without explicit approval.
- Never delete, recreate, rename or repurpose a user's Telegram group automatically.
- Make every setup action resumable and idempotent. Persist progress locally, but re-check Telegram state before trusting a stored result.
- Distinguish verified state from user confirmation and from an operation that has merely been requested.

## Proposed onboarding state machine

Each prerequisite should have one of: NOT_CHECKED, NEEDS_ACTION, READY, BLOCKED, or ERROR. The wizard advances only when a check confirms the prerequisite.

1. **Existing MLib Telegram session** — inspect the current TDLib authorization and connection state. If already ready, reuse it. If not authenticated, show the existing Spotygram login UI.
2. **MCat service reachability** — verify the configured MCat API endpoint and API version. Do not ask the user for bot credentials or store the MCat service's Telethon session in the Android app.
3. **Bot identity and conversation** — obtain the official bot username from trusted MCat configuration/API metadata. Resolve it with TDLib, open the conversation and check whether the bot can be contacted. If Telegram requires an initial user action, guide the user to press Start or send /start, then re-check.
4. **Destination group discovery** — first read the destination configured by the MCat service. Resolve that Telegram chat using the existing TDLib chat cache/search. Do not create a different group while the backend still forwards to a fixed destination.
5. **User access and permissions** — confirm the account is a member and can read the destination messages/media. Use existing TDLib chat/group information and permission inspection where possible. A successful name lookup alone is not proof of membership or media access.
6. **Bot membership and destination permissions** — where the MCat backend requires the bot to be in the destination, verify the bot is present and has the rights required to post audio/documents. If it is absent, provide a guided/manual action unless the API and Telegram method can safely perform the invite with explicit confirmation.
7. **TDLib source registration** — after destination access is confirmed, offer to add the group as a library source using the existing Library.source operation. Do not add it silently if the user has not approved the library scope.
8. **Controlled end-to-end test** — use a single known test track/job, record the MCat job identifier and expected result, observe the destination message, verify TDLib can read the resulting media, correlate the job with the message, and import through the existing library pipeline. Do not create a playlist or alter existing tracks as a side effect of the test.
9. **Ready** — show which prerequisites are verified, which were completed manually and which remain blocked. Provide a retry path for individual failed steps.

## Automatic versus manual-assisted actions

| Operation | Preferred mode | Notes |
| --- | --- | --- |
| Check TDLib login/connection | Automatic | Existing Telegram state flows. |
| Find bot and destination chats | Automatic | Reuse existing chat loading/search and public-chat resolution. |
| Check group type/membership/permissions | Automatic where TDLib exposes reliable state | Use the existing permission inspection patterns from Sharing. |
| Open bot conversation | Automatic navigation, user confirmation where required | Do not assume opening a chat means /start has completed. |
| Send /start | Manual-assisted by default | Only automate after trusted bot identity and safe, explicit user consent are established. |
| Add MCat bot to a group | Manual-assisted by default | Group administrator rights and bot invitation rules vary. |
| Create a new Telegram group | Manual-assisted by default | Only useful if MCat backend can be configured to use that group. Never create a group that the service cannot target. |
| Add the group as a library source | User-approved automatic action | Reuse Library.source; this affects what the library indexes. |
| Test acquisition and import | Automatic after confirmation | One track; retain job/message evidence; idempotent import. |

## Critical dependency: MCat destination configuration

The inspected MCat production implementation has a configured TG_DESTINATION and forwards acquired media there. The current API does not expose a verified per-user destination selection or a contract that returns destination message IDs for each acquired track. Therefore:

- The first integration should target the existing configured destination (currently understood to be GinjMusic) if it is compatible and the user has access.
- A group created by the wizard must not be presented as a working MCat destination until the backend has explicitly confirmed that it is configured to forward there.
- Per-user groups require a separately designed MCat service capability: destination selection/registration, authorization, validation, persistence, and safe concurrency. This is not an Android-only feature.
- The wizard must not alter the production MCat TG_DESTINATION as a side effect.

## Reuse of existing Spotygram capabilities

- Authentication: Telegram.auth and Telegram.connection.
- Chat population: SpotygramApp.loadChats, searchChats and addPublicChat.
- Chat metadata and caching: Telegram.rememberChat and the chat cache/revision flows.
- Membership/permission inspection: the recipient inspection logic in Sharing, adapted to read requirements rather than send permissions blindly.
- Library source registration: Library.source, after explicit user approval.
- Media read/import: existing message update handling, TrackReader/resolve/import pipeline, after job-to-message correlation is validated.
- Progress and notices: existing application state flows and action/error reporting.

Do not make the onboarding wizard a second Telegram client or a second catalogue. It should orchestrate existing services through narrow interfaces.

## Proposed components

- MCatApiClient: versioned HTTP adapter for MCat API calls. No UI code should call endpoints directly.
- MCatCapabilities: describes API version, bot identity, configured destination and supported job/correlation operations.
- TelegramPrerequisiteChecker: read-only TDLib checks for login, bot, destination, membership and permissions.
- TelegramSetupActions: explicit, auditable actions requiring user consent; separates actions the user must complete in Telegram UI.
- MCatOnboarding: persistent step state and orchestration. It stores stable Telegram chat/user identifiers and API job identifiers, not credentials or duplicate TDLib session data.
- MCatAcquisitionAdapter: translates MCat search/job state into the existing library import pipeline.

## Outstanding questions before implementation

1. What exact bot username and metadata can the deployed MCat API expose reliably?
2. Does the current deployed MCat API expose its configured destination and can it confirm the destination's Telegram identity?
3. Can the MCat bot be added to an existing group, and what exact rights are required?
4. Does the deployed API support cancellation, retry/idempotency keys, per-track result identifiers and job-to-message correlation?
5. Is per-user destination configuration in scope, or will the first release use the existing GinjMusic destination?
6. Which onboarding steps did MCat Player automate versus merely guide? This must be confirmed from its source before reproducing its exact workflow.

## Acceptance criteria

- An already authenticated Spotygram user reaches the wizard without another Telegram login.
- Existing GinjMusic access is detected and reused; no unnecessary group is created.
- The wizard clearly distinguishes the private sync channel from the MCat music destination.
- Missing prerequisites are described with a specific action and a re-check.
- No production MCat configuration is changed.
- A test acquisition can be correlated with the delivered Telegram message and imported once.
- Reopening the wizard resumes safely and does not duplicate actions, sources, tracks or playlists.
- No Telegram message, group, track or local file is deleted as part of setup.
## Additional source audit: exact MCat Player setup behaviour

The MCat-Player main branch was inspected on 2026-11-12, including SetupWizardModal.tsx and the server-side setup route. The original workflow is more specific than a generic group-creation wizard:

1. The automatic tab calls POST /api/telegram/setup/auto.
2. The server uses the current MCat Player per-user Telethon client/session and sends /start to the configured bot username. The default is @MusicsHuntersbot (overridable through TG_BOT_USERNAME). The route reports success when the send call succeeds; it does not prove that the bot returned a successful activation response.
3. It searches existing dialogs for a non-Saved-Messages chat whose title contains 'mcat player', contains 'mcat -', or equals 'mcat'.
4. If none is found, it creates a private broadcast channel, not a basic group or supergroup, titled '🎵 MCat Player - Músicas & Arquivo'.
5. It sets that channel as both destination and source in the MCat Player user context, and sets it as the cloud-configuration chat.
6. It attempts to invite @MusicsHuntersbot to the channel. If invitation fails, the wizard reports a warning and suggests that the user may promote the bot to administrator if direct forwarding is required. The code does not automatically grant administrator rights.
7. It writes a #MCAT_PLAYER_CONFIG record (or a JSON document for a large payload) to the channel and attempts to pin the configuration message.
8. If channel creation/discovery fails, the route falls back to Telegram Saved Messages as source and destination and attempts to persist the configuration there.
9. It refreshes dialogs and scans the selected channel for tracks.

The manual tab lets the user select destination and source independently and trigger cloud-config synchronisation. The wizard therefore treats the music destination/source and its own playlist/config persistence as related but distinct settings.

### What must not be copied literally

- MCat Player's server owns a separate Telethon client and session per user context. MLib must not export its TDLib session or create a second Telegram login just to reproduce that workflow.
- MCat Player's server-side session persistence and browser localStorage strategy are not a suitable reason to send the MLib TDLib authorization data to a backend. Keep MLib's Telegram session local to the device.
- The original wizard's channel-title matching is broad and may select an unrelated channel. MLib must use explicit identity/config markers and ask for confirmation when a match is ambiguous.
- The original wizard calls a successful /start send 'bot activated' without verifying the bot's response. MLib must track command-sent and bot-confirmed as different states.
- The original route calls the channel 'destination' after creating it, but the production MCat service inspected separately uses a server-configured TG_DESTINATION. A channel created in the user's account does not automatically change that production destination.
- The original wizard can report cloud persistence success based on the helper's return path. MLib should verify that the configuration message can be read back and parsed before declaring the step READY.

### Three Telegram resources that must remain distinct

1. **MLib library-sync channel:** the private, muted channel managed by Spotygram CloudSync for library metadata and cross-device sync. Preserve its current protocol markers and format.
2. **MCat Player-style personal music archive:** the optional private broadcast channel created by the MCat Player wizard, with the MCat bot invited. It can be both a music source and destination only when the acquisition backend is configured for that user's channel.
3. **Production MCat destination:** the existing configured destination used by the production MCat service (currently understood as GinjMusic). The service's fixed TG_DESTINATION remains authoritative until a per-user destination API is implemented and approved.

These resources are not interchangeable. The first release should validate and use the production MCat destination where that is the active acquisition path. The MCat Player-style personal archive can be offered only as a separate optional setup path after backend support is proven. The MLib sync channel must never be repurposed as a music archive.

### Updated implementation order

- First reuse TDLib to detect the signed-in user, discover the known bot and destination, inspect access, and add an approved destination as a library source.
- Then reproduce the safe parts of the MCat Player setup: explicit bot activation, create-or-select a personal private broadcast channel, invite the bot with consent, and verify membership/permissions.
- Keep destination selection behind a backend capability check. If the active MCat API exposes only fixed TG_DESTINATION, do not claim the newly created personal channel is the acquisition destination.
- Implement cloud playlist/config persistence only if MLib actually needs to import MCat Player's configuration format. Prefer MLib's own LibrarySync for MLib library state rather than creating a parallel playlist database in Telegram.
- Do not grant bot administrator rights automatically. Explain the exact permission required and let the user complete it manually in Telegram.