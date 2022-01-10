package com.example.keyrisdk.view

import androidx.appcompat.app.AppCompatActivity
import com.example.keyrisdk.KeyriSdk
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.entity.Service
import com.example.keyrisdk.exception.KeyriSdkException

/**
 * Params class to to initialize Keyri Scanner View.
 *
 * @activity AppCompatActivity to bind lifecycle and asking permissions.
 * @keyriSdk pass KeyriSdk, which you initialized earlier.
 * @customArgument optional custom argument.
 * @onChooseAccount required callback to implement for choosing account.
 * @onCompleted optional callback to listen complete auth events.
 * @onError optional callback to listen error events.
 * @onAccountAlreadyExists optional callback for cases when the flag allowMultipleAccounts = false
 * and you need to choose: replace the current account or cancel the action.
 * Return true to remove existing account and init new session
 * @onLoading optional callback for loading events.
 */
data class KeyriScannerViewParams(
    val activity: AppCompatActivity,
    val keyriSdk: KeyriSdk,
    val customArgument: String? = null,
    val onChooseAccount: (accounts: List<PublicAccount>, sessionId: String, service: Service) -> Unit,
    val onCompleted: (() -> Unit)? = null,
    val onError: ((error: KeyriSdkException) -> Unit)? = null,
    val onAccountAlreadyExists: (() -> Boolean)? = null,
    val onLoading: ((isLoading: Boolean) -> Unit)? = null
)
