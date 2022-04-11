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
    private val apiService: ApiService,
    private val cryptoService: CryptoService,
    private val rpPublicKey: String
) {

    suspend fun challengeSession(
        publicUserId: String,
        sessionId: String,
        secureCustom: String?,
        publicCustom: String?
    ) {
        // TODO Add Impl
    }

    suspend fun whitelabelAuth(sessionId: String, custom: String, externalKey: String? = null) {
        // TODO Add Impl
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

        val account = createAccount(service.id, username, custom)
        val request = AuthMobileRequest(
            account.userId,
            username,
            cryptoService.getAssociationKey(account.userId)
        )

        return makeApiCall { apiService.authMobile(extendedHeaders, callbackUrl, request) }.body()
    }

    suspend fun loginMobile(
        publicAccount: PublicAccount,
        service: Service,
        extendedHeaders: Map<String, String>,
        callbackUrl: String
    ): AuthMobileResponse? {
        val account = storageService
            .getAccounts(service.id)
            .find { it.username == publicAccount.username } ?: throw AccountNotFoundException

        val request =
            AuthMobileRequest(
                account.userId,
                account.username,
                cryptoService.getAssociationKey(account.userId)
            )

        return makeApiCall { apiService.authMobile(extendedHeaders, callbackUrl, request) }.body()
    }

    private suspend fun createAccount(
        serviceId: String,
        username: String,
        custom: String?
    ): Account {
        val publicUserId = Utils.getRandomString(32)

        cryptoService.generateAssociationKey(publicUserId, rpPublicKey)

        return Account(generateUserId(publicUserId), serviceId, username, custom).also {
            storageService.addAccount(it, publicUserId)
        }
    }

    private fun generateUserId(publicUserId: String) =
        cryptoService.encryptAes(publicUserId, publicUserId)
}
