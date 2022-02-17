package com.keyri.accounts

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.keyri.R
import com.keyri.accounts.AccountsActivity.Companion.EXTRA_MODE
import com.keyrico.keyrisdk.KeyriSdk
import com.keyrico.keyrisdk.entity.PublicAccount
import com.keyrico.keyrisdk.exception.KeyriSdkException
import com.keyrico.keyrisdk.services.api.AuthMobileResponse
import com.keyrico.keyrisdk.utils.LiveEvent
import kotlinx.coroutines.launch

class AccountsVM(private val app: Application, private val keyriSdk: KeyriSdk) :
    AndroidViewModel(app) {

    private var initialized: Boolean = false

    lateinit var mode: AccountsMode
        private set

    private val loadingLD = MutableLiveData<Boolean>()
    private val messageLD = LiveEvent<String>()
    private val authenticatedLD = LiveEvent<AuthMobileResponse>()
    private val accountsLD = MutableLiveData<List<PublicAccount>>()

    fun loading() = loadingLD as LiveData<Boolean>

    fun message() = messageLD as LiveData<String>

    fun authenticated() = authenticatedLD as LiveData<AuthMobileResponse>

    fun accounts() = accountsLD as LiveData<List<PublicAccount>>

    fun initialize(args: Bundle?) {
        if (initialized) return

        mode = args?.getSerializable(EXTRA_MODE) as? AccountsMode ?: throw IllegalStateException()

        loadAccounts()

        initialized = true
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            loadingLD.value = true
            try {
                accountsLD.value = keyriSdk.getAccounts()
            } catch (e: Throwable) {
                Log.d("Keyri", "Failed to load accounts $e")
                if (e is KeyriSdkException) {
                    messageLD.value = app.getString(e.errorMessage)
                } else {
                    messageLD.value = app.getString(R.string.error_general)
                }
            }
            loadingLD.value = false
        }
    }

    fun processUserAccount(account: PublicAccount) {
        when (mode) {
            AccountsMode.ACCOUNTS -> return
            AccountsMode.LOGIN -> mobileLogin(account)
            AccountsMode.REMOVE -> removeAccount(account)
        }
    }

    private fun mobileLogin(account: PublicAccount) {
        viewModelScope.launch {
            loadingLD.value = true
            try {
                authenticatedLD.value = keyriSdk.directLogin(account, CUSTOM_HEADERS)
            } catch (e: Throwable) {
                Log.d("Keyri", "Mobile login exception $e")
                if (e is KeyriSdkException) {
                    messageLD.value = app.getString(e.errorMessage)
                } else {
                    messageLD.value = app.getString(R.string.error_general)
                }
            }
            loadingLD.value = false
        }
    }

    private fun removeAccount(account: PublicAccount) {
        viewModelScope.launch {
            loadingLD.value = true
            try {
                keyriSdk.removeAccount(account)
                accountsLD.postValue(keyriSdk.getAccounts())
            } catch (e: Throwable) {
                Log.d("Keyri", "Remove account exception $e")
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
        private val CUSTOM_HEADERS = mapOf("TestHeader" to "TestHeaderValue")
    }
}
