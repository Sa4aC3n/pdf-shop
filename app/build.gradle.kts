import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.pdfcompress.CamScanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pdfcompress.CamScanner"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val localProperties = Properties().apply {
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { stream -> this.load(stream) }
        }
    }

    val releaseStoreFilePath = localProperties.getProperty("RELEASE_STORE_FILE")
        ?: System.getenv("RELEASE_STORE_FILE")
        ?: "release.keystore"
    val releaseKeystore = if (file(releaseStoreFilePath).isAbsolute) {
        file(releaseStoreFilePath)
    } else {
        rootProject.file(releaseStoreFilePath)
    }

    val releaseStorePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
        ?: System.getenv("RELEASE_STORE_PASSWORD")
        ?: System.getenv("KEYSTORE_PASSWORD")

    val releaseKeyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
        ?: System.getenv("RELEASE_KEY_ALIAS")
        ?: System.getenv("KEY_ALIAS")

    val releaseKeyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
        ?: System.getenv("RELEASE_KEY_PASSWORD")
        ?: System.getenv("KEY_PASSWORD")


    signingConfigs {
        create("release") {

                storeFile = rootProject.file("release.keystore")
                storePassword = releaseStorePassword ?: "camscanner2026"
                keyAlias = releaseKeyAlias ?: "camscanner"
                keyPassword = releaseKeyPassword ?: releaseStorePassword ?: "camscanner2026"






        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
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
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:files"))
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.pdfbox.android)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.room.runtime)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation("com.google.guava:guava:33.3.1-android")
    implementation(libs.play.services.ads)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
