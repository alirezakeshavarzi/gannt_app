plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.ganttapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ganttapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        // Java 8+ language features
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        // **Enable desugaring** so java.time.* works back to API 24
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Make sure this matches your Compose BOM version
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.datastore:datastore-preferences:1.1.6")
    implementation(libs.androidx.datastore.preferences.core.android)
    // 👉 Pull in the desugaring library directly
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    // JSON parsing (if still needed)
    implementation("com.google.code.gson:gson:2.10.1")

    // AnyChart (if you decide to mix in the Android‑View‑based chart)
    implementation("com.github.AnyChart:AnyChart-Android:1.1.2")

    // Compose + Material3
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Icons (for FloatingActionButton, etc.)
    implementation("androidx.compose.material:material-icons-core:1.5.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
