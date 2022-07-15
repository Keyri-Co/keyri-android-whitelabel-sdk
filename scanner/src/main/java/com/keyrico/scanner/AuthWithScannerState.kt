package com.keyrico.scanner

import android.os.Parcelable
import com.keyrico.keyrisdk.entity.session.Session
import kotlinx.parcelize.Parcelize

internal sealed class AuthWithScannerState {

    object Empty : AuthWithScannerState()

    object Loading : AuthWithScannerState()

    @Parcelize
    data class Confirmation(val session: Session) : AuthWithScannerState(), Parcelable

    data class Authenticated(val isSuccess: Boolean) : AuthWithScannerState()

    class Error(val message: String) : AuthWithScannerState()
}
