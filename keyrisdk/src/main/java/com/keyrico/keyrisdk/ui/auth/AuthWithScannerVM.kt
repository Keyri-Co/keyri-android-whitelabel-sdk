package com.keyrico.keyrisdk.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyrico.keyrisdk.KeyriSdk
import com.keyrico.keyrisdk.entity.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AuthWithScannerVM : ViewModel() {

    private val _uiState = MutableStateFlow<AuthWithScannerState>(AuthWithScannerState.Empty)

    val uiState: StateFlow<AuthWithScannerState> = _uiState.asStateFlow()

    private var session: Session? = null

    fun initiateSession(sessionId: String, keyriSdk: KeyriSdk) {
        _uiState.value = AuthWithScannerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val session = keyriSdk.initiateSession(sessionId)

                this@AuthWithScannerVM.session = session

                _uiState.value = AuthWithScannerState.Confirmation(session)
            } catch (e: Throwable) {
                processError(e)
            }
        }
    }

    fun approveSession(
        publicUserId: String,
        username: String?,
        publicCustom: String?,
        secureCustom: String?,
        keyriSdk: KeyriSdk
    ) {
        _uiState.value = AuthWithScannerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val browserPublicKey = session?.browserPublicKey
                    ?: throw IllegalStateException("BrowserPublicKey is null")
                val sessionId =
                    session?.sessionId ?: throw IllegalStateException("SessionId is null")

                keyriSdk.approveSession(
                    publicUserId,
                    username,
                    browserPublicKey,
                    sessionId,
                    publicCustom,
                    secureCustom
                )

                _uiState.value = AuthWithScannerState.Authenticated
            } catch (e: Throwable) {
                processError(e)
            }
        }
    }

    fun clearState() {
        _uiState.value = AuthWithScannerState.Empty
    }

    private suspend fun processError(e: Throwable) {
        val errorMessage = e.message ?: "Unable to authorize"

        _uiState.value = AuthWithScannerState.Error(errorMessage)

        delay(3_000L)

        _uiState.value = AuthWithScannerState.Empty
    }
}
