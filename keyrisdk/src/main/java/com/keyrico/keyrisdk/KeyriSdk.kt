package com.keyrico.keyrisdk

import android.annotation.SuppressLint
import android.content.Context
import com.keyrico.keyrisdk.entity.PublicAccount
import com.keyrico.keyrisdk.entity.Service
import com.keyrico.keyrisdk.entity.Session
import com.keyrico.keyrisdk.exception.AccountNotFoundException
import com.keyrico.keyrisdk.exception.AuthorizationException
import com.keyrico.keyrisdk.exception.MultipleAccountsNotAllowedException
import com.keyrico.keyrisdk.exception.NotInitializedException
import com.keyrico.keyrisdk.exception.PermissionsException
import com.keyrico.keyrisdk.exception.WrongConfigException
import com.keyrico.keyrisdk.services.api.AuthMobileResponse
import com.keyrico.keyrisdk.services.api.InitRequest
import com.keyrico.keyrisdk.utils.Utils
import com.keyrico.keyrisdk.utils.makeApiCall

/**
 * Keyri SDK public API.
 */
class KeyriSdk(context: Context, private val config: KeyriConfig) {

    private var service: Service? = null
    private val keyriSdkModule = KeyriSdkModule(context)

    init {
        initKeys()
        generateDeviceIdIfNeeded()
    }

    /**
     * Retrieves user session by given @sessionId.
     * If session doesn't match Keyri configuration, throws WrongConfigException exception
     */
    @Throws(
        NotInitializedException::class,
        IllegalStateException::class,
        WrongConfigException::class
    )
    suspend fun onReadSessionId(sessionId: String): Session {
        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.SESSION)

        val session =
            makeApiCall { keyriSdkModule.provideApiService().getSession(sessionId) }.body()
        if (session?.service?.serviceId != service.serviceId) throw WrongConfigException
        return session
    }

    /**
     * Create new user for Desktop agent. If @allowMultipleAccounts is false,
     * throws MultipleAccountsNotAllowedException exception.
     * Must be called after [onReadSessionId], if @isNewUser is true.
     *
     * @username for new user.
     * @sessionId scanned sessionId.
     * @service obtained Session from [onReadSessionId].
     * @custom custom argument.
     */
    @Throws(NotInitializedException::class, MultipleAccountsNotAllowedException::class)
    suspend fun signup(username: String, sessionId: String, service: Service, custom: String?) {
        assertPermissionGranted(KeyriPermission.SIGNUP)

        keyriSdkModule
            .provideUserService()
            .signup(
                username,
                sessionId,
                service,
                custom,
                config.allowMultipleAccounts
            )
    }

    /**
     * Login user for Desktop agent.
     * Must be called after [onReadSessionId], if @isNewUser is false.
     *
     * @account pass created earlier publicAccount.
     * @sessionId scanned sessionId.
     * @service obtained Session from [onReadSessionId].
     * @custom custom argument.
     */
    @Throws(AccountNotFoundException::class, NotInitializedException::class)
    suspend fun login(
        account: PublicAccount,
        sessionId: String,
        service: Service,
        custom: String?
    ) {
        loadServiceIfNeeded()

        assertPermissionGranted(KeyriPermission.LOGIN)

        val acc = keyriSdkModule
            .provideStorageService()
            .getAccounts(service.serviceId)
            .firstOrNull { it.username == account.username } ?: throw AccountNotFoundException

        keyriSdkModule
            .provideUserService()
            .login(sessionId, acc, custom)
    }

    /**
     * Create new user on mobile device. If @allowMultipleAccounts is false,
     * throws MultipleAccountsNotAllowedException exception.
     *
     * @username for new user.
     * @custom custom argument.
     * @extendedHeaders custom headers.
     */
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
        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.MOBILE_SIGNUP)

        return keyriSdkModule
            .provideUserService()
            .signupMobile(
                username,
                service,
                extendedHeaders,
                config.callbackUrl,
                custom,
                config.allowMultipleAccounts
            ) ?: throw AuthorizationException
    }

    /**
     * Login user on mobile device.
     *
     * @account pass created earlier publicAccount.
     * @extendedHeaders custom headers.
     */
    @Throws(
        IllegalStateException::class,
        NotInitializedException::class,
        AuthorizationException::class
    )
    suspend fun mobileLogin(
        account: PublicAccount,
        extendedHeaders: Map<String, String> = emptyMap()
    ): AuthMobileResponse {
        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.MOBILE_LOGIN)

        return keyriSdkModule
            .provideUserService()
            .loginMobile(account, service, extendedHeaders, config.callbackUrl)
            ?: throw AuthorizationException
    }

    /**
     * Retrieves all public accounts on device.
     */
    @Throws(IllegalStateException::class, NotInitializedException::class)
    suspend fun accounts(): List<PublicAccount> {
        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.ACCOUNTS)

        return keyriSdkModule
            .provideStorageService()
            .getAccounts(service.serviceId)
            .map { PublicAccount(it.username, it.custom) }
    }

    /**
     * Remove public account from database.
     *
     * @account public account to remove.
     */
    @Throws(IllegalStateException::class, NotInitializedException::class)
    suspend fun removeAccount(account: PublicAccount) {
        loadServiceIfNeeded()
        val service = this.service ?: throw IllegalStateException()

        assertPermissionGranted(KeyriPermission.ACCOUNTS)

        keyriSdkModule
            .provideStorageService()
            .removeAccount(service.serviceId, account)
    }

    @Throws(IllegalStateException::class)
    private suspend fun loadServiceIfNeeded() {
        if (service != null) return
        val deviceId =
            keyriSdkModule.provideStorageService().getDeviceId() ?: throw IllegalStateException()

        val request = InitRequest(deviceId, config.appKey)
        val response = makeApiCall { keyriSdkModule.provideApiService().init(request) }.body()
        service = response?.service
    }

    private fun initKeys() {
        keyriSdkModule.provideCryptoService().generateSecretKey(config.publicKey)
    }

    private fun generateDeviceIdIfNeeded() {
        if (keyriSdkModule.provideStorageService().getDeviceId() == null) {
            keyriSdkModule.provideStorageService().setDeviceId(Utils.getRandomString(32))
        }
    }

    @SuppressLint("DefaultLocale")
    @Throws(PermissionsException::class)
    private fun assertPermissionGranted(permission: KeyriPermission) {
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
}
