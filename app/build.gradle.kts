plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    defaultConfig {
        applicationId = "com.keyri"
        minSdk = 23
        targetSdk = 32
        compileSdk = 31

        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "APP_KEY", "\"IT7VrTQ0r4InzsvCNJpRCRpi1qzfgpaj\"")
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
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":keyrisdk"))

    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.activity:activity-ktx:1.4.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.google.android.material:material:1.6.0")
}
