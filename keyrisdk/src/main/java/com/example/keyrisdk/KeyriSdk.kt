package com.example.keyrisdk

import android.annotation.SuppressLint
import android.app.Application
import com.example.keyrisdk.di.DaggerKeyriSdkGraph
import com.example.keyrisdk.di.KeyriSdkGraph
import com.example.keyrisdk.di.KeyriSdkModule
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.entity.Service
import com.example.keyrisdk.entity.Session
import com.example.keyrisdk.exception.AccountNotFoundException
import com.example.keyrisdk.exception.NotInitializedException
import com.example.keyrisdk.exception.PermissionsException
import com.example.keyrisdk.exception.WrongConfigException
import com.example.keyrisdk.services.api.AuthMobileResponse
import com.example.keyrisdk.services.api.InitRequest
import com.example.keyrisdk.utils.Utils
import com.example.keyrisdk.utils.makeApiCall

/**
 * Keyri SDK public API
 */
object KeyriSdk {

    private var initialized = false
    private lateinit var config: KeyriConfig
    private lateinit var keyriSdkGraph: KeyriSdkGraph

    private var service: Service? = null

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

        generateDeviceIdIfNeeded()

        initialized = true
    }

    /**
     * Retrieves user session by given @sessionId
     * If session doesn't match Keyri configuration, throws WrongConfigException exception
     */
    suspend fun onReadSessionId(sessionId: String): Session {
        assertInitialized()

        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.SESSION)

        val session = makeApiCall { keyriSdkGraph.getApiService().getSession(sessionId) }.body()!!
        if (session.service.serviceId != service.serviceId) throw WrongConfigException
        return session
    }

    suspend fun signup(username: String, sessionId: String, service: Service, custom: String?) {
        assertInitialized()

        assertPermissionGranted(KeyriPermission.SIGNUP)

        keyriSdkGraph
            .getUserService()
            .signup(username, sessionId, service, custom, config.publicKey)
    }

    suspend fun login(account: PublicAccount, sessionId: String, service: Service, custom: String?) {
        assertInitialized()

        loadServiceIfNeeded()

        assertPermissionGranted(KeyriPermission.LOGIN)

        val acc = keyriSdkGraph
            .getStorageService()
            .getAccounts(service.serviceId)
            .firstOrNull { it.username == account.username } ?: throw AccountNotFoundException

        keyriSdkGraph.getUserService().login(sessionId, acc, config.publicKey, custom)
    }

    suspend fun mobileSignup(username: String, custom: String?): AuthMobileResponse {
        assertInitialized()

        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.MOBILE_SIGNUP)

        return keyriSdkGraph
            .getUserService()
            .signupMobile(username, service, config.callbackUrl, custom)
    }

    suspend fun mobileLogin(account: PublicAccount): AuthMobileResponse {
        assertInitialized()

        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.MOBILE_LOGIN)

        return keyriSdkGraph
            .getUserService()
            .loginMobile(account, service, config.callbackUrl)
    }

    suspend fun accounts(): List<PublicAccount> {
        assertInitialized()

        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.ACCOUNTS)

        return keyriSdkGraph
            .getStorageService()
            .getAccounts(service.serviceId)
            .map { PublicAccount(it.username, it.custom) }
    }

    /**
     * Checks if Keyri SDK was initialized and throws @NotInitializedException if it wasn't
     */
    private fun assertInitialized() {
        if (!initialized) throw NotInitializedException
    }

    private suspend fun loadServiceIfNeeded() {
        if (service != null) return
        val deviceId = keyriSdkGraph.getStorageService().getDeviceId() ?: throw IllegalStateException()

        val request = InitRequest(deviceId, config.appKey)
        val response = makeApiCall { keyriSdkGraph.getApiService().init(request) }.body()!!
        service = response.service
    }

    private fun generateDeviceIdIfNeeded() {
        if (keyriSdkGraph.getStorageService().getDeviceId() == null) {
            keyriSdkGraph.getStorageService().setDeviceId(Utils.getRandomString(32))
        }
    }

    @SuppressLint("DefaultLocale")
    private suspend fun assertPermissionGranted(permission: KeyriPermission) {
        /*val serviceId = service?.serviceId ?: throw IllegalStateException()

        val permissionName = permission.id
        val response = makeApiCall { keyriSdkGraph.getApiService().getPermissions(serviceId, listOf(permissionName)) }.body()!!
        val granted: Boolean = when(permission) {
            KeyriPermission.SESSION -> response.session == true
            KeyriPermission.ACCOUNTS -> response.accounts == true
            KeyriPermission.LOGIN -> response.login == true
            KeyriPermission.SIGNUP -> response.signup == true
            KeyriPermission.MOBILE_LOGIN -> response.mobileLogin == true
            KeyriPermission.MOBILE_SIGNUP -> response.mobileSignup == true
        }
        if (!granted) throw PermissionsException*/
    }

}