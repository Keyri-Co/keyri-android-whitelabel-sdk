package com.example.keyrisdk

import android.app.Application
import com.example.keyrisdk.di.DaggerKeyriSdkGraph
import com.example.keyrisdk.di.KeyriSdkGraph
import com.example.keyrisdk.di.KeyriSdkModule
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.entity.Service
import com.example.keyrisdk.entity.Session
import com.example.keyrisdk.exception.AccountNotFoundException
import com.example.keyrisdk.exception.NotInitializedException
import com.example.keyrisdk.exception.WrongConfigException
import com.example.keyrisdk.services.api.AuthMobileResponse
import com.example.keyrisdk.utils.makeApiCall

/**
 * Keyri SDK public API
 */
object KeyriSdk {

    private var initialized = false
    private lateinit var config: KeyriConfig
    private lateinit var keyriSdkGraph: KeyriSdkGraph

    internal val app
        get() = keyriSdkGraph.getContext()

    /**
     * Initializes Keyri SDK.
     * Should be called before using other Keyri SDK api methods.
     */
    fun initialize(app: Application, config: KeyriConfig) {

        if (initialized) return

        this.config = config
        keyriSdkGraph = DaggerKeyriSdkGraph.builder()
            .keyriSdkModule(KeyriSdkModule(app))
            .build()

        initialized = true
    }

    /**
     * Retrieves user session by given @sessionId
     * If session doesn't match Keyri configuration, throws WrongConfigException exception
     */
    suspend fun onReadSessionId(sessionId: String): Session {
        assertInitialized()

        val session = makeApiCall { keyriSdkGraph.getApiService().getSession(sessionId) }.body()!!
        if (session.service.serviceId != config.id) throw WrongConfigException
        return session
    }

    suspend fun signup(username: String, sessionId: String, service: Service, custom: String?) {
        assertInitialized()

        keyriSdkGraph
            .getUserService()
            .signup(username, sessionId, service, custom)
    }

    suspend fun login(account: PublicAccount, sessionId: String, service: Service, custom: String?) {
        assertInitialized()

        val acc = keyriSdkGraph
            .getStorageService()
            .getAccounts(service.serviceId)
            .firstOrNull { it.username == account.username } ?: throw AccountNotFoundException

        keyriSdkGraph.getUserService().login(sessionId, acc)
    }

    suspend fun mobileSignup(username: String, custom: String?): AuthMobileResponse {
        assertInitialized()

        val service = Service(config.id, config.name, config.logoUrl)
        return keyriSdkGraph
            .getUserService()
            .signupMobile(username, service, config.callbackUrl, custom)
    }

    suspend fun mobileLogin(account: PublicAccount): AuthMobileResponse {
        assertInitialized()

        val service = Service(config.id, config.name, config.logoUrl)
        return keyriSdkGraph
            .getUserService()
            .loginMobile(account, service, config.callbackUrl)
    }

    fun accounts(): List<PublicAccount> {
        assertInitialized()

        return keyriSdkGraph
            .getStorageService()
            .getAccounts(config.id)
            .map { PublicAccount(it.username, it.custom) }
    }

    /**
     * Checks if Keyri SDK was initialized and throws @NotInitializedException if it wasn't
     */
    private fun assertInitialized() {
        if (!initialized) throw NotInitializedException
    }

}