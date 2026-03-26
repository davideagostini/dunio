# First Release Checklist

Use this checklist before publishing the first public GitHub release or Play Console build.

## Repository

- [ ] `README.md` reflects the current setup and feature set
- [ ] `LICENSE` is present and intentional
- [ ] `.gitignore` excludes Firebase config, local properties, and signing files
- [ ] `google-services.json` is not tracked
- [ ] no keystore or private credentials are tracked
- [ ] outdated internal-only docs or prompts are removed

## Android build

- [ ] `./gradlew assembleDebug` passes
- [ ] `./gradlew bundleRelease` passes with a local signing config
- [ ] version code and version name are correct
- [ ] app name, package name, and icon branding are consistent
- [ ] no placeholder UI text remains

## Firebase

- [ ] Firebase project created
- [ ] Android app registered with package `com.davideagostini.summ`
- [ ] Google sign-in enabled
- [ ] Firestore created
- [ ] Firebase files in `firebase/` reflect the intended public setup
- [ ] debug SHA-1 and SHA-256 added in Firebase

## Product checks

- [ ] sign in works
- [ ] create household works
- [ ] join household works with a real invite
- [ ] dashboard loads
- [ ] entries CRUD works
- [ ] assets CRUD works
- [ ] recurring apply due works without duplicates
- [ ] month close read-only mode works
- [ ] member vs owner invite visibility works as expected

## GitHub polish

- [ ] issue templates are present
- [ ] CI workflow is green
- [ ] first release notes drafted
- [ ] screenshots or demo media prepared
