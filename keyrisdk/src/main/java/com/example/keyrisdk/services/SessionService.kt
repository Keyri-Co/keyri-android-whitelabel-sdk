package com.example.keyrisdk.services

import com.example.keyrisdk.services.crypto.CryptoService
import com.example.keyrisdk.services.socket.SocketService
import com.example.keyrisdk.services.socket.messages.ValidateMessage
import com.example.keyrisdk.services.socket.messages.VerifyApproveMessage
import com.example.keyrisdk.utils.Utils

class SessionService(
    private val socketService: SocketService,
    private val cryptoService: CryptoService
) {

    private val sessions = mutableMapOf<String, String>()

    /**
     * User verification
     */
    suspend fun verifyUserSession(userId: String, sessionId: String) {
        val sessionKey = Utils.getRandomString(32)
        sessions[sessionKey] = userId

        val encryptedSessionKey = cryptoService.encryptAes(sessionKey)
        val validationMessage = ValidateMessage(sessionId, encryptedSessionKey)
        val verificationResult = socketService.sendVerificationEvent(validationMessage)

        val decryptedSessionKey = cryptoService.decryptAes(verificationResult.sessionKey)
        val verifiedUserId = sessions[decryptedSessionKey] ?: return

        val result = cryptoService.encryptCryptoBoxEasy(verifiedUserId, verificationResult.publicKey)
        val cipherText = result.first
        val nonce = result.second

        val confirmationMessage = VerifyApproveMessage(cipherText, nonce, cryptoService.getCryptoBoxPublicKey())
        socketService.sendConfirmationEvent(confirmationMessage)
    }

}