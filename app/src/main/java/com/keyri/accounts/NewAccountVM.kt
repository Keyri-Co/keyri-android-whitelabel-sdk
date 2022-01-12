package com.keyri.accounts

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.keyri.BuildConfig
import com.keyri.R
import com.keyrico.keyrisdk.KeyriSdk
import com.keyrico.keyrisdk.exception.KeyriSdkException
import com.keyrico.keyrisdk.services.api.AuthMobileResponse
import com.keyrico.keyrisdk.utils.LiveEvent
import kotlinx.coroutines.launch

class NewAccountVM(private val app: Application, private val keyriSdk: KeyriSdk) :
    AndroidViewModel(app) {

    private val loadingLD = MutableLiveData<Boolean>()
    private val messageLD = LiveEvent<String>()
    private val authenticatedLD = LiveEvent<AuthMobileResponse>()

    fun loading() = loadingLD as LiveData<Boolean>

    fun message() = messageLD as LiveData<String>

    fun authenticated() = authenticatedLD as LiveData<AuthMobileResponse>

    fun mobileSignup(username: String) {
        viewModelScope.launch {
            loadingLD.value = true
            try {
                authenticatedLD.value =
                    keyriSdk.mobileSignup(username, CUSTOM_DATA_SIGNUP, CUSTOM_HEADERS)
            } catch (e: Throwable) {
                if (BuildConfig.DEBUG) {
                    Log.e("Keyri", "Mobile signup exception $e")
                }

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
        private val CUSTOM_HEADERS = mapOf("TestHeader" to "TestHeaderValue")
    }
}
