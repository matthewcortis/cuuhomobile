plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.cuutro"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.cuutro"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.mlkit.text.recognition.common)
    implementation("io.github.track-asia:android-sdk-opengl:2.0.2")
    implementation("io.github.track-asia:android-plugin-localization-v9:2.0.1") {
        exclude(group = "io.github.track-asia", module = "android-sdk")
    }
    implementation("io.github.track-asia:android-plugin-annotation-v9:2.0.1") {
        exclude(group = "io.github.track-asia", module = "android-sdk")
    }
    implementation("io.github.track-asia:android-plugin-markerview-v9:2.0.1") {
        exclude(group = "io.github.track-asia", module = "android-sdk")
    }
    implementation("io.github.track-asia:android-sdk-turf:2.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
