package com.keyrico.keyrisdk.ui.auth

import android.os.Parcelable
import com.keyrico.keyrisdk.entity.RiskAnalytics
import kotlinx.parcelize.Parcelize

@Parcelize
internal sealed class AuthWithScannerState : Parcelable {

    object Empty : AuthWithScannerState()

    object Loading : AuthWithScannerState()

    data class Confirmation(
        val username: String?,
        val widgetUserAgent: String,
        val widgetOrigin: String,
        val logo: String,
        val iPAddressWidget: String,
        val riskAnalytics: RiskAnalytics
    ) : AuthWithScannerState()

    object Authenticated : AuthWithScannerState()

    class Error(val message: String) : AuthWithScannerState()
}
