# Overview

This repository contains the open source code for [Keyri](https://keyri.com) Android SDK.

![Lint](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/workflows/Lint/badge.svg)
![Instrumentation Tests](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/workflows/Instrumented%20Tests/badge.svg)
![Unit Tests](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/workflows/Unit%20Tests/badge.svg)
![Release](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/workflows/Release/badge.svg)

[![GitHub release](https://img.shields.io/github/release/Keyri-Co/keyri-android-whitelabel-sdk.svg?maxAge=10)](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/releases)

![demo](images/demo.gif)

## Contents

* [Requirements](#requirements)
* [Demo](#demo)
* [Integration](#integration)
* [Usage](#usage)
* [License](#license)

## Requirements

* Android API level 23 or higher
* AndroidX compatibility
* Kotlin coroutines compatibility

Note: Your app does not have to be written in kotlin to integrate this SDK, but must be able to
depend on kotlin functionality.

## Demo

This repository contains a demonstration app for the Keyri SDK product. To build and run the demo
app, follow the instructions in the [Usage](#usage).

## Integration

See the [integration documentation](https://docs.keyri.com/android)
in the Keyri Docs.

### Dependencies

* Add the JitPack repository to your root build.gradle file:

```groovy
allprojects {
    repositories {
        // ...
        maven { url "https://jitpack.io" }
    }
}
```

* Add SDK dependency to your build.gradle file and sync project:

```groovy
dependencies {
    // ...
    implementation "com.github.Keyri-Co:keyri-android-whitelabel-sdk:1.0.10"
}
```

### Provisioning Keyri config parameters

Supply these parameters to your app:

* Service Domain
* RP Public Key

For example:

```groovy
android {
    defaultConfig {
        // ...
        buildConfigField "String", "SERVICE_DOMAIN", "\"misc.keyri.com\""
        buildConfigField "String", "APP_KEY", "\"IT7VrTQ0r4InzsvCNJpRCRpi1qzfgpaj\""
    }
    // ...
}
```

And then use them to initialize the SDK:

```kotlin
val keyriSdk = KeyriSdk(
    requireContext(),
    appKey = BuildConfig.APP_KEY,
    serviceDomain = BuildConfig.SERVICE_DOMAIN
) 
```

Or with koin DI:

```kotlin
val keyriModule = module {
    single {
        KeyriSdk(
            get(),
            BuildConfig.APP_KEY,
            BuildConfig.SERVICE_DOMAIN
        )
    }
}
```

## Usage

Note that the SDK object must not be destroyed between calling **initiateSession()** and retrieving
the result of **approveSession()**.

### Option 1: Use **easyKeyriAuth()** method to delegate authentication to SDK (ActivityResult API)

```kotlin
private val easyKeyriAuthLauncher = registerForActivityResult(ShowEasyKeyriAuth()) { isSuccess ->
    // Handle authentication result
    // ...
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ...
    binding.bEasyKeyriAuth.setOnClickListener {
        keyriSdk.easyKeyriAuth(
            easyKeyriAuthLauncher,
            "public-user-id",
            "secure custom",
            "public custom"
        )
    }
}
```

You could check full code
in [AuthWithScannerActivity](app/src/main/java/com/keyri/ui/main/MainActivity.kt).

### Option 2: Build a custom authentication/authorization UI/UX

Alternatively, if you want to provide a custom authentication/authorization UI/UX, use the following
methods:

* **initiateSession()** - Call it after retrieving the sessionId from QR-code or deep link. It will
  provide Session object or Risk Analytics information (needed to show confirmation screen).
* **approveSession()** - Call this function to finish user authentication.

```kotlin
val session = keyriSdk.initiateSession(sessionId)

// Show confirmation screen and if positive do next:

keyriSdk.approveSession(
    publicUserId,
    username,
    key,
    sessionId,
    publicCustom,
    secureCustom
)
```

### Deep Link Handling

To handle deep links (e.g., for QR login straight from the user's built-in camera app) you need to
define in your AndroidManifest.xml following intent-filter block:

```xml

<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />

    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <data android:host="www.keyri.co" android:scheme="https" />

</intent-filter>
```

This will handle all links with such scheme: [https://www.keyri.co/application?sessionId=324e23]. In
the activity where the processing of links is declared, you need to add handlers in the
**onNewIntent()** and **onCreate()** methods:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_auth)

    intent.data?.let(::processLink)

    initializeUi()
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    processLink(intent.data)
}

private fun processLink(uri: Uri?) {
    uri?.getQueryParameters("sessionId")?.firstOrNull()?.let { sessionId ->
        viewModel.handleSessionId(sessionId, keyriSdk)
    } ?: Log.e("Keyri", "Failed to process link")
}
```

The last thing you need to do in order for your deep links to be processed is to create the
associations for each of the declared hosts for handling in JSON file as described
here: [https://developer.android.com/training/app-links/verify-site-associations].

## License

This library is available under paid and free licenses. See the [LICENSE](LICENSE.txt) file for the
full license text.

* Details of licensing (pricing, etc) are available
  at [https://keyri.com/pricing](https://keyri.com/pricing), or you can contact us
  at [Sales@keyri.com](mailto:Sales@keyri.com).

### Details

What's allowed under the license:

* Free use for any app under the Keyri Developer plan.
* Any modifications as needed to work in your app

What's not allowed under the license:

* Redistribution under a different license
* Removing attribution
* Modifying logos
* Indemnification: using this free software is ‘at your own risk’, so you can’t sue Keyri, Inc. for
  problems caused by this library
