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
5. Do not let screen files grow into long monolithic `.kt` files; extract UI sections early into `components/`.
6. Avoid introducing new patterns when an approved one already exists.
7. Keep the app aligned with the current mobile-first design and Firebase household model.
8. If the user asks to align one screen or flow to another existing screen, replicate the same structural pattern and navigation behavior, not just a visually similar approximation.
9. When mirroring an existing implementation, do not improvise an alternative container pattern (for example sheet-like fullscreen instead of a true dedicated screen) unless the user explicitly asks for it.
10. Verify Android changes with:
   - `./gradlew assembleDebug`
11. All code you add or change must be commented clearly. Document file responsibility and every non-trivial block of logic with explicit comments; do not leave produced code undocumented.

## Notes

- Prefer consistency with the existing `Entries`, `Assets`, `Categories`, `Dashboard`, and `Settings` screens.
- For auth and Firebase work, avoid regressions in Google sign-in, session restore, and household gating.
