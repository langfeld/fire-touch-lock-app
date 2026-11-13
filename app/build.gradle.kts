plugins {
    id("com.android.application")
}

android {
    namespace = "org.langfeld.firetouchlock"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.langfeld.firetouchlock"
        // Fire OS 7 (Android 9) und neuer
        minSdk = 28
        targetSdk = 35
        // Die GitHub Action setzt Build-Nummer und Version (.github/workflows/android.yml).
        versionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("APP_VERSION")?.takeIf { it.isNotBlank() } ?: "0.1.0"
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
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
