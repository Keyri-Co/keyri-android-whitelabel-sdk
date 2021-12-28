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
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.entity.Service
import com.example.keyrisdk.exception.AccountNotFoundException
import com.example.keyrisdk.exception.KeyriSdkException
import com.example.keyrisdk.exception.MultipleAccountsNotAllowedException
import com.example.keyrisdk.exception.ServerErrorException
import com.example.keyrisdk.ui.scanner.KeyriQrScannerActivity.Companion.ARG_CUSTOM
import com.example.keyrisdk.utils.LiveEvent
import kotlinx.coroutines.launch

class KeyriQrScannerVM(private val app: Application, private val keyriSdk: KeyriSdk) :
    AndroidViewModel(app) {

    private var initialized = false
    private var custom: String? = null

    private val loadingLD = MutableLiveData<Boolean>()
    private val messageLD = LiveEvent<String>()
    private val completedLD = LiveEvent<Boolean>()
    private val accountAlreadyExistsLD = LiveEvent<String>()
    private val chooseAccountLD = LiveEvent<String>()

    fun loading() = loadingLD as LiveData<Boolean>

    fun message() = messageLD as LiveData<String>

    fun completed() = completedLD as LiveData<Boolean>

    fun accountAlreadyExists() = accountAlreadyExistsLD as LiveData<String>

    fun chooseAccount() = chooseAccountLD as LiveData<String>

    fun initialize(args: Bundle?) {
        if (initialized) return

        custom = args?.getString(ARG_CUSTOM)

        initialized = true
    }

    fun authenticate(sessionId: String, account: PublicAccount? = null) {
        viewModelScope.launch {
            loadingLD.value = true
            try {
                val session = keyriSdk.onReadSessionId(sessionId)

                if (session.isNewUser) {
                    try {
                        keyriSdk.signup(session.username, sessionId, session.service, custom)
                        completeAuthWithScanner(isFailed = false)
                    } catch (e: Throwable) {
                        if (e is MultipleAccountsNotAllowedException) {
                            accountAlreadyExistsLD.value = sessionId
                        } else {
                            throw e
                        }
                    }
                } else {
                    val accounts = keyriSdk.accounts()

                    when {
                        accounts.isEmpty() -> throw AccountNotFoundException
                        account != null -> authAccount(account, sessionId, session.service)
                        accounts.size == 1 -> authAccount(
                            accounts.first(),
                            sessionId,
                            session.service
                        )
                        else -> chooseAccountLD.value = sessionId
                    }
                }
            } catch (e: Throwable) {
                Log.d("Keyri", "Authentication exception $e")

                if (e is KeyriSdkException) {
                    when (e) {
                        is ServerErrorException -> messageLD.value = e.errorResponse
                        is AccountNotFoundException -> messageLD.value =
                            app.getString(R.string.keyri_no_accounts_create_one)
                        else -> messageLD.value = app.getString(e.errorMessage)
                    }
                } else {
                    messageLD.value = app.getString(R.string.keyri_error_general)
                }

                completeAuthWithScanner(isFailed = true)
            }

            loadingLD.value = false
        }
    }

    fun removeExistingAccountAndInitNewSession(sessionId: String) {
        viewModelScope.launch {
            keyriSdk.accounts().firstOrNull()?.let { account ->
                keyriSdk.removeAccount(account)
                authenticate(sessionId)
            }
        }
    }

    fun cancelAuth() {
        completeAuthWithScanner(isFailed = true)
    }

    private fun completeAuthWithScanner(isFailed: Boolean) {
        completedLD.value = true
        keyriSdk.completeAuthWithScanner(isFailed)
    }

    private suspend fun authAccount(account: PublicAccount, sessionId: String, service: Service) {
        keyriSdk.login(account, sessionId, service, custom)
        completeAuthWithScanner(isFailed = false)
    }
}
