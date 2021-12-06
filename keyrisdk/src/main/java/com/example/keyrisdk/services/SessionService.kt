package com.example.keyrisdk.services

import com.example.keyrisdk.services.crypto.CryptoService
import com.example.keyrisdk.services.socket.SocketService
import com.example.keyrisdk.services.socket.messages.ValidateMessage
import com.example.keyrisdk.services.socket.messages.VerificationMessage
import com.example.keyrisdk.services.socket.messages.VerifyApproveMessage
import com.example.keyrisdk.utils.Utils
import com.google.gson.Gson

class SessionService(
    private val socketService: SocketService,
    private val cryptoService: CryptoService
) {

    private val sessions = mutableMapOf<String, String>()

    /**
     * User verification
     */
    suspend fun verifyUserSession(userId: String, sessionId: String, custom: String?) {
        val sessionKey = Utils.getRandomString(32)
        sessions[sessionKey] = userId

        val encryptedSessionKey = cryptoService.encryptAes(sessionKey)
        val validationMessage = ValidateMessage(sessionId, encryptedSessionKey)
        val extraHeader = cryptoService.encryptAes(userId).take(15)

        socketService.reconnect(extraHeader)

        val verificationResult = socketService.sendVerificationEvent(validationMessage)
        val decryptedSessionKey = cryptoService.decryptAes(verificationResult.sessionKey)
        val verifiedUserId = sessions[decryptedSessionKey] ?: return
        val timestamp = System.currentTimeMillis().toString()

        val verificationDto = VerificationMessage(verifiedUserId, custom, timestamp)
        val message = Gson().toJson(verificationDto)

        val encryptedMessage = cryptoService.encryptAes(message)
        val publicKeyForVerification = cryptoService.getPublicKey()
        val initializationVector = cryptoService.getIV()
        val confirmationMessage =
            VerifyApproveMessage(encryptedMessage, publicKeyForVerification, initializationVector)

        socketService.sendConfirmationEvent(confirmationMessage)
    }
}
