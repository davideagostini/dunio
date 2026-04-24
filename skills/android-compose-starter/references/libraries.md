# Android Library Selection Guide

## How to find the latest stable version

| Source | What it covers |
|---|---|
| [Google Maven](https://maven.google.com/web/index.html) | All `androidx.*`, `com.google.*`, `com.android.*` |
| [Maven Central](https://search.maven.org) | All other libraries (Retrofit, OkHttp, Coil, etc.) |
| [Android Release Notes](https://developer.android.com/jetpack/androidx/releases) | Structured release history per library |
| [Compose BOM mapping](https://developer.android.com/jetpack/compose/bom/bom-mapping) | Which Compose version the BOM resolves to |

> Always pick the **stable** release. Avoid alpha/beta unless you need a specific new API.

---

## Core — always include

### Compose BOM + UI

The BOM (Bill of Materials) manages consistent versions across all `androidx.compose.*` libraries. Import it once; individual Compose libraries need no `version.ref`.

The version numbers below are example snapshots. Treat them as placeholders and refresh them against the latest stable releases before use.

```toml
# libs.versions.toml
[versions]
compose-bom = "2025.05.00"   # check https://developer.android.com/jetpack/compose/bom/bom-mapping

[libraries]
compose-bom                 = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui                  = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling-preview  = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3           = { group = "androidx.compose.material3", name = "material3" }
activity-compose            = { group = "androidx.activity", name = "activity-compose", version = "1.10.1" }
```

```kotlin
// build.gradle.kts
val composeBom = platform(libs.compose.bom)
implementation(composeBom)
implementation(libs.compose.ui)
implementation(libs.compose.ui.tooling.preview)
implementation(libs.compose.material3)
implementation(libs.activity.compose)
debugImplementation(libs.compose.ui.tooling)
```

### Lifecycle + ViewModel

```toml
[versions]
lifecycle = "2.9.1"

[libraries]
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose   = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-runtime-ktx       = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
```

`lifecycle-runtime-compose` provides `collectAsStateWithLifecycle()` — always use this over `collectAsState()` to stop collection when the screen is in the background.

### Kotlin Coroutines

```toml
[versions]
coroutines = "1.10.2"

[libraries]
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
```

---

## Dependency Injection — Hilt

The standard DI solution for Android. Integrates with ViewModel, Navigation, and WorkManager.

```toml
[versions]
hilt = "2.56.2"
hilt-navigation-compose = "1.2.0"
ksp = "2.1.0-1.0.29"   # must match kotlin version prefix

[libraries]
hilt-android            = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler           = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp  = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

```kotlin
// build.gradle.kts
implementation(libs.hilt.android)
implementation(libs.hilt.navigation.compose)
ksp(libs.hilt.compiler)   // use ksp, not kapt
```

> Use **KSP** (not KAPT) for Hilt — faster build times and fully supported since Hilt 2.48.

---

## Navigation

```toml
[versions]
navigation = "2.9.0"

[libraries]
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
```

Provides `NavHost`, `NavController`, `composable()`, `hiltViewModel()` integration.

---

## Networking — Retrofit + OkHttp + Moshi

```toml
[versions]
retrofit = "2.11.0"
okhttp   = "4.12.0"
moshi    = "1.15.2"

[libraries]
retrofit         = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-moshi   = { group = "com.squareup.retrofit2", name = "converter-moshi", version.ref = "retrofit" }
okhttp-logging   = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
moshi-kotlin     = { group = "com.squareup.moshi", name = "moshi-kotlin", version.ref = "moshi" }
moshi-codegen    = { group = "com.squareup.moshi", name = "moshi-kotlin-codegen", version.ref = "moshi" }
```

```kotlin
implementation(libs.retrofit)
implementation(libs.retrofit.moshi)
implementation(libs.okhttp.logging)
implementation(libs.moshi.kotlin)
ksp(libs.moshi.codegen)
```

**Why Moshi over Gson?** Moshi is null-safe, supports Kotlin data classes without reflection hacks, and has first-class KSP codegen.

If you prefer **Kotlinx Serialization**:
```toml
[versions]
kotlinx-serialization = "1.8.1"
retrofit-kotlinx      = "1.0.0"

[libraries]
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
retrofit-kotlinx-serialization = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version.ref = "retrofit-kotlinx" }
```

---

## Persistence

### Room — relational local DB

```toml
[versions]
room = "2.7.1"

[libraries]
room-runtime  = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx      = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```

```kotlin
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)
```

Use Room when you need SQL queries, complex joins, or offline-first caching.

### DataStore Preferences — key-value store

```toml
[versions]
datastore = "1.1.4"

[libraries]
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
```

Use DataStore instead of `SharedPreferences` for all user settings and preferences.

---

## Image loading — Coil

```toml
[versions]
coil = "2.7.0"

[libraries]
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
```

```kotlin
// Usage
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .crossfade(true)
        .build(),
    contentDescription = "Description",
    contentScale = ContentScale.Crop,
)
```

For Coil 3.x (supports Compose Multiplatform):
```toml
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version = "3.1.0" }
coil-network = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version = "3.1.0" }
```

---

## Testing

```toml
[versions]
junit        = "4.13.2"
junit-ext    = "1.2.1"
espresso     = "3.6.1"
# coroutines version already declared above

