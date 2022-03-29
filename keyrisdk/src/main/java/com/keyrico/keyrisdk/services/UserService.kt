package com.keyrico.keyrisdk.services

import com.keyrico.keyrisdk.entity.Account
import com.keyrico.keyrisdk.entity.PublicAccount
import com.keyrico.keyrisdk.entity.session.service.Service
import com.keyrico.keyrisdk.exception.AccountNotFoundException
import com.keyrico.keyrisdk.exception.MultipleAccountsNotAllowedException
import com.keyrico.keyrisdk.services.api.ApiService
import com.keyrico.keyrisdk.services.api.AuthMobileRequest
import com.keyrico.keyrisdk.services.api.AuthMobileResponse
import com.keyrico.keyrisdk.services.crypto.CryptoService
import com.keyrico.keyrisdk.utils.Utils
import com.keyrico.keyrisdk.utils.makeApiCall

class UserService(
    private val storageService: StorageService,
    private val sessionService: SessionService,
    private val apiService: ApiService,
    private val cryptoService: CryptoService
) {

    suspend fun signup(
        username: String,
        sessionId: String,
        service: Service,
        custom: String?,
        allowMultipleAccounts: Boolean,
        isTestEnv: Boolean
    ) {
        val hasAccounts = storageService.getAllAccounts().isNotEmpty()

        if (hasAccounts && !allowMultipleAccounts) {
            throw MultipleAccountsNotAllowedException
        }

        val account = createAccount(service.serviceId, username, custom)
        sessionService.verifyUserSession(true, account.userId, sessionId, custom, isTestEnv)
    }

    suspend fun login(
        sessionId: String,
        account: Account,
        custom: String?,
        isTestEnv: Boolean
    ) {
        sessionService.verifyUserSession(false, account.userId, sessionId, custom, isTestEnv)
    }

    suspend fun whitelabelAuth(sessionId: String, custom: String) {
        sessionService.whitelabelAuth(sessionId, custom)
    }

    suspend fun signupMobile(
        username: String,
        service: Service,
        extendedHeaders: Map<String, String>,
        callbackUrl: String,
        custom: String?,
        allowMultipleAccounts: Boolean
    ): AuthMobileResponse? {
        val hasAccounts = storageService.getAllAccounts().isNotEmpty()

        if (hasAccounts && !allowMultipleAccounts) {
            throw MultipleAccountsNotAllowedException
        }

        val account = createAccount(service.serviceId, username, custom)
        val request = AuthMobileRequest(account.userId, username, cryptoService.getPublicKey())

        return makeApiCall { apiService.authMobile(extendedHeaders, callbackUrl, request) }.body()
    }

    suspend fun loginMobile(
        publicAccount: PublicAccount,
        service: Service,
        extendedHeaders: Map<String, String>,
        callbackUrl: String
    ): AuthMobileResponse? {
        val account = storageService
            .getAccounts(service.serviceId)
            .find { it.username == publicAccount.username } ?: throw AccountNotFoundException

        val request =
            AuthMobileRequest(account.userId, account.username, cryptoService.getPublicKey())

        return makeApiCall { apiService.authMobile(extendedHeaders, callbackUrl, request) }.body()
    }

    private suspend fun createAccount(serviceId: String, username: String, custom: String?) =
        Account(generateUserId(), serviceId, username, custom).also {
            storageService.addAccount(it)
        }

    private fun generateUserId() = cryptoService.encryptAes(Utils.getRandomString(32))
}
