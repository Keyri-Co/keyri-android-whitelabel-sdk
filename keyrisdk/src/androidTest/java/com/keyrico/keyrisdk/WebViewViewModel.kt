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

    fun newSession(sessionId: String, publicUserId: String, appKey: String, keyriSdk: KeyriSdk) {
        viewModelScope.launch(Dispatchers.IO) {
            keyriSdk.initiateQrSession(sessionId, appKey).onSuccess { session ->
                session.confirm(publicUserId, "Some payload")

                _authenticated.value = session
            }.onFailure { throw it }
        }
    }
}
