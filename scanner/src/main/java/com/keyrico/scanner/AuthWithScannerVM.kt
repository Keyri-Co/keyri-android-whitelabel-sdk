package com.keyrico.scanner

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyrico.keyrisdk.Keyri
import com.keyrico.keyrisdk.entity.session.Session
import com.keyrico.keyrisdk.exception.RiskException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AuthWithScannerVM : ViewModel() {

    private val _uiState = MutableStateFlow<AuthWithScannerState>(AuthWithScannerState.Empty)

    val uiState: StateFlow<AuthWithScannerState> = _uiState.asStateFlow()

    fun initiateSession(appKey: String, sessionId: String, publicUserId: String?, keyri: Keyri) {
        _uiState.value = AuthWithScannerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            keyri.initiateQrSession(
                appKey = appKey,
                sessionId = sessionId,
                publicUserId = publicUserId
            ).onSuccess { session ->
                _uiState.value = AuthWithScannerState.Confirmation(session)
            }.onFailure { processError(it) }
        }
    }

    fun showConfirmationScreen(
        supportFragmentManager: FragmentManager,
        session: Session,
        payload: String,
        keyri: Keyri
    ) {
        _uiState.value = AuthWithScannerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            keyri.initializeDefaultScreen(supportFragmentManager, session, payload)
                .onSuccess { isAuthenticated ->
                    _uiState.value = AuthWithScannerState.Authenticated(isAuthenticated)
                }.onFailure {
                    if (it !is RiskException) {
                        processError(it)
                    }
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
