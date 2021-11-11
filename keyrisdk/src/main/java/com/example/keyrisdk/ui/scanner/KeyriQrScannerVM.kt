package com.example.keyrisdk.ui.scanner

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.keyrisdk.KeyriSdk
import com.example.keyrisdk.R
import com.example.keyrisdk.exception.AccountNotFoundException
import com.example.keyrisdk.exception.KeyriSdkException
import com.example.keyrisdk.exception.ServerErrorException
import com.example.keyrisdk.ui.scanner.KeyriQrScannerActivity.Companion.ARG_CUSTOM
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.launch

class KeyriQrScannerVM(private val app: Application) : AndroidViewModel(app) {

    private var initialized = false
    private var custom: String? = null

    private val loadingLD = MutableLiveData<Boolean>()
    private val messageLD = LiveEvent<String>()
    private val completedLD = LiveEvent<Boolean>()

    fun loading() = loadingLD as LiveData<Boolean>

    fun message() = messageLD as LiveData<String>

    fun completed() = completedLD as LiveData<Boolean>

    fun initialize(args: Bundle?) {
        if (initialized) return

        custom = args?.getString(ARG_CUSTOM)

        initialized = true
    }

    fun authenticate(sessionId: String) {
        viewModelScope.launch {
            loadingLD.value = true
            try {
                val session = KeyriSdk.onReadSessionId(sessionId)
                if (session.isNewUser) {
                    KeyriSdk.signup(session.username, sessionId, session.service, custom)
                } else {
                    val account =
                        KeyriSdk.accounts().firstOrNull() ?: throw AccountNotFoundException
                    KeyriSdk.login(account, sessionId, session.service, custom)
                }
                completedLD.value = true
                KeyriSdk.completeAuthWithScanner(false)
            } catch (e: Throwable) {
                Log.d("Keyri", "Authentication exception $e")
                if (e is KeyriSdkException) {
                    if (e is ServerErrorException) {
                        messageLD.value = e.errorResponse
                    } else {
                        messageLD.value = app.getString(e.errorMessage)
                    }
                } else {
                    messageLD.value = app.getString(R.string.keyri_error_general)
                }
                completedLD.value = true
                KeyriSdk.completeAuthWithScanner(true)
            }
            loadingLD.value = false
        }
    }

    fun cancelAuth() {
        completedLD.value = true
        KeyriSdk.completeAuthWithScanner(true)
    }
}
