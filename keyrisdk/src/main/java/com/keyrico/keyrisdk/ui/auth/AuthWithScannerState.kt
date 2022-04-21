package com.keyrico.keyrisdk.ui.auth

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal sealed class AuthWithScannerState : Parcelable {

    object Empty : AuthWithScannerState()

    object Loading : AuthWithScannerState()

    data class Confirmation(
        val username: String,
        val message: String?,
        val characteristics: Map<String, String>?
    ) : AuthWithScannerState()

    object Authenticated : AuthWithScannerState()

    class Error(val message: String) : AuthWithScannerState()
}
