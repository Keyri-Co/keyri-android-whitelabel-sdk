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

    fun initiateSession(sessionId: String, appKey: String, keyriSdk: KeyriSdk) {
        _uiState.value = AuthWithScannerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val session = keyriSdk.initiateQrSession(sessionId, appKey).getOrThrow()

                _uiState.value = AuthWithScannerState.Confirmation(session)
            } catch (e: Throwable) {
                processError(e)
            }
        }
    }

    fun showConfirmationScreen(
        supportFragmentManager: FragmentManager,
        session: Session,
        publicUserId: String?,
        payload: String,
        keyriSdk: KeyriSdk
    ) {
        _uiState.value = AuthWithScannerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isApproved = keyriSdk.initializeDefaultScreen(supportFragmentManager, session)

                val result = if (isApproved) {
                    session.confirm(publicUserId, payload)
                } else {
                    session.deny(publicUserId, payload)
                }.getOrThrow()

                _uiState.value = AuthWithScannerState.Authenticated(result)
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
