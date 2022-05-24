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

```kotlin
dependencies {
    // ...
    implementation("com.github.Keyri-Co:keyri-android-whitelabel-sdk:1.0.11")
}
```

## Usage

### Option 1: Use **AuthWithScannerActivity** built-in functionality to delegate authentication to SDK

You can use ActivityResult API or onActivityResult. All you need to pass is App Key, Public User ID
and Payload with AuthWithScannerActivity.APP_KEY, AuthWithScannerActivity.PUBLIC_USER_ID and
AuthWithScannerActivity.PAYLOAD extra identifiers:

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
    binding.bEasyKeyriAuth.setOnClickListener {
        val intent = Intent(this, AuthWithScannerActivity::class.java).apply {
            putExtra(AuthWithScannerActivity.APP_KEY, BuildConfig.APP_KEY)
            putExtra(AuthWithScannerActivity.PUBLIC_USER_ID, "public-User-ID")
            putExtra(
                AuthWithScannerActivity.PAYLOAD,
                "{ \"token\" : \"jWwajc88y32kndsf-9a234sdfdhfyr5y\""
            )
        }

        easyKeyriAuthLauncher.launch(intent)
    }
}
```

You could check full code
in [AuthWithScannerActivity](app/src/main/java/com/keyri/ui/main/MainActivity.kt).

### Option 2: Build a custom authentication UI/UX

Alternatively, if you want to provide a custom authentication UI/UX, use the following methods:

* **suspend fun initiateQrSession(sessionId: String, appKey: String): Session** - Call it after
  obtaining the sessionId from QR-code or deep link. Returns Session object with Risk Attributes (
  needed to show confirmation screen) or Exception.
* **suspend fun initializeDefaultScreen(fm: FragmentManager, session: Session): Boolean** - To show
  Confirmation Screen with default UI. Returns Boolean result of confirmation. Also you can
  implement your custom Confirmation Screen, just inherit
  from [BaseConfirmationBottomDialog.kt](keyrisdk/src/main/java/com/keyrico/keyrisdk/ui/confirmation/BaseConfirmationBottomDialog.kt)
  a class.
* **suspend fun Session.confirm(publicUserId: String?, payload: String): Boolean** - Call this
  function if user confirmed the dialog.
* **suspend fun Session.deny(publicUserId: String?, payload: String): Boolean** - Call if user
  denied the dialog.
* **fun generateAssociationKey(publicUserId: String): String** - Create a persistent ECDSA keypair
  for the given public user ID (example: email address) and return public key.
* **fun getUserSignature(publicUserId: String?, customSignedData: String?): String** - Return an
  ECDSA signature of the timestamp and optional customSignedData with the publicUserId's privateKey,
  customSignedData can be anything.
* **fun listAssociationKey(): List<String>** - Return a list of names (publicUserIds) of "
  association keys" (public keys).
* **getAssociationKey(publicUserId: String): String** - Returns Base64 public key for the specified
  publicUserId.

Payload can be anything (session token or a stringified JSON containing multiple items. Can include
things like publicUserId, timestamp, customSignedData and ECDSA signature).

```kotlin
val session = keyriSdk.initiateQrSession(sessionId, BuildConfig.APP_KEY)

// Show confirmation screen and if positive do next:

val confirmationResult = initializeDefaultScreenn(supportFragmentManager, session)

if (confirmationResult) {
    val isSuccess = session.confirm(publicUserId, payload)
} else {
    session.deny(publicUserId, payload)
}
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

This will handle all links with such scheme: [https://www.keyri.co?sessionId=3842hsf-324e23]. In the
activity where the processing of links is declared, you need to add handlers in the
**onNewIntent()** and **onCreate()** methods:

```kotlin
private val keyriSdk = KeyriSdk()

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
        viewModel.initiateQrSession(sessionId, appKey, keyriSdk)
    } ?: Log.e("Keyri", "Failed to process link")
}
```

The last thing you need to do in order for your deep links to be processed is to create the
associations for each of the declared hosts for handling in JSON file as described
here: [Verify Android App Links](https://developer.android.com/training/app-links/verify-site-associations)
.

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
