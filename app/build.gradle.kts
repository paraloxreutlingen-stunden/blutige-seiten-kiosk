plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.paralox.kiosk.blutigeseiten"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.paralox.kiosk.blutigeseiten"
        minSdk = 26
        targetSdk = 36
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.activity:activity-ktx:1.9.3")

    // QR-Scan laeuft komplett in der eigenen App (nicht ueber eine separate Google-Activity),
    // damit es auch waehrend der Bildschirmfixierung (Screen Pinning) zuverlaessig funktioniert.
    // Screen Pinning erlaubt es gepinnten Apps naemlich nicht zuverlaessig, Activities aus
    // einem fremden Package (z.B. Google Play Services) zu oeffnen.
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
}
