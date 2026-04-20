/**
 * Gradle build configuration for the Wear OS quick-entry module.
 *
 * This module produces a standalone Wear OS APK (applicationId: com.davideagostini.summ)
 * that communicates with the companion phone app via the Wear Data Layer.
 *
 * Key sections:
 * - **Signing**: supports both CI environment variables and local keystore.properties.
 * - **Release build**: enables R8 minification and resource shrinking.
 * - **Post-build tasks**: rename APK, AAB and mapping files to include the version name.
 * - **Dependencies**: Jetpack Compose, Wear Compose Material 3, Wear Navigation,
 *   Play Services Wearable, and Lifecycle components.
 */
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/** Human-readable version string displayed to the user and embedded in output filenames. */
val wearVersionName = "0.1.7-wear"

/** Numeric version code used by the Play Store for update ordering. */
val wearVersionCode = 3601008

/**
 * Local keystore configuration loaded from keystore.properties in the project root.
 * Used for release signing when CI environment variables are not available.
 */
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

/**
 * True when all four signing-related environment variables are set (CI mode).
 * Environment variables take precedence over the local keystore.properties file.
 */
val hasEnvReleaseSigning =
    listOf(
        "ANDROID_KEYSTORE_PATH",
        "ANDROID_KEYSTORE_PASSWORD",
        "ANDROID_KEY_ALIAS",
        "ANDROID_KEY_PASSWORD",
    ).all { !System.getenv(it).isNullOrBlank() }

/**
 * True when the local keystore.properties file exists and contains all
 * four required signing properties. Used as a fallback when CI variables
 * are not set.
 */
val hasLocalReleaseSigning =
    keystorePropertiesFile.exists() &&
        listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
            .all { !keystoreProperties.getProperty(it).isNullOrBlank() }

android {
    namespace = "com.davideagostini.summ.wearapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.davideagostini.summ"
        minSdk = 30
        targetSdk = 36
        versionCode = wearVersionCode
        versionName = wearVersionName
    }

    /**
     * Configures the release signing key. Supports two sources:
     * 1. Environment variables (preferred for CI builds).
     * 2. Local keystore.properties file (for developer builds).
     * If neither is available, the release build will use the debug key.
     */
    signingConfigs {
        if (hasEnvReleaseSigning || hasLocalReleaseSigning) {
            create("release") {
                if (hasEnvReleaseSigning) {
                    storeFile = file(System.getenv("ANDROID_KEYSTORE_PATH"))
                    storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                    keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
                } else {
                    storeFile = file(keystoreProperties.getProperty("storeFile"))
                    storePassword = keystoreProperties.getProperty("storePassword")
                    keyAlias = keystoreProperties.getProperty("keyAlias")
                    keyPassword = keystoreProperties.getProperty("keyPassword")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasEnvReleaseSigning || hasLocalReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

/**
 * Renames the release APK to include the version name.
 *
 * Output: `dunio-wear-{versionName}.apk`
 * Runs after `assembleRelease`.
 */
val renameWearReleaseApk by tasks.registering {
    dependsOn("assembleRelease")

    doLast {
        val releaseDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val sourceApk = releaseDir.resolve("wear-release.apk")
        val targetApk = releaseDir.resolve("dunio-wear-$wearVersionName.apk")

        if (sourceApk.exists()) {
            if (targetApk.exists()) {
                targetApk.delete()
            }
            sourceApk.renameTo(targetApk)
        }
    }
}

/**
 * Renames the release AAB (Android App Bundle) to include the version name.
 *
 * Output: `dunio-wear-{versionName}.aab`
 * Runs after `bundleRelease`.
 */
val renameWearReleaseBundle by tasks.registering {
    dependsOn("bundleRelease")

    doLast {
        val releaseDir = layout.buildDirectory.dir("outputs/bundle/release").get().asFile
        val sourceBundle = releaseDir.resolve("wear-release.aab")
        val targetBundle = releaseDir.resolve("dunio-wear-$wearVersionName.aab")

        if (sourceBundle.exists()) {
            if (targetBundle.exists()) {
                targetBundle.delete()
            }
            sourceBundle.renameTo(targetBundle)
        }
    }
}

/**
 * Copies the R8 mapping file and renames it to include the version name.
 *
 * Output: `dunio-wear-{versionName}-mapping.txt`
 * The original mapping.txt is preserved so Crashlytics can de-obfuscate stack traces.
 */
val renameWearReleaseMapping by tasks.registering {
    doLast {
        val mappingDir = layout.buildDirectory.dir("outputs/mapping/release").get().asFile
        val sourceMapping = mappingDir.resolve("mapping.txt")
        val targetMapping = mappingDir.resolve("dunio-wear-$wearVersionName-mapping.txt")

        if (sourceMapping.exists()) {
            if (targetMapping.exists()) {
                targetMapping.delete()
            }
            sourceMapping.copyTo(targetMapping)
        }
    }
}

/** Wires APK renaming and mapping copy to the assembleRelease task. */
tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy(renameWearReleaseApk, renameWearReleaseMapping)
}

/** Wires AAB renaming and mapping copy to the bundleRelease task. */
tasks.matching { it.name == "bundleRelease" }.configureEach {
    finalizedBy(renameWearReleaseBundle, renameWearReleaseMapping)
}

/**
 * Dependency declarations for the Wear quick-entry module.
 *
 * - Compose BOM ensures all Compose libraries use compatible versions.
 * - Wear Compose Material 3 provides the component library optimised for watches.
 * - Wear Navigation handles the swipe-dismissable back stack.
 * - Play Services Wearable provides the Data Layer API for phone communication.
 * - Lifecycle components power the ViewModel and state collection.
 */
dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.google.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.wearable)

    debugImplementation(libs.compose.ui.tooling)
}
