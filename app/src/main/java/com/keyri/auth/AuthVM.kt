package com.keyri.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.keyrisdk.KeyriSdk
import com.example.keyrisdk.exception.AccountNotFoundException
import com.example.keyrisdk.exception.KeyriSdkException
import com.hadilq.liveevent.LiveEvent
import com.keyri.R
import kotlinx.coroutines.launch

class AuthVM(private val app: Application): AndroidViewModel(app) {

    private val loadingLD = MutableLiveData<Boolean>()
    fun loading() = loadingLD as LiveData<Boolean>

    private val messageLD = LiveEvent<String>()
    fun message() = messageLD as LiveData<String>

    private val authenticatedLD = LiveEvent<Boolean>()
    fun authenticated() = authenticatedLD as LiveData<Boolean>

    fun authenticate(sessionId: String) {
        viewModelScope.launch {
            loadingLD.value = true
            try {
                val session = KeyriSdk.onReadSessionId(sessionId)
                if (session.isNewUser) {
                    KeyriSdk.signup(session.username, sessionId, session.service,
                        CUSTOM_DATA_SIGNUP
                    )
                } else {
                    val account = KeyriSdk.accounts().firstOrNull() ?: throw AccountNotFoundException
                    KeyriSdk.login(account, sessionId, session.service,
                        CUSTOM_DATA_LOGIN
                    )
                }
                authenticatedLD.value = true
            } catch (e: Throwable) {
                Log.d("Keyri", "Authentication exception $e")
                if (e is KeyriSdkException) {
                    messageLD.value = app.getString(e.errorMessage)
                } else {
                    messageLD.value = app.getString(R.string.error_general)
                }
            }
            loadingLD.value = false
        }
    }

    companion object {
        private const val CUSTOM_DATA_SIGNUP = "test custom signup"
        private const val CUSTOM_DATA_LOGIN = "test custom login"
    }

}