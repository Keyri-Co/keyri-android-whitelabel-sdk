package com.example.keyrisdk.ui.choose_account

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.keyrisdk.BuildConfig
import com.example.keyrisdk.KeyriSdk
import com.example.keyrisdk.R
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.exception.KeyriSdkException
import com.example.keyrisdk.exception.ServerErrorException
import com.example.keyrisdk.utils.LiveEvent
import kotlinx.coroutines.launch

class KeyriQrChooseAccountVM(private val app: Application) : AndroidViewModel(app) {

    private val accountsLD = MutableLiveData<List<PublicAccount>>()
    private val loadingLD = MutableLiveData<Boolean>()
    private val messageLD = LiveEvent<String>()

    fun accounts() = accountsLD as LiveData<List<PublicAccount>>

    fun loading() = loadingLD as LiveData<Boolean>

    fun message() = messageLD as LiveData<String>

    fun getAccounts() {
        viewModelScope.launch {
            loadingLD.value = true

            try {
                accountsLD.value = KeyriSdk.accounts()
            } catch (e: Throwable) {
                if (BuildConfig.DEBUG) {
                    Log.e("Keyri", "Authentication exception $e")
                }

                messageLD.value = if (e is KeyriSdkException) {
                    if (e is ServerErrorException) {
                        e.errorResponse
                    } else {
                        app.getString(e.errorMessage)
                    }
                } else {
                    app.getString(R.string.keyri_error_general)
                }
            }

            loadingLD.value = false
        }
    }
}
