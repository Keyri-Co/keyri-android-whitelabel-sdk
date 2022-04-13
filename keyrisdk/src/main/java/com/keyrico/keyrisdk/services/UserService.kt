package com.keyrico.keyrisdk.services

import com.google.gson.JsonObject
import com.keyrico.keyrisdk.services.api.ApiService
import com.keyrico.keyrisdk.services.api.ChallengeSessionRequest

internal class UserService(
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
        val associationKey = cryptoService.getAssociationKey(publicUserId)

        val public = JsonObject().also {
            it.addProperty("publicUserId", publicUserId)
            it.addProperty("publicCustom", publicCustom)
        }

        val toEncrypt = JsonObject().also {
            it.addProperty("publicUserId", publicUserId)
            it.addProperty("timestamp", System.currentTimeMillis())
            it.addProperty("secureCustom", secureCustom)
        }.toString()

        val cipher = if (associationKey != null) {
            public.addProperty("userPublicKey", associationKey)

            cryptoService.encryptAes(toEncrypt, publicUserId, rpPublicKey)
        } else {
            val key = cryptoService.generateAssociationKey(publicUserId)

            public.addProperty("userPublicKey", key)

            cryptoService.encryptAes(toEncrypt, publicUserId, rpPublicKey)
        }

        public.addProperty("IV", cryptoService.getIV(publicUserId))

        val request = ChallengeSessionRequest(
            sessionId = sessionId,
            publicObject = public.toString(),
            cipher = cipher
        )

        apiService.challengeSession(request)
    }
}
