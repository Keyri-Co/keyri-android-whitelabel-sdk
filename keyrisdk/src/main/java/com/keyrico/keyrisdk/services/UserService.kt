package com.keyrico.keyrisdk.services

import com.google.gson.JsonObject
import com.keyrico.keyrisdk.services.api.ApiService
import com.keyrico.keyrisdk.services.api.ChallengeSessionRequest
import com.keyrico.keyrisdk.services.api.ServerDataRequest

internal class UserService(
    private val apiService: ApiService,
    private val cryptoService: CryptoService,
    private val rpPublicKey: String
) {

    suspend fun approveSession(
        publicUserId: String,
        sessionId: String,
        backendPublicKey: String,
        secureCustom: String?,
        publicCustom: String?,
        salt: String,
        hash: String
    ) {
        val toEncrypt = JsonObject().also {
            it.addProperty("publicUserId", publicUserId)
            it.addProperty("timestamp", System.currentTimeMillis())
            it.addProperty("secureCustom", secureCustom)
        }.toString()

        val cipher = cryptoService.encryptHkdf(publicUserId, backendPublicKey, toEncrypt)

        val publicKey = cipher.publicKey
        val ciphertext = cipher.ciphertext
        val iv = cipher.iv

        val serverDataRequest = ServerDataRequest(publicKey, ciphertext, cipher.salt, iv)
        val request = ChallengeSessionRequest(serverDataRequest, salt, hash)

        apiService.challengeSession(sessionId, request)
    }
}
