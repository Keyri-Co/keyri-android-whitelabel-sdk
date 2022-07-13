# Overview

This repository contains the open source code for [Keyri](https://keyri.com) Android SDK.

![Lint](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/workflows/Lint/badge.svg)
![Firebase Instrumentation Tests](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/workflows/Firebase%20Instrumentation%20Tests/badge.svg)
![Unit Tests](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/workflows/Unit%20Tests/badge.svg)
![Release](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/workflows/Release/badge.svg)

[![GitHub release](https://img.shields.io/github/release/Keyri-Co/keyri-android-whitelabel-sdk.svg?maxAge=10)](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/releases)

![demo](images/demo.gif)

## Contents

* [System Requirements](#system-requirements)
* [Demo](#demo)
* [Integration](#integration)
* [Option 1 - App Links](#option-1---app-links)
* [Option 2 - In-App Scanner](#option-2---in-app-scanner)
* [Interacting with the API](#interacting-with-the-api)
* [Session Object](#session-object)
* [Disclaimer](#Disclaimer)
* [License](#license)

The latest source code of the Keyri Android SDK can be found
here: [Releases](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/releases)

### **System Requirements**

* Android API level 23 or higher

* AndroidX compatibility

* Kotlin coroutines compatibility

Note: Your app does not have to be written in Kotlin to integrate this SDK, but it must be able to
depend on Kotlin functionality.

## Demo

This repository contains a demonstration app for the Keyri SDK product. To build and run the demo
app, follow the instructions below.

### **Integration**

* Add the JitPack repository to your root **build.gradle** file:

```kotlin
allprojects {
    repositories {
        // ...
        maven { url("https://jitpack.io") }
    }
}
```

* Add SDK dependency to your **build.gradle** file and sync project:

```kotlin
dependencies {
    // ...
    implementation("com.github.Keyri-Co:keyri-android-whitelabel-sdk:$latestKeyriVersion")
}
```

### **Option 1 - App Links**

To handle Android App Links (e.g., for QR login straight from the user's built-in camera app) you
need to define the following intent-filter block in your **AndroidManifest.xml**:

```xml

<application...>
    <!-- ...  -->
    <activity...>
        <!-- ...  -->
        <intent-filter android:autoVerify="true">
            <action android:name="android.intent.action.VIEW" />

            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />

            <data android:host="${domainName}" android:scheme="https" />
        </intent-filter>
    </activity>
</application>
```

This will handle all links with the following
scheme: `https://{yourCompany}.onekey.to?sessionId={sessionId}`

**Note:** Keyri will create your `https://{yourCompany}.onekey.to` page automatically once you
configure it in the [dashboard](https://app.keyri.com)

In the activity where the processing of links is declared, you need to add handlers in
the `onNewIntent()` and `onCreate()` methods:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    intent.data?.let(::process)
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    process(intent.data)
}

private fun process(uri: Uri?) {
    uri?.getQueryParameters("sessionId")?.firstOrNull()?.let { sessionId ->
        launch {
            try {
                val appKey = "App key here" // Get this value from the Keyri Developer Portal
                val publicUserId = "example@dot.com" // publicUserId is optional
                val payload = "Custom payload here"

                val keyri = Keyri() // Be sure to import the SDK at the top of the file

                keyri.initiateQrSession(appKey, sessionId, publicUserId)
                    .onSuccess { session ->
                        // You can optionally create a custom screen and pass the session ID there. We recommend this approach for large enterprises
                        keyri.initializeDefaultScreen(supportFragmentManager, session, payload)

                        // In a real world example you’d wait for user confirmation first
                        session.confirm(payload) // or session.deny(payload)
                    }

                // Process result
            } catch (e: Throwable) {
                Log.e("Keyri", "Authentication exception $e")
            }
        }
    } ?: Log.e("Keyri", "Failed to process link")
}
```

**Note:** Keyri will set up the required `/.well-known/assetlinks.json` JSON at
your `https://{yourSubdomain}.onekey.to` page as required by Android App Links handling. Details on
this mechanism are described
here: [Verify Android App Links](https://developer.android.com/training/app-links/verify-site-associations)

### **Option 2 - In-App Scanner**

Use `AuthWithScannerActivity` built-in functionality to delegate authentication to SDK. You can
use `ActivityResult API` or `onActivityResult`. Create Intent for `AuthWithScannerActivity` and
pass `App Key` with `AuthWithScannerActivity.APP_KEY`, optional public user ID
with `AuthWithScannerActivity.PUBLIC_USER_ID` and payload with `AuthWithScannerActivity.PAYLOAD` :

```kotlin
private val easyKeyriAuthLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
        val isSuccess = activityResult.resultCode == Activity.RESULT_OK
        // Handle authentication result
        // ...
    }

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ...
    binding.bAuthWithScanner.setOnClickListener {
        val intent = Intent(this, AuthWithScannerActivity::class.java).apply {
            putExtra(AuthWithScannerActivity.APP_KEY, BuildConfig.APP_KEY)
            putExtra(AuthWithScannerActivity.PUBLIC_USER_ID, "public-User-ID") // Optional
            putExtra(
                AuthWithScannerActivity.PAYLOAD,
                "{ \"token\" : \"jWwajc88y32kndsf-9a234sdfdhfyr5y\""
            )
        }

        easyKeyriAuthLauncher.launch(intent)
    }
}
```

Or define custom scanner UI/UX. You can use Firebase ML Kit, ZXing, your own scanner, or any other
equivalent. All you need to do is convert to URI, and then you're free to process the response the
same way we did above (notice the `process(uri)` function is exactly the same in both cases)

```kotlin
private fun scanQr() {
    // Your scanner realization
    // Get link from QR and process it:
    process(uri)
}

private fun process(uri: Uri?) {
    uri?.getQueryParameters("sessionId")?.firstOrNull()?.let { sessionId ->
        launch {
            try {
                val appKey = "App key here" // Get this value from the Keyri Developer Portal
                val publicUserId = "example@dot.com" // publicUserId is optional
                val payload = "Custom payload here"

                val keyri = Keyri() // Be sure to import the SDK at the top of the file

                keyri.initiateQrSession(appKey, sessionId, publicUserId)
                    .onSuccess { session ->
                        // You can optionally create a custom screen and pass the session ID there. We recommend this approach for large enterprises
                        keyri.initializeDefaultScreen(supportFragmentManager, session, payload)

                        // In a real world example you’d wait for user confirmation first
                        session.confirm(payload) // or session.deny(payload)
                    }

                // Process result
            } catch (e: Throwable) {
                Log.e("Keyri", "Authentication exception $e")
            }
        }
    } ?: Log.e("Keyri", "Failed to process link")
}
```

### **Interacting with the API**

The following methods are available to interact with the Keyri SDK API, which can be used to craft
your own custom flows and leverage the SDK in different ways:

* `suspend fun initializeQrSession(appKey: String, sessionId: String, publicUserId: String?): Result<Session>`
  - call it after obtaining the sessionId from QR code or deep link. Returns Session object with
  Risk attributes (needed to show confirmation screen) or Exception

* `suspend fun initializeDefaultScreen(fm: FragmentManager, session: Session, payload: String): Boolean`
  - to show Confirmation with default UI. Returns Boolean result. Also you can implement your custom
  Confirmation Screen, just inherit from BaseConfirmationDialog.kt

* `suspend fun Session.confirm(payload: String): Result` - call this function if user confirmed the
  dialog. Returns Boolean authentication result

* `suspend fun Session.deny(payload: String): Result<Boolean>` - call if the user denied the dialog.
  Returns Boolean authentication result

* `fun generateAssociationKey(publicUserId: String): String` - creates a persistent ECDSA keypair
  for the given public user ID (example: email address) and return public key

* `fun getUserSignature(publicUserId: String?, data: String): String` - returns an ECDSA signature
  of the timestamp and optional customSignedData with the publicUserId's privateKey (or, if not
  provided, anonymous privateKey), customSignedData can be anything

* `fun listAssociationKey(): List<String>` - returns a list of names (publicUserIds) of "association
  keys" (public keys)

* `fun getAssociationKey(publicUserId: String?): String` - returns Base64 public key for the
  specified publicUserId

Payload can be anything (session token or a stringified JSON containing multiple items. Can include
things like publicUserId, timestamp, customSignedData and ECDSA signature)

### **Session Object**

The session object is returned on successful `initializeQrSession` calls, and is used to handle
presenting the situation to the end user and getting their confirmation to complete authentication.
Below are some of the key properties and methods that can be triggered. If you are utilizing the
built-in views, you are only responsible for calling the confirm/deny methods above

* `IPAddressMobile/Widget` - The IP Address of both mobile device and web browser&#x20;

* `RiskAnalytics` - if applicable

    - `RiskStatus` - clear, warn or deny

    - `RiskFlagString` - if RiskStatus is warn or deny, this string alerts the user to what is
      triggering the risk situation

    - `GeoData` - Location data for both mobile and widget

        * `Mobile`

            - `city`

            - `country\_code`

        * `Browser`

            - `city`

            - `country\_code`

* `Session.confirm(payload: String)` and `Session.deny(payload: String)` - see descriptions
  in [Interacting with the API](#interacting-with-the-api).

### Disclaimer

We care deeply about the quality of our product and rigorously test every piece of functionality we
offer. That said, every integration is different. Every app on the App Store has a different
permutation of build settings, compiler flags, processor requirements, compatibility issues etc and
it's impossible for us to cover all of those bases, so we strongly recommend thorough testing of
your integration before shipping to production. Please feel free to file a bug or issue if you
notice anything that seems wrong or strange on GitHub 🙂

[Issues](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/issues)

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
