package com.keyri.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keyrico.keyrisdk.KeyriSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(private val keyriSdk: KeyriSdk) : ViewModel() {

    private val loadingLD = MutableLiveData<Boolean>()

    fun loading() = loadingLD as LiveData<Boolean>

    fun onReadSessionId(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loadingLD.postValue(true)

            val session = keyriSdk.handleSessionId(sessionId)

            keyriSdk.challengeSession(
                "mocked-public-user-id",
                session.sessionId,
                "secure custom",
                "public custom"
            )

            loadingLD.postValue(false)
        }
    }

    // TODO Remove
    fun test() {
        viewModelScope.launch(Dispatchers.IO) {
            keyriSdk.challengeSession(
                "mocked-public-user-id",
               "mocked-session-id",
                "secure custom",
                "public custom"
            )
        }
    }
}
