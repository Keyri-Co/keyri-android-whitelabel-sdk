package com.keyrico.keyrisdk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyrico.keyrisdk.entity.session.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebViewViewModel : ViewModel() {

    private val _authenticated = MutableStateFlow<Session?>(null)

    val authenticated: StateFlow<Session?> = _authenticated.asStateFlow()

    fun newSession(sessionId: String, publicUserId: String, appKey: String, keyri: Keyri) {
        viewModelScope.launch(Dispatchers.IO) {
            keyri.initiateQrSession(
                appKey = appKey,
                sessionId = sessionId,
                payload = "Some payload",
                publicUserId = publicUserId
            ).onSuccess { session ->
                session.confirm()

                _authenticated.value = session
            }.onFailure { throw it }
        }
    }
}
