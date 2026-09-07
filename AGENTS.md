# TJ Telegram Project Instructions

## Scope and identity

- This repository contains an independent Android Telegram client named TjGram.
- Keep product names, package-facing labels, settings, links, and new identifiers TjGram-specific. Do not introduce branding or runtime dependencies from reference clients.
- Preserve upstream Telegram behavior unless a TJ feature explicitly changes it.

## Project structure

- `TMessagesProj/` contains the shared Android application code and resources.
- `TMessagesProj_App/` contains the buildable application variants.
- TJ runtime configuration and controllers live under `org.telegram.messenger.tj`.
- TJ settings screens live under `org.telegram.ui` and use the `Tj` prefix.
- User-visible base strings belong in `TMessagesProj/src/main/res/values/strings.xml`; Hebrew translations belong in `values-he/strings.xml`. Keep both in sync for every new TJ string.

## Privacy and behavior invariants

- Ghost mode must remain user-controlled. A short press on its drawer entry toggles it; a long press opens the complete privacy and Ghost settings screen.
- Normal viewing while read receipts are hidden must not silently bypass Ghost mode. Explicit actions such as “mark as viewed” or “mark as read up to here” may allow only the intended request.
- Locally preserved deleted, edited, protected, or ephemeral content must remain in app-private storage unless the user explicitly exports it.
- Secret chats remain protected by Android secure-window behavior. Do not weaken screenshot protection or screenshot notifications without an explicit product decision.
- Local Premium changes only client-side presentation and checks; never describe it as granting server-side Premium access.
- Crash reporting and external synchronization must be opt-in. Never add a default synchronization endpoint, access token, or user identifier.

## Implementation guidelines

- Integrate changes into existing Telegram flows instead of duplicating network, media, database, or lifecycle systems.
- Use `TjConfig` for persisted TJ preferences and `TjLocale` where runtime locale fallback is required.
- Keep account-scoped state account-scoped. When UI affects every active account, notify each active account explicitly.
- Treat message archive and media code as sensitive: preserve dialog IDs, message IDs, grouped-message ordering, captions, entities, spoilers, and TTL semantics.
- Do not perform blocking database or network work on the UI thread unless the existing API is intentionally synchronous and the operation is bounded.
- Avoid broad rewrites of upstream files. Keep hooks small and clearly attributable to TJ behavior.

## Verification

- Compile Java changes with:
  `GRADLE_USER_HOME=/tmp/tj-gradle-cache ./gradlew :TMessagesProj_App:compileAfatDebugJavaWithJavac`
- Build the AFAT debug APK with:
  `GRADLE_USER_HOME=/tmp/tj-gradle-cache ./gradlew :TMessagesProj_App:assembleAfatDebug`
- Before handoff, run `git diff --check`, parse changed XML resources, and inspect the final APK path and checksum.
- A successful compile proves source compatibility, not runtime correctness. State clearly which flows were not tested on a device.

## Git safety

- Preserve unrelated user changes in a dirty worktree.
- Do not commit, merge, rebase, push, or change the target branch unless the user explicitly requests that Git operation.
- Do not add local SDK paths, signing secrets, tokens, credentials, APKs, or build outputs to Git.
