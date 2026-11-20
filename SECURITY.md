# Security policy

MLib is a personal project currently distributed as automatic evaluation releases. Only the latest published release is supported.

## Reporting a vulnerability

Do not open a public issue for a vulnerability. Use GitHub's private vulnerability reporting on this repository (Security -> Report a vulnerability) so the report and any proof of concept remain private until a fix is available.

Include:

- affected version and platform (Android ABI or desktop OS);
- the affected component (for example Telegram sign-in, CloudSync, update verification or audio import);
- reproduction steps or a minimal proof of concept;
- whether the issue requires a malicious local app, a Telegram account, physical access or a compromised remote service.

Never include real Telegram API hashes, session data, recovery codes, signing keys or user media in a report.

## Release integrity

Android releases are signed with the MLib release key. The release workflow fails if the APK signer certificate SHA-256 does not match the expected MLib fingerprint. Users should verify the published SHA-256 checksums before installing.

Desktop update packaging is disabled. Desktop update verification fails closed until an MLib signing key is provisioned.

## Out of scope

- Vulnerabilities in Telegram itself, TDLib, Android, the operating system or third-party music bots.
- Reports requiring a rooted device, a compromised OS, a compromised GitHub account or the MLib signing key.
- Terms-of-service or copyright disputes; those are product/legal issues, not software vulnerabilities.
