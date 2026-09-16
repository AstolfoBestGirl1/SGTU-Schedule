import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// ---- Version from version.properties ----
val versionProps = Properties().apply {
    val f = rootProject.file("version.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
val vMajor = versionProps.getProperty("VERSION_MAJOR", "1").toInt()
val vMinor = versionProps.getProperty("VERSION_MINOR", "0").toInt()
val vPatch = versionProps.getProperty("VERSION_PATCH", "0").toInt()
val appVersionName = "$vMajor.$vMinor.$vPatch"
val appVersionCode = vMajor * 10000 + vMinor * 100 + vPatch
// ------------------------------------------

android {
    namespace = "ru.abg.sstuschedule"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "ru.abg.sstuschedule"
        minSdk = 30
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    useLibrary("wear-sdk")

    buildFeatures {
        compose = true
        buildConfig = false
    }

    // Wear OS работает только на ARM — x86_64 не нужен
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "/META-INF/*.kotlin_module",
                "/META-INF/versions/**",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json"
            )
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.protolayout)
    implementation(libs.androidx.protolayout.material3)
    implementation(libs.androidx.tiles)
    implementation(libs.androidx.tiles.tooling.preview)
    implementation(libs.androidx.watchface.complications.data.source.ktx)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.guava)
    implementation(libs.play.services.wearable)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.tiles.renderer)
    debugImplementation(libs.androidx.tiles.tooling)

    implementation(libs.gson)
    implementation(libs.androidx.work.runtime.ktx)
}