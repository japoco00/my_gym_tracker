plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.jacopo.mygymtracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jacopo.mygymtracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    // MigrationTestHelper reads the exported schemas as assets. Robolectric only sees the
    // debug variant's merged assets, so they go there (debug APK only, never release).
    sourceSets.getByName("debug").assets.directories.add("$projectDir/schemas")
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Robolectric needs this on SDK 36+: https://github.com/robolectric/robolectric/issues/11434
        unitTests.all { it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED") }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}


dependencies {
    // Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose & Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation (type-safe routes are @Serializable)
    implementation(libs.androidx.navigation.compose)

    // Room Persistence
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Settings
    implementation(libs.androidx.datastore.preferences)

    // Backup file (JSON)
    implementation(libs.kotlinx.serialization.json)

    // Charts
    implementation(libs.vico.compose.m3)

    // Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.room.testing)
}