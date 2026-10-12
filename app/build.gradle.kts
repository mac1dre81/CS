import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    // google-services and firebase-crashlytics Gradle plugins are declared in
    // settings.gradle.kts (apply = false) and applied conditionally below,
    // because the google-services plugin requires app/google-services.json,
    // which is never committed (see .gitignore). See FIREBASE_SETUP.md.
}

// Conditionally apply Firebase plugins. The google-services plugin FAILS the
// build if app/google-services.json is absent, so we gate on its existence.
// The Crashlytics SDK dependency (in deps below) is always present; without
// the config file it silently no-ops at runtime.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "com.credstow.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.credstow.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing: reads credentials from keystore.properties (git-ignored,
    // never committed). See keystore.properties.example for setup instructions.
    // The Play Store requires an unsigned-by-debug upload; Google Play App Signing
    // then manages your production signing key. Until you enroll, sign with your
    // own upload key created via keytool.
    signingConfigs {
        create("release") {
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) {
                val props = Properties()
                FileInputStream(propsFile).use { props.load(it) }
                storeFile = file(props.getProperty("MYAPP_STORE_FILE")!!)
                storePassword = props.getProperty("MYAPP_STORE_PASSWORD")!!
                keyAlias = props.getProperty("MYAPP_KEY_ALIAS")!!
                keyPassword = props.getProperty("MYAPP_KEY_PASSWORD")!!
            }
        }
    }

    buildTypes {
        release {
            // Keys are encrypted at rest; shrinking/obfuscation does not expose them.
            // R8 minify + resource shrinking reduces APK size and hardens the binary
            // by removing unused code and renaming internal symbols. The proguard
            // rules in proguard-rules.pro keep all reflection-dependent library
            // code (Room, OkHttp, AdMob, Play Billing, Compose) intact.
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles("proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE.txt",
                "/META-INF/LICENSE",
                "/META-INF/notice.txt",
                "/META-INF/NOTICE.txt",
                "/META-INF/NOTICE",
                "/META-INF/INDEX.LIST",
                "/META-INF/ASL2.0"
            )
        }
    }

    // Don't let lint block assembly during build verification; keep it reporting.
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    // Room: export schema JSON to app/schemas/ so migrations are version-controlled.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.exportSchema", "true")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // --- AndroidX / Foundation ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // --- Security: Android Keystore + AES-256-GCM master key ---
    implementation(libs.androidx.security.crypto)

    // --- Jetpack Compose (versions aligned via BOM) ---
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
    implementation(libs.compose.material.icons.extended)

    // --- Material Components: Material3 View-system themes (Theme.Material3.DayNight.NoActionBar)
    // and colorPrimary/colorOnPrimary attrs referenced by app themes.xml. ---
    implementation(libs.material)
    debugImplementation(libs.compose.ui.tooling)

    // --- Room (structured metadata; secret column encrypted) ---
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // --- Kotlin eco ---
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // --- Networking (used ONLY for user-initiated key test ping, never for storage) ---
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    coreLibraryDesugaring(libs.coreLibraryDesugaring)

    // --- Monetization: one-time Pro purchase ---
    implementation(libs.billing)

    // --- Crash reporting (Firebase Crashlytics) ------------------------------
    // Requires app/google-services.json (generated from the Firebase console,
    // MUST NOT be committed — see .gitignore). Without the JSON file the SDK
    // silently no-ops on startup; the build still succeeds.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.ktx)

    // --- Ads (Google AdMob) — banner shown to free users; suppressed when Pro ---
    implementation(libs.google.ads)

    // --- Biometric unlock (Pro feature) ---
    implementation(libs.biometric)

    // --- Tests (JVM unit tests) ---
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.okhttp)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.core)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    // Kept harmless; the kotlin {} compilerOptions above is the canonical target setting.
}
