package com.keyri.accounts

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.keyrisdk.KeyriSdk
import com.example.keyrisdk.exception.KeyriSdkException
import com.example.keyrisdk.services.api.AuthMobileResponse
import com.hadilq.liveevent.LiveEvent
import com.keyri.R
import kotlinx.coroutines.launch

class NewAccountVM(private val app: Application): AndroidViewModel(app) {

    private val loadingLD = MutableLiveData<Boolean>()
    fun loading() = loadingLD as LiveData<Boolean>

    private val messageLD = LiveEvent<String>()
    fun message() = messageLD as LiveData<String>

    private val authenticatedLD = LiveEvent<AuthMobileResponse>()
    fun authenticated() = authenticatedLD as LiveData<AuthMobileResponse>

    fun mobileSignup(username: String) {
        viewModelScope.launch {
            loadingLD.value = true
            try {
                authenticatedLD.value = KeyriSdk.mobileSignup(username, CUSTOM_DATA_SIGNUP)
            } catch (e: Throwable) {
                Log.d("Keyri", "Mobile signup exception $e")
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
    }

}