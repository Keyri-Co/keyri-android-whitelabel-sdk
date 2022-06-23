plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    defaultConfig {
        applicationId = "com.keyri"
        minSdk = 23
        targetSdk = 32
        compileSdk = 32

        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        named("debug").configure {
            isMinifyEnabled = false
            isDebuggable = true
        }

        named("release").configure {
            isMinifyEnabled = false
            isDebuggable = false
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

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    viewBinding {
        isEnabled = true
    }
}

dependencies {
    implementation(project(":keyrisdk"))

    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.activity:activity-ktx:1.4.0")
    implementation("com.google.android.material:material:1.6.1")
}
