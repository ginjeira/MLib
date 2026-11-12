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