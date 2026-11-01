plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "dev.deva.devphone"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.deva.devphone"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The phone is arm64; shipping one ABI keeps the APK small.
        ndk { abiFilters += "arm64-v8a" }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            // Personal sideloaded app: sign release with the debug key so it installs directly.
            signingConfig = signingConfigs.getByName("debug")
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
    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
    }
    packaging {
        // Native libs stay uncompressed and page-aligned: the system maps them straight from the APK.
        jniLibs { useLegacyPackaging = false }
    }
    androidResources {
        // The app rings with the system ringtone; Linphone's bundled ringtones (~8 MB) are never played.
        ignoreAssetsPatterns += listOf("!rings", "!dont_wait_too_long.mkv")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Robolectric pokes java.io.FileDescriptor internals; JDK 17+ needs this opened explicitly.
                it.jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED", "--add-opens=java.base/java.lang=ALL-UNNAMED")
                it.maxHeapSize = "2g"
            }
        }
    }
}

roborazzi {
    outputDir.set(file("screenshots"))
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.telecom)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.linphone)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.material.components) // material-color-utilities, for Material You palettes in tests
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
