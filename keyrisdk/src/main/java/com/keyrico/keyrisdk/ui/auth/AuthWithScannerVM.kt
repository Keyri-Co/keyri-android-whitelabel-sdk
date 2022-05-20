package com.keyrico.keyrisdk.ui.auth

import androidx.fragment.app.FragmentManager
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

    fun initiateSession(
        sessionId: String,
        appKey: String,
        keyriSdk: KeyriSdk
    ) {
        _uiState.value = AuthWithScannerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val session = keyriSdk.initiateQrSession(sessionId, appKey)

                this@AuthWithScannerVM.session = session

                _uiState.value = AuthWithScannerState.Confirmation(session)
            } catch (e: Throwable) {
                processError(e)
            }
        }
    }

    fun approveSession(
        supportFragmentManager: FragmentManager,
        session: Session,
        keyriSdk: KeyriSdk
    ) {
        _uiState.value = AuthWithScannerState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                keyriSdk.initializeDefaultScreen(supportFragmentManager, session)

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
