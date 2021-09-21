package com.keyri.accounts

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.keyrisdk.KeyriSdk
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.exception.KeyriSdkException
import com.example.keyrisdk.services.api.AuthMobileResponse
import com.hadilq.liveevent.LiveEvent
import com.keyri.R
import com.keyri.accounts.AccountsActivity.Companion.EXTRA_MODE
import kotlinx.coroutines.launch

class AccountsVM(private val app: Application): AndroidViewModel(app) {

    private var initialized: Boolean = false

    lateinit var mode: AccountsMode
        private set

    private val loadingLD = MutableLiveData<Boolean>()
    fun loading() = loadingLD as LiveData<Boolean>

    private val messageLD = LiveEvent<String>()
    fun message() = messageLD as LiveData<String>

    private val authenticatedLD = LiveEvent<AuthMobileResponse>()
    fun authenticated() = authenticatedLD as LiveData<AuthMobileResponse>

    private val accountsLD = MutableLiveData<List<PublicAccount>>()
    fun accounts() = accountsLD as LiveData<List<PublicAccount>>

    fun initialize(args: Bundle?) {
        if (initialized) return

        mode = args?.getSerializable(EXTRA_MODE) as? AccountsMode
            ?: throw IllegalStateException()

        loadAccounts()

        initialized = true
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            loadingLD.value = true
            try {
                accountsLD.value = KeyriSdk.accounts()
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
        if (mode == AccountsMode.ACCOUNTS) return

        viewModelScope.launch {
            loadingLD.value = true
            try {
                authenticatedLD.value = KeyriSdk.mobileLogin(account)
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

}