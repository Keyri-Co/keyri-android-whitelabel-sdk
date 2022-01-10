package com.keyri.auth_with_scanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.keyrico.keyrisdk.entity.PublicAccount
import com.keyrico.keyrisdk.entity.Service

class AuthWithScannerVM : ViewModel() {

    val accountsLD: LiveData<List<PublicAccount>>
        get() = _accountsLD

    var service: Service? = null
    var sessionId: String? = null

    private val _accountsLD = MutableLiveData<List<PublicAccount>>()

    fun init(accounts: List<PublicAccount>, service: Service, sessionId: String) {
        _accountsLD.value = accounts
        this.service = service
        this.sessionId = sessionId
    }

    fun clear() {
        _accountsLD.value = null
        service = null
        sessionId = null
    }
}
