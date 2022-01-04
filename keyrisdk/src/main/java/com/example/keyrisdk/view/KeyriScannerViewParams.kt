package com.example.keyrisdk.view

import androidx.appcompat.app.AppCompatActivity
import com.example.keyrisdk.KeyriSdk
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.exception.KeyriSdkException

data class KeyriScannerViewParams(
    val activity: AppCompatActivity,
    val keyriSdk: KeyriSdk,
    val customArgument: String?,
    val onCompleted: (() -> Unit)? = null,
    val onError: ((error: KeyriSdkException) -> Unit)? = null,
    val onChooseAccount: ((List<PublicAccount>) -> PublicAccount)? = null,
    val onAccountAlreadyExists: ((sessionId: String) -> Boolean)? = null,
    val onLoading: ((isLoading: Boolean) -> Unit)? = null
)
