import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val wearVersionName = "0.1.0-wear"
val wearVersionCode = 3601001

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
    namespace = "com.davideagostini.summ.wearapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.davideagostini.summ"
        minSdk = 30
        targetSdk = 36
        versionCode = wearVersionCode
        versionName = wearVersionName
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
            isMinifyEnabled = false
            if (hasEnvReleaseSigning || hasLocalReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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

tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy(renameWearReleaseApk)
}

tasks.matching { it.name == "bundleRelease" }.configureEach {
    finalizedBy(renameWearReleaseBundle)
}

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
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.wearable)

    debugImplementation(libs.compose.ui.tooling)
}
