package com.keyrico.keyrisdk.ui.auth

internal sealed class AuthWithScannerState {

    object Empty : AuthWithScannerState()

    object Loading : AuthWithScannerState()

    sealed class Confirmation : AuthWithScannerState() {

        class Message(val message: String) : Confirmation()

        class RiskCharacteristics(val characteristics: List<String>) : Confirmation()
    }

    object Authenticated : AuthWithScannerState()

    class Error(val message: String) : AuthWithScannerState()
}
