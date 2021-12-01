package com.example.keyrisdk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import com.example.keyrisdk.di.DaggerKeyriSdkGraph
import com.example.keyrisdk.di.KeyriSdkGraph
import com.example.keyrisdk.di.KeyriSdkModule
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.entity.Service
import com.example.keyrisdk.entity.Session
import com.example.keyrisdk.exception.*
import com.example.keyrisdk.services.api.AuthMobileResponse
import com.example.keyrisdk.services.api.InitRequest
import com.example.keyrisdk.ui.scanner.KeyriQrScannerActivity
import com.example.keyrisdk.ui.scanner.KeyriQrScannerActivity.Companion.ARG_CUSTOM
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

        initKeys()
        generateDeviceIdIfNeeded()

        initialized = true
    }

    /**
     * Retrieves user session by given @sessionId
     * If session doesn't match Keyri configuration, throws WrongConfigException exception
     */
    @Throws(
        NotInitializedException::class,
        IllegalStateException::class,
        WrongConfigException::class
    )
    suspend fun onReadSessionId(sessionId: String): Session {
        assertInitialized()

        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.SESSION)

        val session = makeApiCall { keyriSdkGraph.getApiService().getSession(sessionId) }.body()
        if (session?.service?.serviceId != service.serviceId) throw WrongConfigException
        return session
    }

    @Throws(NotInitializedException::class, MultipleAccountsNotAllowedException::class)
    suspend fun signup(username: String, sessionId: String, service: Service, custom: String?) {
        assertInitialized()

        assertPermissionGranted(KeyriPermission.SIGNUP)

        keyriSdkGraph
            .getUserService()
            .signup(
                username,
                sessionId,
                service,
                custom,
                config.allowMultipleAccounts
            )
    }

    @Throws(AccountNotFoundException::class, NotInitializedException::class)
    suspend fun login(
        account: PublicAccount,
        sessionId: String,
        service: Service,
        custom: String?
    ) {
        assertInitialized()

        loadServiceIfNeeded()

        assertPermissionGranted(KeyriPermission.LOGIN)

        val acc = keyriSdkGraph
            .getStorageService()
            .getAccounts(service.serviceId)
            .firstOrNull { it.username == account.username } ?: throw AccountNotFoundException

        keyriSdkGraph
            .getUserService()
            .login(sessionId, acc, custom)
    }

    @Throws(
        IllegalStateException::class,
        NotInitializedException::class,
        AuthorizationException::class,
        MultipleAccountsNotAllowedException::class
    )
    suspend fun mobileSignup(
        username: String,
        custom: String?,
        extendedHeaders: Map<String, String> = emptyMap()
    ): AuthMobileResponse {
        assertInitialized()

        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.MOBILE_SIGNUP)

        return keyriSdkGraph
            .getUserService()
            .signupMobile(
                username,
                service,
                extendedHeaders,
                config.callbackUrl,
                custom,
                config.allowMultipleAccounts
            ) ?: throw AuthorizationException
    }

    @Throws(
        IllegalStateException::class,
        NotInitializedException::class,
        AuthorizationException::class
    )
    suspend fun mobileLogin(
        account: PublicAccount,
        extendedHeaders: Map<String, String> = emptyMap()
    ): AuthMobileResponse {
        assertInitialized()

        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.MOBILE_LOGIN)

        return keyriSdkGraph
            .getUserService()
            .loginMobile(account, service, extendedHeaders, config.callbackUrl)
            ?: throw AuthorizationException
    }

    @Throws(IllegalStateException::class, NotInitializedException::class)
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

    @Throws(IllegalStateException::class, NotInitializedException::class)
    suspend fun removeAccount(account: PublicAccount) {
        assertInitialized()

        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.ACCOUNTS)

        keyriSdkGraph
            .getStorageService()
            .removeAccount(service.serviceId, account)
    }

    /**
     * Checks if Keyri SDK was initialized and throws @NotInitializedException if it wasn't
     */
    @Throws(NotInitializedException::class)
    private fun assertInitialized() {
        if (!initialized) throw NotInitializedException
    }

    @Throws(IllegalStateException::class)
    private suspend fun loadServiceIfNeeded() {
        if (service != null) return
        val deviceId =
            keyriSdkGraph.getStorageService().getDeviceId() ?: throw IllegalStateException()

        val request = InitRequest(deviceId, config.appKey)
        val response = makeApiCall { keyriSdkGraph.getApiService().init(request) }.body()
        service = response?.service
    }

    private fun initKeys() {
        keyriSdkGraph.getCryptoService().generateECDHSecret(config.publicKey)
    }

    private fun generateDeviceIdIfNeeded() {
        if (keyriSdkGraph.getStorageService().getDeviceId() == null) {
            keyriSdkGraph.getStorageService().setDeviceId(Utils.getRandomString(32))
        }
    }

    @SuppressLint("DefaultLocale")
    @Throws(PermissionsException::class)
    private suspend fun assertPermissionGranted(permission: KeyriPermission) {
        /*val serviceId = service?.serviceId ?: throw IllegalStateException()

        val permissionName = permission.id
        val response = makeApiCall { keyriSdkGraph.getApiService().getPermissions(serviceId, listOf(permissionName)) }.body()
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

    fun authWithScanner(activity: Activity, custom: String?, callbacks: QrAuthCallbacks) {
        if (qrAuthCallbacks != null) return
        qrAuthCallbacks = callbacks

        val intent = Intent(activity, KeyriQrScannerActivity::class.java)
            .putExtra(ARG_CUSTOM, custom)
        activity.startActivity(intent)
    }

    internal fun completeAuthWithScanner(isFailed: Boolean) {
        if (isFailed) {
            qrAuthCallbacks?.onFailed?.invoke()
        } else {
            qrAuthCallbacks?.onCompleted?.invoke()
        }
        qrAuthCallbacks = null
    }

    class QrAuthCallbacks(
        val onCompleted: () -> Unit,
        val onFailed: () -> Unit
    )

    private var qrAuthCallbacks: QrAuthCallbacks? = null

}