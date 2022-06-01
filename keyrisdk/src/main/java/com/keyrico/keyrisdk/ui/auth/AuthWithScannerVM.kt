package com.keyrico.keyrisdk.ui.auth

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyrico.keyrisdk.KeyriSdk
import com.keyrico.keyrisdk.entity.session.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AuthWithScannerVM : ViewModel() {

    private val _uiState = MutableStateFlow<AuthWithScannerState>(AuthWithScannerState.Empty)

    val uiState: StateFlow<AuthWithScannerState> = _uiState.asStateFlow()

    fun initiateSession(
        appKey: String,
        sessionId: String,
        payload: String,
        publicUserId: String?,
        keyriSdk: KeyriSdk
    ) {
        _uiState.value = AuthWithScannerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            keyriSdk.initiateQrSession(
                appKey = appKey,
                sessionId = sessionId,
                payload = payload,
                publicUserId = publicUserId
            ).onSuccess { session ->
                _uiState.value = AuthWithScannerState.Confirmation(session)
            }.onFailure { processError(it) }
        }
    }

    fun showConfirmationScreen(
        supportFragmentManager: FragmentManager,
        session: Session,
        keyriSdk: KeyriSdk
    ) {
        _uiState.value = AuthWithScannerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            val isApproved = keyriSdk.initializeDefaultScreen(supportFragmentManager, session)

            if (isApproved) {
                session.confirm()
            } else {
                session.deny()
            }.onSuccess {
                _uiState.value = AuthWithScannerState.Authenticated(it)
            }.onFailure {
                processError(it)
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
