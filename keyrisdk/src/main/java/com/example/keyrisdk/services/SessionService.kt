package com.example.keyrisdk.services

import com.example.keyrisdk.services.crypto.CryptoService
import com.example.keyrisdk.services.socket.SocketService
import com.example.keyrisdk.services.socket.messages.ValidateMessage
import com.example.keyrisdk.services.socket.messages.VerifyApproveMessage
import com.example.keyrisdk.utils.Utils
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class SessionService(
    private val socketService: SocketService,
    private val cryptoService: CryptoService
) {

    private val sessions = mutableMapOf<String, String>()

    /**
     * User verification
     */
    suspend fun verifyUserSession(
        userId: String,
        sessionId: String,
        publicKey: String?,
        usePublicKey: Boolean,
        custom: String?
    ) {
        val sessionKey = Utils.getRandomString(32)
        sessions[sessionKey] = userId

        val encryptedSessionKey = cryptoService.encryptAes(sessionKey)
        val validationMessage = ValidateMessage(sessionId, encryptedSessionKey)
        val extraHeader = cryptoService.encryptAes(userId).take(15)

        socketService.reconnect(extraHeader)

        val verificationResult = socketService.sendVerificationEvent(validationMessage)
        val decryptedSessionKey = cryptoService.decryptAes(verificationResult.sessionKey)
        val verifiedUserId = sessions[decryptedSessionKey] ?: return

        val verificationDto =
            VerificationMessage(verifiedUserId, custom, System.currentTimeMillis().toString())
        val message = Gson().toJson(verificationDto)

        val targetPublicKey =
            publicKey ?: verificationResult.publicKey ?: throw IllegalStateException()
        val encryptedMessage = cryptoService.encryptSeal(message, targetPublicKey)
        val signedMessage = cryptoService.createSignature(message)

        // TODO Check Android Crypto (is packages supported)
        // TODO Check Security, behavior changes
        // TODO If we can do it -> change crypto service

        // TODO Generate secret key for AES
        // TODO And save to Keystore

        val publicKeyForVerification =
            if (usePublicKey) cryptoService.getCryptoBoxPublicKey() else null
        val confirmationMessage =
            VerifyApproveMessage(encryptedMessage, signedMessage, publicKeyForVerification)

        socketService.sendConfirmationEvent(confirmationMessage)
    }

    data class VerificationMessage(

        @SerializedName("userId")
        val userId: String,

        @SerializedName("custom")
        val custom: String?,

        @SerializedName("timestamp")
        val timestamp: String,
    )
}
