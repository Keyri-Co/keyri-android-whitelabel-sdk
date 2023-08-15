<p align="center">
    <a href="">
        <img src="https://keyri.com/wp-content/uploads/2022/09/Keyri-Grey-Logo-Website-300x147.png" width=200 />
    </a>
</p>

<p align="center">Gradle Android plugin for creating and check checksums with Keyri SDK</p>

# Overview

This repository contains the open source code for [Keyri](https://keyri.com) Android SDK.

![Lint](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/workflows/Lint/badge.svg)
![Firebase Instrumentation Tests](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/workflows/Firebase%20Instrumentation%20Tests/badge.svg)
![Release](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/workflows/Release/badge.svg)

[![GitHub release](https://img.shields.io/github/release/Keyri-Co/keyri-android-whitelabel-sdk.svg?maxAge=10)](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/releases)

![demo](images/demo.gif)

## Contents

* [System Requirements](#system-requirements)
* [Demo](#demo)
* [Integration](#integration)
* [Option 1 - App Links](#option-1---app-links)
* [Option 2 - In-App Scanner](#option-2---in-app-scanner)
* [Jetpack Compose support](#jetpack-compose-support)
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
depend on Kotlin functionality. If you are using Java - see [Keyri java](keyrisdk-java/README.md).

## Demo

This repository contains a demonstration app for the Keyri SDK product. To build and run the demo
app, follow the instructions below.

### **Integration**

Add SDK dependency to your module **build.gradle** file and sync project:

```kotlin
dependencies {
    // ...
    implementation("com.keyri:keyrisdk:$latestKeyriVersion")
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
the `onNewIntent()` and `onCreate()` methods, and pass `sessionId` to process the entire flow
yourself:

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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val appKey = "[Your appKey]" // Get this value from the Keyri Developer Portal
                val publicApiKey =
                    "[Your publicApiKey]" // Get this optional value from the Keyri Developer Portal for Fraud Prevention
                val serviceEncryptionKey =
                    "[Your serviceEncryptionKey]" // Get this optional value from the Keyri Developer Portal for Fraud Prevention
                val publicUserId = "public-User-Id" // publicUserId is optional
                val payload = "Custom payload here"

                // Be sure to import the SDK at the top of the file
                val keyri = Keyri(this@MainActivity, appKey, publicApiKey, serviceEncryptionKey)

                keyri.initiateQrSession(sessionId, publicUserId)
                    .onSuccess { session ->
                        // You can optionally create a custom screen and pass the session ID there. We recommend this approach for large enterprises
                        val session =
                            keyri.initializeDefaultScreen(supportFragmentManager, session, payload)
                                .getOrThrow()

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

Add Keyri and Keyri-Scanner dependency to your module **build.gradle** file and sync project:

```kotlin
dependencies {
    // ...
    implementation("com.keyri:keyrisdk:$latestKeyriVersion")
    implementation("com.keyri:scanner:$latestKeyriVersion")
}
```

Use `AuthWithScannerActivity` built-in functionality to delegate authentication to SDK. You can use
`ActivityResult API` or `onActivityResult`. Call `easyKeyriAuth` and pass `appKey`, `payload` and
optional `publicUserId`, `publicApiKey`, `serviceEncryptionKey`:

```kotlin
private val easyKeyriAuthLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
        val isSuccess = activityResult.resultCode == Activity.RESULT_OK
        // Handle authentication result
        // ...
    }

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Using ActivityResult API:
    binding.bAuthWithScanner.setOnClickListener {
        easyKeyriAuth(
            this,
            easyKeyriAuthLauncher,
            "[Your appKey]", // Get this value from the Keyri Developer Portal
            "[Your publicApiKey]", // Get this optional value from the Keyri Developer Portal for Fraud Prevention
            "[Your serviceEncryptionKey]", // Get this optional value from the Keyri Developer Portal for Fraud Prevention
            "Custom payload here",
            "public-User-Id", // publicUserId is optional
        )
    }

    // Or with on activityResult:
    binding.bAuthWithScanner.setOnClickListener {
        // This will call an activity that will return a result
        // Handle this result in onActivityResult function
        easyKeyriAuth(
            this,
            REQUEST_CODE,
            "[Your appKey]", // Get this value from the Keyri Developer Portal
            "[Your publicApiKey]", // Get this optional value from the Keyri Developer Portal for Fraud Prevention
            "[Your serviceEncryptionKey]", // Get this optional value from the Keyri Developer Portal for Fraud Prevention
            "Custom payload here",
            "public-User-Id", // publicUserId is optional
        )
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val appKey = "[Your appKey]" // Get this value from the Keyri Developer Portal
                val publicApiKey =
                    "[Your publicApiKey]" // Get this optional value from the Keyri Developer Portal for Fraud Prevention
                val serviceEncryptionKey =
                    "[Your serviceEncryptionKey]" // Get this optional value from the Keyri Developer Portal for Fraud Prevention
                val publicUserId = "public-User-Id" // publicUserId is optional
                val payload = "Custom payload here"

                // Be sure to import the SDK at the top of the file
                val keyri = Keyri(this@MainActivity, appKey, publicApiKey, serviceEncryptionKey)

                keyri.initiateQrSession(sessionId, publicUserId)
                    .onSuccess { session ->
                        // You can optionally create a custom screen and pass the session ID there. We recommend this approach for large enterprises
                        val session =
                            keyri.initializeDefaultScreen(supportFragmentManager, session, payload)
                                .getOrThrow()

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

### **Jetpack Compose support**

Add Keyri and Keyri-Compose dependency to your module **build.gradle** file and sync project:

```kotlin
dependencies {
    // ...
    implementation("com.keyri:keyrisdk:$latestKeyriVersion")
    implementation("com.keyri:compose:$latestKeyriVersion")
}
```

Use `ScannerPreview` for retrieving sessionId from scanned QR code. All you need here to provide is:

* `modifier: Modifier` - modifier object to change the appearance of the ScannerPreview.
* `onScanResult: (Result<String>)` - callback for handling sessionId.
* `onClose: () -> Unit` - callback for handling close button.
* `isLoading: Boolean` - value to show progress bar.

After you receive `Session` object you can create your own confirmation dialog or use
default: `ConfirmationModalBottomSheet`. Provide next arguments:

* `modalBottomSheetState: ModalBottomSheetState` - state to to manage BottomSheet.
* `coroutineScope: CoroutineScope` - coroutine scope for suspending calls inside BottomSheet.
* `session: Session?` - session object to process (should not be null when modalBottomSheetState is
  shown).
* `payload: String` - can be anything (session token or a stringified JSON containing multiple
  items. Can include things like publicUserId, timestamp, customSignedData and ECDSA signature).
* `onResult: (Result<Boolean>)` - callback for handling result of user confirmation.

To handle deeplink with default confirmation screen you can use `EasyKeyriAuth` composable:

```kotlin
val coroutineScope = rememberCoroutineScope()

val modalBottomSheetState = rememberModalBottomSheetState(
    initialValue = ModalBottomSheetValue.Hidden,
    skipHalfExpanded = true
)

EasyKeyriAuth(
    modalBottomSheetState,
    coroutineScope,
    keyri,
    sessionId,
    "Custom payload here",
    "public-User-Id" // publicUserId is optional
) { result ->
    // Process result
}
```

### **Interacting with the API**

The following methods are available to interact with the Keyri SDK API, which can be used to craft
your own custom flows and leverage the SDK in different ways:

* `suspend fun Keyri.initializeQrSession(sessionId: String, publicUserId: String?): Result<Session>`:
  call it after obtaining the sessionId from QR code or deep link. Returns result of Session object
  with Risk attributes (needed to show confirmation screen) or Throwable error

* `suspend fun Keyri.initializeDefaultConfirmationScreen(fm: FragmentManager, session: Session, payload: String, publicUserId: String?): Result<Unit>`:
  to show Confirmation with default UI. Returns Boolean result or Throwable error. Also you can
  implement your custom Confirmation Screen, just inherit from BaseConfirmationDialog.kt

* `suspend fun Session.confirm(payload: String, context: Context, trustNewBrowser: Boolean, publicUserId: String?): Result<Unit>`:
  call this function if user
  confirmed the dialog. Returns Boolean authentication result or Throwable error

* `suspend fun Session.deny(payload: String, context: Context, trustNewBrowser: Boolean): Result<Unit>`:
  call if the user denied the dialog.
  Returns Boolean denial result or Throwable error

* `suspend fun Keyri.processLink(fragmentManager: FragmentManager, url: Uri, payload: String, publicUserId: String?): Result<Unit>`:
  process flow with passed uri with showing default confirmation screen. Easiest way to process
  session from deeplink. Returns Boolean result of authentication or Throwable error

* `suspend fun Keyri.sendEvent(publicUserId: String = ANON_USER, eventType: EventType, success: Boolean): Result<FingerprintEventResponse> `:
  send fingerprint event and event result for specified publicUserId's.

* `suspend fun Keyri.generateAssociationKey(publicUserId: String = "ANON"): Result<String>`: creates
  a
  persistent
  ECDSA keypair for the given publicUserId (example: email address) or default without arguments and
  return Base64 string public key

* `suspend fun Keyri.generateUserSignature(publicUserId: String = "ANON", data: String): Result<String>`:
  returns an
  ECDSA signature of custom data for sign with the custom publicUserId's privateKey (or, if not
  provided, anonymous privateKey), customSignedData can be anything

* `suspend fun Keyri.listAssociationKey(): Result<Map<String, String>>`: returns a map of "
  association keys"
  and
  ECDSA Base64 public keys.

* `suspend fun Keyri.listUniqueAccounts(): Result<Map<String, String>>`: returns a map of unique "
  association keys"
  and ECDSA Base64 public keys.

* `suspend fun Keyri.getAssociationKey(publicUserId: String = "ANON"): Result<String>`: returns
  association
  Base64
  public key for the specified publicUserId (or, if not provided, for anonymous)

* `suspend fun Keyri.removeAssociationKey(publicUserId: String): Result<Unit>`: removes association
  public key for the specified publicUserId

* `fun easyKeyriAuth(content: Context, easyKeyriAuthLauncher: ActivityResultLauncher<Intent>, appKey: String, publicApiKey: String?, serviceEncryptionKey: String?, payload: String, publicUserId: String?)`:
  launches scanner activity with default confirmation screen for ActivityResultLauncher

* `fun easyKeyriAuth(activity: Activity, requestCode: Int, appKey: String, publicApiKey: String?, serviceEncryptionKey: String?, payload: String, publicUserId: String?)`:
  launches scanner activity for result with default confirmation screen for onActivityResult

* `@Composable fun EasyKeyriAuth(sheetState: ModalBottomSheetState, coroutineScope: CoroutineScope, keyri: Keyri, url: Uri, payload: String, publicUserId: String?, result: (Result<Unit>) -> Unit)`:
  handle process flow with passed scanned url and showing default confirmation screen. Easiest way
  to process session from deeplink

* `@Composable fun ConfirmationModalBottomSheet(modalBottomSheetState: ModalBottomSheetState, coroutineScope: CoroutineScope, session: Session? = null, publicUserId: String?, payload: String, onResult: (Result<Unit>) -> Unit)`:
  to show Confirmation with default UI. Returns Boolean result or Throwable error

* `@Composable fun ScannerPreview(modifier: Modifier = Modifier, onScanResult: (Result<String>) -> Unit = {}, onClose: () -> Unit = {}, isLoading: Boolean = false)`:
  default QR scanner implementation based on ML Kit. Returns result of scanning (string sessionId or
  error)

Payload can be anything (session token or a stringified JSON containing multiple items. Can include
things like publicUserId, timestamp, customSignedData and ECDSA signature)

### **Session Object**

The session object is returned on successful `initializeQrSession` calls, and is used to handle
presenting the situation to the end user and getting their confirmation to complete authentication.
Below are some of the key properties and methods that can be triggered. If you are utilizing the
built-in views, you are only responsible for calling the confirm/deny methods above

* `iPAddressMobile/Widget` - The IP Address of both mobile device and web browser&#x20;
* `riskAnalytics` - if applicable
    - `riskStatus` - clear, warn or deny
    - `riskFlagString` - if RiskStatus is warn or deny, this string alerts the user to what is
      triggering the risk situation
    - `geoData` - Location data for both mobile and widget
        * `mobile`
            - `city`
            - `country\_code`

        * `browser`
            - `city`
            - `country\_code`

* `Session.confirm(payload: String)` and `Session.deny(payload: String)` - see descriptions
  in [Interacting with the API](#interacting-with-the-api).

### Backup Keyri accounts

If you do not want to lose the list of Keyri accounts after deleting the application, you can
configure Backup. You can learn more about this in
the [BACKUP](keyrisdk/src/main/java/com/keyrico/keyrisdk/backup/BACKUP.md) section.

### Disclaimer

We care deeply about the quality of our product and rigorously test every piece of functionality we
offer. That said, every integration is different. Every app on the App Store has a different
permutation of build settings, compiler flags, processor requirements, compatibility issues etc and
it's impossible for us to cover all of those bases, so we strongly recommend thorough testing of
your integration before shipping to production. Please feel free to file a bug or issue if you
notice anything that seems wrong or strange on GitHub 🙂

[Issues](https://github.com/Keyri-Co/keyri-android-whitelabel-sdk/issues)

## License

This library is available under paid and free licenses. See the [LICENSE](LICENSE) file for the full
license text.

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
