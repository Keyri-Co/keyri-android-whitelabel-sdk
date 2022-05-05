package com.keyrico.keyrisdk.ui.auth

import android.os.Parcelable
import com.keyrico.keyrisdk.entity.Session
import kotlinx.parcelize.Parcelize

internal sealed class AuthWithScannerState {

    object Empty : AuthWithScannerState()

    object Loading : AuthWithScannerState()

    @Parcelize
    data class Confirmation(val session: Session) : AuthWithScannerState(), Parcelable

    object Authenticated : AuthWithScannerState()

    class Error(val message: String) : AuthWithScannerState()
}
