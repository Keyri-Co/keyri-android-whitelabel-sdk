package com.example.keyrisdk.services

import com.example.keyrisdk.entity.Account
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.entity.Service
import com.example.keyrisdk.exception.AccountNotFoundException
import com.example.keyrisdk.exception.MultipleAccountsNotAllowedException
import com.example.keyrisdk.services.api.ApiService
import com.example.keyrisdk.services.api.AuthMobileRequest
import com.example.keyrisdk.services.api.AuthMobileResponse
import com.example.keyrisdk.services.crypto.CryptoService
import com.example.keyrisdk.utils.Utils
import com.example.keyrisdk.utils.makeApiCall

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
        allowMultipleAccounts: Boolean
    ) {
        val hasAccounts = storageService.getAllAccounts().isNotEmpty()

        if (hasAccounts && !allowMultipleAccounts) {
            throw MultipleAccountsNotAllowedException
        }

        val account = createAccount(service.serviceId, username, custom)
        sessionService.verifyUserSession(account.userId, sessionId, true, custom)
    }

    suspend fun login(sessionId: String, account: Account, custom: String?) {
        sessionService.verifyUserSession(account.userId, sessionId, false, custom)
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
