package com.keyrico.keyrisdk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyrico.keyrisdk.entity.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebViewViewModel : ViewModel() {

    private val _authenticated = MutableStateFlow<Session?>(null)

    val authenticated: StateFlow<Session?> = _authenticated.asStateFlow()

    fun newSession(
        sessionId: String,
        publicUserId: String,
        username: String?,
        publicCustom: String?,
        secureCustom: String?,
        keyriSdk: KeyriSdk
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = keyriSdk.initiateSession(sessionId)

            keyriSdk.approveSession(
                publicUserId,
                username,
                session.browserPublicKey,
                sessionId,
                publicCustom,
                secureCustom
            )

            _authenticated.value = session
        }
    }
}