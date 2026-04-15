import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

val appVersionName = "0.1.9"
val appVersionCode = 21

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

val hasEnvReleaseSigning =
    listOf(
        "ANDROID_KEYSTORE_PATH",
        "ANDROID_KEYSTORE_PASSWORD",
        "ANDROID_KEY_ALIAS",
        "ANDROID_KEY_PASSWORD",
    ).all { !System.getenv(it).isNullOrBlank() }

val hasLocalReleaseSigning =
    keystorePropertiesFile.exists() &&
        listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
            .all { !keystoreProperties.getProperty(it).isNullOrBlank() }

android {
    namespace = "com.davideagostini.summ"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.davideagostini.summ"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        ndk {
            debugSymbolLevel = "FULL"
        }
    }

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
        buildConfig = true
    }
}

val renameReleaseApk by tasks.registering {
    dependsOn("assembleRelease")

    doLast {
        val releaseDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val sourceApk = releaseDir.resolve("app-release.apk")
        val targetApk = releaseDir.resolve("dunio-$appVersionName.apk")

        if (sourceApk.exists()) {
            if (targetApk.exists()) {
                targetApk.delete()
            }
            sourceApk.renameTo(targetApk)
        }
    }
}

val renameReleaseBundle by tasks.registering {
    dependsOn("bundleRelease")

    doLast {
        val releaseDir = layout.buildDirectory.dir("outputs/bundle/release").get().asFile
        val sourceBundle = releaseDir.resolve("app-release.aab")
        val targetBundle = releaseDir.resolve("dunio-$appVersionName.aab")

        if (sourceBundle.exists()) {
            if (targetBundle.exists()) {
                targetBundle.delete()
            }
            sourceBundle.renameTo(targetBundle)
        }
    }
}

val renameReleaseMapping by tasks.registering {
    doLast {
        val mappingDir = layout.buildDirectory.dir("outputs/mapping/release").get().asFile
        val sourceMapping = mappingDir.resolve("mapping.txt")
        val targetMapping = mappingDir.resolve("dunio-$appVersionName-mapping.txt")

        if (sourceMapping.exists()) {
            if (targetMapping.exists()) {
                targetMapping.delete()
            }
            sourceMapping.copyTo(targetMapping)
        }
    }
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy(renameReleaseApk, renameReleaseMapping)
}

tasks.matching { it.name == "bundleRelease" }.configureEach {
    finalizedBy(renameReleaseBundle, renameReleaseMapping)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.coil.compose)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    val firebaseBom = platform(libs.firebase.bom)
    implementation(firebaseBom)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}
