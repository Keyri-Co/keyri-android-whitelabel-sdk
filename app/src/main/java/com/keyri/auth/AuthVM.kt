package com.keyri.auth

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.keyri.R
import com.keyrico.keyrisdk.KeyriSdk
import com.keyrico.keyrisdk.exception.AccountNotFoundException
import com.keyrico.keyrisdk.exception.KeyriSdkException
import com.keyrico.keyrisdk.utils.LiveEvent
import kotlinx.coroutines.launch

class AuthVM(private val app: Application, private val keyriSdk: KeyriSdk) : AndroidViewModel(app) {

    private val loadingLD = MutableLiveData<Boolean>()
    private val messageLD = LiveEvent<String>()
    private val authenticatedLD = LiveEvent<Boolean>()

    fun loading() = loadingLD as LiveData<Boolean>

    fun message() = messageLD as LiveData<String>

    fun authenticated() = authenticatedLD as LiveData<Boolean>

    fun authenticate(sessionId: String, secureCustom: String? = null) {
        viewModelScope.launch {
            loadingLD.value = true
            try {
                val session = keyriSdk.handleSessionId(sessionId)

                if (secureCustom != null) {
                    keyriSdk.whitelabelAuth(sessionId, secureCustom)
                } else {
                    if (session.isNewUser) {
                        keyriSdk.sessionSignup(
                            session.username ?: "",
                            sessionId,
                            session.service,
                            CUSTOM_DATA_SIGNUP
                        )
                    } else {
                        val account =
                            keyriSdk.getAccounts().firstOrNull() ?: throw AccountNotFoundException
                        keyriSdk.sessionLogin(account, sessionId, session.service, CUSTOM_DATA_LOGIN)
                    }
                }

                authenticatedLD.value = true
            } catch (e: Throwable) {
                Log.d("Keyri", "Authentication exception $e")

                if (e is KeyriSdkException) {
                    messageLD.value = app.getString(e.errorMessage)
                } else {
                    messageLD.value = e.message ?: app.getString(R.string.error_general)
                }
            }
            loadingLD.value = false
        }
    }

    fun authWithScanner(activity: Activity, requestCode: Int) {
        keyriSdk.easyKeyriAuth(activity, requestCode)
    }

    companion object {
        private const val CUSTOM_DATA_SIGNUP = "test custom signup"
        private const val CUSTOM_DATA_LOGIN = "test custom login"
    }
}
