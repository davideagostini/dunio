# Android Compose Readiness Checklist

## Project setup

- [ ] Project builds with `./gradlew assembleDebug`
- [ ] Version catalog is in place in `gradle/libs.versions.toml`
- [ ] No raw version strings remain in `build.gradle.kts`
- [ ] `gradle.properties` includes the required performance flags

## Architecture skeleton

- [ ] `@HiltAndroidApp` is applied in `App.kt`
- [ ] `@AndroidEntryPoint` is applied in `MainActivity`
- [ ] `BaseViewModel` exists with toast or snackbar flows
- [ ] `NavHost` and at least one route are wired

## First screen

- [ ] `<Name>State.kt` is immutable and has defaults
- [ ] `<Name>Event.kt` models UI interactions as a sealed class
- [ ] `<Name>ViewModel.kt` exposes a single `StateFlow`
- [ ] `<Name>Screen.kt` uses `collectAsStateWithLifecycle()`
- [ ] A pure content composable is extracted
- [ ] Route registration and navigation wiring are complete
- [ ] Success, error, and exception cases are all handled

## Code quality

- [ ] No `!!` operators remain
- [ ] No `runBlocking` runs on the main thread
- [ ] Business logic does not live in composables
- [ ] ViewModel instances are not passed to child composables

## Release

- [ ] `isMinifyEnabled = true` is set for release
- [ ] `isShrinkResources = true` is set for release
- [ ] Signing is configured and secrets are not committed
