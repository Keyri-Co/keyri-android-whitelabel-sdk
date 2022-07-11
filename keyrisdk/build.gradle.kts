plugins {
    id("com.android.library")
    id("maven-publish")
    id("kotlin-android")
    id("kotlin-parcelize")
}

android {
    defaultConfig {
        minSdk = 23
        targetSdk = 32
        compileSdk = 32

        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"https://prod.api.keyri.com\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }

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

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    viewBinding {
        isEnabled = true
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.8.0")

    // UI
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.activity:activity-ktx:1.5.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.2")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Camera and analytics
    implementation("com.google.mlkit:barcode-scanning:17.0.2")
    implementation("androidx.camera:camera-core:1.1.0")
    implementation("androidx.camera:camera-camera2:1.1.0")
    implementation("androidx.camera:camera-lifecycle:1.1.0")
    implementation("androidx.camera:camera-view:1.1.0")

    // Tink crypto
    implementation("com.google.crypto.tink:tink-android:1.6.1")

    // Unit testing
    testImplementation("junit:junit:4.13.2")

    // Instrumented testing
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.test:core-ktx:1.4.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.3")
}

afterEvaluate {
    publishing {
        publications {
            register("release", MavenPublication::class) {
                from(components["release"])

                groupId = "com.keyrico.keyrisdk"
                artifactId = "keyrisdk"
            }
        }
    }
}
