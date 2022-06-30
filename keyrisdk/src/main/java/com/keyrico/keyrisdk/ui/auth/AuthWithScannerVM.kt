package com.keyrico.keyrisdk.ui.auth

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyrico.keyrisdk.Keyri
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
        keyri: Keyri
    ) {
        _uiState.value = AuthWithScannerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            keyri.initiateQrSession(
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
        keyri: Keyri
    ) {
        _uiState.value = AuthWithScannerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            val isApproved = keyri.initializeDefaultScreen(supportFragmentManager, session)

            if (isApproved) {
                session.confirm().onSuccess {
                    _uiState.value = AuthWithScannerState.Authenticated(it)
                }.onFailure {
                    processError(it)
                }
            } else {
                session.deny().getOrNull()

                _uiState.value = AuthWithScannerState.Authenticated(false)
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
