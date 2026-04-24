# Android Project Setup Reference

## 1. Create the project in Android Studio

1. Open Android Studio → **New Project** → **Empty Activity**
2. Configure:
   - **Name**: your app name
   - **Package**: `com.<company>.<app>` (reverse DNS, all lowercase)
   - **Language**: Kotlin
   - **Minimum SDK**: API 26 unless product requirements justify lower support
   - **Build configuration language**: **Kotlin DSL** (`build.gradle.kts`)
3. Click **Finish**. Do NOT run the app yet.

---

## 2. Version catalog — `gradle/libs.versions.toml`

Android Studio may generate a partial catalog. Replace / complete it with this structure.

The version numbers below are example snapshots, not fixed recommendations. Refresh them against the current stable releases before using them in a real project.

```toml
[versions]
# Core
agp = "9.1.0"                       # Android Gradle Plugin
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"               # must match kotlin version prefix

# Compose
compose-bom = "2025.05.00"          # BOM manages all compose/* versions
compose-compiler = "2.1.0"          # matches Kotlin version

# Architecture
lifecycle = "2.9.1"
navigation = "2.9.0"
hilt = "2.56.2"
hilt-navigation-compose = "1.2.0"

# Async
coroutines = "1.10.2"

# Networking
retrofit = "2.11.0"
okhttp = "4.12.0"
moshi = "1.15.2"

# Persistence
room = "2.7.1"
datastore = "1.1.4"

# Image
coil = "2.7.0"

# Testing
junit = "4.13.2"
junit-ext = "1.2.1"
espresso = "3.6.1"

[libraries]
# Compose (versions managed by BOM — do not add version.ref here)
compose-bom                 = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui                  = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics         = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling-preview  = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-tooling          = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-test-manifest    = { group = "androidx.compose.ui", name = "ui-test-manifest" }
compose-ui-test-junit4      = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-material3           = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons      = { group = "androidx.compose.material", name = "material-icons-extended" }
activity-compose            = { group = "androidx.activity", name = "activity-compose", version = "1.10.1" }

# Lifecycle / ViewModel
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose   = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-runtime-ktx       = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }

# Navigation
navigation-compose          = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Hilt
hilt-android                = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler               = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose     = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

# Coroutines
coroutines-android          = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

# Networking
retrofit                    = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-moshi              = { group = "com.squareup.retrofit2", name = "converter-moshi", version.ref = "retrofit" }
okhttp-logging              = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
moshi-kotlin                = { group = "com.squareup.moshi", name = "moshi-kotlin", version.ref = "moshi" }
moshi-codegen               = { group = "com.squareup.moshi", name = "moshi-kotlin-codegen", version.ref = "moshi" }

# Persistence
room-runtime                = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx                    = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler               = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
datastore-preferences       = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Image
coil-compose                = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# Testing
junit                       = { group = "junit", name = "junit", version.ref = "junit" }
junit-ext                   = { group = "androidx.test.ext", name = "junit", version.ref = "junit-ext" }
espresso                    = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
coroutines-test             = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
turbine                     = { group = "app.cash.turbine", name = "turbine", version = "1.2.0" }

[plugins]
android-application         = { id = "com.android.application", version.ref = "agp" }
android-library             = { id = "com.android.library", version.ref = "agp" }
kotlin-android              = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler            = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "compose-compiler" }
ksp                         = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt                        = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

> **Finding latest versions**: check [Google Maven](https://maven.google.com/web/index.html) for `androidx.*` and `com.google.*`; check [Maven Central](https://search.maven.org) for everything else. Always pick the stable release, not alpha/beta, unless you have a specific reason.

---

## 3. Root `build.gradle.kts`

```kotlin
// Top-level build file — do NOT add dependencies here
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

---

## 4. App `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.<company>.<app>"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.<company>.<app>"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM — import first, all compose/* libraries below get version from it
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.runtime.ktx)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // Networking (add only if needed)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)

    // Persistence (add only what you need)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // Image
    implementation(libs.coil.compose)

    // Debug
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
```

---

## 5. `gradle.properties`

```properties
# Build performance
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# Android
android.useAndroidX=true
android.enableJetifier=false    # false unless a library still uses Support Library

# Kotlin
kotlin.code.style=official

# Compose
android.defaults.buildfeatures.compose=false  # enable per-module only
```

> **AGP 9 breaking changes to be aware of:**
> - `android.nonFinalResIds=false` is removed — non-final R IDs are always enforced
> - `android.namespace` is mandatory in every module's `build.gradle.kts`
> - `BuildConfig` generation is **disabled by default** — opt in with `buildFeatures { buildConfig = true }` if you use `BuildConfig.BASE_URL` or similar
> - Several deprecated DSL methods were removed — use property syntax (`isMinifyEnabled = true`, not `minifyEnabled true`)

---

## 6. Release configuration

### Signing
Create `keystore.properties` (never commit to git):
```properties
storeFile=../release.jks
storePassword=<password>
keyAlias=<alias>
keyPassword=<password>
```

Reference in `build.gradle.kts`:
```kotlin
val keystoreProperties = Properties().apply {
    load(rootProject.file("keystore.properties").inputStream())
}

android {
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### ProGuard rules for Compose
Add to `proguard-rules.pro`:
```proguard
# Compose stability annotations
-keep @androidx.compose.runtime.Stable class **
-keep @androidx.compose.runtime.Immutable class **

# Moshi (if used)
-keep class **JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# Retrofit (if used)
-keepattributes Signature
-keepattributes Exceptions
```

---

## 7. Verify the build

```bash
./gradlew assembleDebug       # should succeed with no warnings
./gradlew assembleRelease     # verify minification works
./gradlew dependencies        # inspect the dependency tree
./gradlew dependencyUpdates   # check for outdated versions (requires ben-manes plugin)
```
