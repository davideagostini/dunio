# AGENTS.md

## Scope

These instructions apply to everything inside:

- `/Users/davideagostini/Documents/networth-app/mobile-app`

## Mandatory Android workflow

Before creating a new Android feature or updating an existing one, read these local skills first:

- [android-compose-starter-skill/SKILL.md](/Users/davideagostini/Documents/networth-app/.claude/skills/android-compose-starter-skill/SKILL.md)

Consult the relevant reference files in:

- [/Users/davideagostini/Documents/networth-app/.claude/skills/android-compose-starter-skill/references](\/Users\/davideagostini\/Documents\/networth-app\/.claude\/skills\/android-compose-starter-skill\/references)

Do this before making architectural, UI, navigation, state-management, or dependency decisions.

## Expected behavior

When working in this folder:

1. Inspect the existing feature first.
2. Reuse validated UI patterns already present in the app.
3. Keep screen files focused on orchestration, state, and navigation.
4. Move reusable or presentational UI into each feature's `components/` folder.
5. Avoid introducing new patterns when an approved one already exists.
6. Keep the app aligned with the current mobile-first design and Firebase household model.
7. Verify Android changes with:
   - `./gradlew assembleDebug`

## Notes

- Prefer consistency with the existing `Entries`, `Assets`, `Categories`, `Dashboard`, and `Settings` screens.
- For auth and Firebase work, avoid regressions in Google sign-in, session restore, and household gating.
