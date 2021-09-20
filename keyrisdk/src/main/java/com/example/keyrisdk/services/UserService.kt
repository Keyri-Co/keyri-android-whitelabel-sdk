package com.example.keyrisdk.services

import com.example.keyrisdk.entity.Account
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.entity.Service
import com.example.keyrisdk.exception.AccountNotFoundException
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

    suspend fun signup(username: String, sessionId: String, service: Service, custom: String?) {
        val account = createAccount(service.serviceId, username, custom)
        sessionService.verifyUserSession(account.userId, sessionId)
    }

    suspend fun login(sessionId: String, account: Account) {
        sessionService.verifyUserSession(account.userId, sessionId)
    }

    suspend fun signupMobile(
        username: String,
        service: Service,
        callbackUrl: String,
        custom: String?
    ): AuthMobileResponse {
        val account = createAccount(service.serviceId, username, custom)

        val request = AuthMobileRequest(account.userId, username)
        return makeApiCall { apiService.authMobile(callbackUrl, request) }.body()!!
    }

    suspend fun loginMobile(
        publicAccount: PublicAccount,
        service: Service,
        callbackUrl: String
    ): AuthMobileResponse {
        val account = storageService
            .getAccounts(service.serviceId)
            .find { it.username == publicAccount.username }
            ?: throw AccountNotFoundException

        val request = AuthMobileRequest(account.userId, account.username)
        return makeApiCall { apiService.authMobile(callbackUrl, request) }.body()!!
    }

    private fun createAccount(serviceId: String, username: String, custom: String?) =
        Account(generateUserId(), serviceId, username, custom).also {
            storageService.addAccount(it)
        }

    private fun generateUserId() =
        cryptoService.encryptAes(Utils.getRandomString(32))

}