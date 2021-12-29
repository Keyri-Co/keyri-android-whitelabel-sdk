package com.keyri.auth

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.keyrisdk.KeyriSdk
import com.example.keyrisdk.exception.AccountNotFoundException
import com.example.keyrisdk.exception.KeyriSdkException
import com.example.keyrisdk.utils.LiveEvent
import com.keyri.R
import kotlinx.coroutines.launch

class AuthVM(private val app: Application, private val keyriSdk: KeyriSdk) : AndroidViewModel(app) {

    private val loadingLD = MutableLiveData<Boolean>()
    private val messageLD = LiveEvent<String>()
    private val authenticatedLD = LiveEvent<Boolean>()

    fun loading() = loadingLD as LiveData<Boolean>

    fun message() = messageLD as LiveData<String>

    fun authenticated() = authenticatedLD as LiveData<Boolean>

    fun authenticate(sessionId: String) {
        viewModelScope.launch {
            loadingLD.value = true
            try {
                val session = keyriSdk.onReadSessionId(sessionId)
                if (session.isNewUser) {
                    keyriSdk.signup(
                        session.username,
                        sessionId,
                        session.service,
                        CUSTOM_DATA_SIGNUP
                    )
                } else {
                    val account =
                        keyriSdk.accounts().firstOrNull() ?: throw AccountNotFoundException
                    keyriSdk.login(account, sessionId, session.service, CUSTOM_DATA_LOGIN)
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

    fun authWithScanner(activity: Activity, custom: String) {
        keyriSdk.authWithScanner(
            activity,
            custom,
            KeyriSdk.QrAuthCallbacks({
                authenticatedLD.value = true
            }, {
                messageLD.value = app.getString(R.string.not_authenticated)
            })
        )
    }

    companion object {
        private const val CUSTOM_DATA_SIGNUP = "test custom signup"
        private const val CUSTOM_DATA_LOGIN = "test custom login"
    }
}
