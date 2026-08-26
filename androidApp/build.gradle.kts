plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "me.sandbad.medireminder.android"
    compileSdk = 37
    defaultConfig {
        applicationId = "me.sandbad.medireminder"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(projects.shared)

    api(libs.jetbrains.compose.foundation)
    api(libs.jetbrains.compose.material3)
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.ui)
    implementation(libs.jetbrains.compose.uiToolingPreview)
    implementation(libs.jetbrains.compose.componentsResources)

    implementation(libs.androidx.activity.compose)

    implementation(libs.koin.android)
    implementation(libs.koin.android.compat)
    implementation(libs.koin.androidx.workmanager)
    debugImplementation(libs.koin.androidx.compose)

    implementation(libs.multiplatform.settings)

}
