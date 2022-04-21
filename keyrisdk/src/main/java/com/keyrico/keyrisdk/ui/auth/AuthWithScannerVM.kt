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
    private val _isFlashEnabled = MutableStateFlow(false)
    private val _isAutofocusEnabled = MutableStateFlow(false)

    val uiState: StateFlow<AuthWithScannerState> = _uiState.asStateFlow()
    val isFlashEnabled: StateFlow<Boolean> = _isFlashEnabled.asStateFlow()
    val isAutofocusEnabled: StateFlow<Boolean> = _isAutofocusEnabled.asStateFlow()

    private var sessionId: String = ""

    fun handleSessionId(sessionId: String, keyriSdk: KeyriSdk) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = AuthWithScannerState.Loading

            try {
                this@AuthWithScannerVM.sessionId = sessionId

                keyriSdk.handleSessionId(sessionId)

                _uiState.value =
                    AuthWithScannerState.Confirmation(
                        "Andrew Kuliahin",
                        "Some important message about login Risk",
                        null
                    )
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

    fun setAutofocusEnabled(isEnabled: Boolean) {
        _isAutofocusEnabled.value = isEnabled
    }

    fun setFlashEnabled(isEnabled: Boolean) {
        _isFlashEnabled.value = isEnabled
    }

    private suspend fun processError(e: Throwable) {
        val errorMessage = e.message ?: "Unable to authorize"

        _uiState.value = AuthWithScannerState.Error(errorMessage)

        delay(3_000L)

        _uiState.value = AuthWithScannerState.Empty
    }
}
