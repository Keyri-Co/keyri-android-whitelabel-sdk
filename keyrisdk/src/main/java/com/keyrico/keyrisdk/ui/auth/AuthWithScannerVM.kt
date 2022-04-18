package com.keyrico.keyrisdk.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyrico.keyrisdk.KeyriSdk
import com.keyrico.keyrisdk.R
import com.keyrico.keyrisdk.exception.KeyriSdkException
import kotlinx.coroutines.Dispatchers
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

    fun handleSessionId(sessionId: String, keyriSdk: KeyriSdk, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = AuthWithScannerState.Loading

            try {
                this@AuthWithScannerVM.sessionId = sessionId

                keyriSdk.handleSessionId(sessionId)

                // TODO Add implementation
//                _uiState.value = AuthWithScannerState.Confirmation

                _uiState.value = AuthWithScannerState.Confirmation.Message("Some message")
//                _uiState.value = AuthWithScannerState.Confirmation.RiskCharacteristics(
//                    listOf("First", "Second", "Third")
//                )
            } catch (e: Throwable) {
                processError(e, context)
            }
        }
    }

    fun challengeSession(
        publicUserId: String,
        publicCustom: String?,
        secureCustom: String?,
        keyriSdk: KeyriSdk,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = AuthWithScannerState.Loading

            try {
                keyriSdk.challengeSession(publicUserId, sessionId, publicCustom, secureCustom)

                _uiState.value = AuthWithScannerState.Authenticated
            } catch (e: Throwable) {
                processError(e, context)
            }
        }
    }

    fun setAutofocusEnabled(isEnabled: Boolean) {
        _isAutofocusEnabled.value = isEnabled
    }

    fun setFlashEnabled(isEnabled: Boolean) {
        _isFlashEnabled.value = isEnabled
    }

    private fun processError(e: Throwable, context: Context) {
        val errorMessage = if (e is KeyriSdkException) {
            context.getString(e.errorMessage)
        } else {
            e.message ?: context.getString(R.string.keyri_err_authorization)
        }

        _uiState.value = AuthWithScannerState.Error(errorMessage)
    }
}
