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
- [ ] signed release APK installs locally and completes a cold start without crashing
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
- [ ] create/join household does not flash back to setup during the transition
- [ ] dashboard loads
- [ ] entries CRUD works
- [ ] assets CRUD works
- [ ] household currency can be changed from Settings
- [ ] app language can be changed from Settings
- [ ] app theme can be changed from Settings
- [ ] selected app language stays highlighted in the language list and the app reloads in the chosen locale
- [ ] selected app theme stays highlighted in the theme list and the app palette updates immediately
- [ ] light / dark / system theme preference persists across app relaunch
- [ ] currency search works and keeps the selected currency at the top
- [ ] core screens show the expected localized strings for supported languages
- [ ] dashboard, entries, assets, recurring, and widgets reflect the selected household currency
- [ ] recurring apply due works without duplicates
- [ ] recurring due items are auto-applied once per day on app startup
- [ ] month close read-only mode works
- [ ] member vs owner invite visibility works as expected
- [ ] widget refresh behavior is re-reviewed from the current simple baseline before the next release

## GitHub polish

- [ ] issue templates are present
- [ ] CI workflow is green
- [ ] release workflow uploads `APK`, `AAB`, and `mapping.txt`
- [ ] optional native symbol artifacts are checked if the workflow produced them
- [ ] first release notes drafted
- [ ] screenshots or demo media prepared
