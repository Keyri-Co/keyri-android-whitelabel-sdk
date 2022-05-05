package com.keyrico.keyrisdk.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyrico.keyrisdk.KeyriSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AuthWithScannerVM : ViewModel() {

    private val _uiState = MutableStateFlow<AuthWithScannerState>(AuthWithScannerState.Empty)

    val uiState: StateFlow<AuthWithScannerState> = _uiState.asStateFlow()

    private var sessionId: String = ""

    fun handleSessionId(sessionId: String, keyriSdk: KeyriSdk) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = AuthWithScannerState.Loading

            try {
                this@AuthWithScannerVM.sessionId = sessionId

                val session = keyriSdk.handleSessionId(sessionId)

                _uiState.value = AuthWithScannerState.Confirmation(session)
            } catch (e: Throwable) {
                processError(e)
            }
        }
    }

    fun challengeSession(
        publicUserId: String,
        publicCustom: String?,
        secureCustom: String?,
        keyriSdk: KeyriSdk
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = AuthWithScannerState.Loading

            try {
                keyriSdk.challengeSession(publicUserId, sessionId, publicCustom, secureCustom)

                _uiState.value = AuthWithScannerState.Authenticated
            } catch (e: Throwable) {
                processError(e)
            }
        }
    }

    private suspend fun processError(e: Throwable) {
        val errorMessage = e.message ?: "Unable to authorize"

        _uiState.value = AuthWithScannerState.Error(errorMessage)

        delay(3_000L)

        _uiState.value = AuthWithScannerState.Empty
    }
}
