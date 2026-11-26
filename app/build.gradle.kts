plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.testcountdown.widget"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.testcountdown.widget"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// No extra runtime libraries: the app only uses the Android framework.

dependencies {
    testImplementation("junit:junit:4.13.2")
}