[libraries]
junit               = { group = "junit", name = "junit", version.ref = "junit" }
junit-ext           = { group = "androidx.test.ext", name = "junit", version.ref = "junit-ext" }
espresso            = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
coroutines-test     = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
turbine             = { group = "app.cash.turbine", name = "turbine", version = "1.2.0" }
compose-ui-test     = { group = "androidx.compose.ui", name = "ui-test-junit4" }  # version from BOM
```

```kotlin
testImplementation(libs.junit)
testImplementation(libs.coroutines.test)
testImplementation(libs.turbine)              // Flow testing
androidTestImplementation(libs.junit.ext)
androidTestImplementation(libs.espresso)
androidTestImplementation(libs.compose.ui.test)
```

**Turbine** is the standard for testing `StateFlow` and `Flow` in ViewModels:
```kotlin
@Test
fun `search updates state`() = runTest {
    val viewModel = HomeViewModel(fakeRepository)
    viewModel.uiState.test {
        awaitItem()  // initial state
        viewModel.handleEvent(HomeEvent.Search("kotlin"))
        val state = awaitItem()
        assertEquals("kotlin", state.searchQuery)
    }
}
```

---

## Optional — add only when needed

### Firebase

```toml
[versions]
firebase-bom = "34.5.0"
google-services = "4.4.4"
crashlytics-gradle = "3.0.6"

[libraries]
firebase-bom       = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }  # version from BOM
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics-ktx" }

[plugins]
google-services      = { id = "com.google.gms.google-services", version.ref = "google-services" }
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "crashlytics-gradle" }
```

### WorkManager — background tasks

```toml
work-runtime = { group = "androidx.work", name = "work-runtime-ktx", version = "2.10.1" }
hilt-work    = { group = "androidx.hilt", name = "hilt-work", version = "1.2.0" }
```

### CameraX — camera + barcode

```toml
[versions]
camerax = "1.5.0"
mlkit-barcode = "17.3.0"

[libraries]
camerax-camera2  = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
camerax-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
camerax-view     = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }
mlkit-barcode    = { group = "com.google.mlkit", name = "barcode-scanning", version.ref = "mlkit-barcode" }
```

### Paging 3

```toml
paging-compose = { group = "androidx.paging", name = "paging-compose", version = "3.3.6" }
```

### Accompanist (use sparingly — prefer Compose built-ins first)

```toml
accompanist-permissions = { group = "com.google.accompanist", name = "accompanist-permissions", version = "0.37.0" }
```

---

## Dependency conflict resolution

When Gradle reports version conflicts:

```bash
./gradlew dependencies --configuration debugRuntimeClasspath | grep <library>
```

Force a specific version:
```kotlin
// build.gradle.kts
configurations.all {
    resolutionStrategy {
        force("com.squareup.okhttp3:okhttp:4.12.0")
    }
}
```

Or use dependency constraints:
```kotlin
dependencies {
    constraints {
        implementation("com.squareup.okhttp3:okhttp:4.12.0") {
            because("Retrofit pulls in older version")
        }
    }
}
```
