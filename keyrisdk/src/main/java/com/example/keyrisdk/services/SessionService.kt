package com.example.keyrisdk.services

import com.example.keyrisdk.exception.AuthorizationException
import com.example.keyrisdk.services.crypto.CryptoService
import com.example.keyrisdk.services.socket.SocketService
import com.example.keyrisdk.services.socket.messages.ValidateMessage
import com.example.keyrisdk.services.socket.messages.VerificationMessage
import com.example.keyrisdk.services.socket.messages.VerifyApproveMessage
import com.example.keyrisdk.utils.Utils
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull

class SessionService(
    private val socketService: SocketService,
    private val cryptoService: CryptoService
) {

    private val sessions = mutableMapOf<String, String>()

    /**
     * Function for user session verification.
     *
     * @userId user id to verify.
     * @sessionId id of the session to verify.
     * @custom custom argument.
     */
    suspend fun verifyUserSession(
        isNewUser: Boolean,
        userId: String,
        sessionId: String,
        custom: String?
    ) {
        val sessionKey = Utils.getRandomString(32)
        sessions[sessionKey] = userId

        val encryptedSessionKey = cryptoService.encryptAes(sessionKey)
        val validationMessage = ValidateMessage(sessionId, encryptedSessionKey)
        val extraHeader = cryptoService.encryptAes(userId).take(15)

        socketService.reconnect(extraHeader)
        socketService.sendVerificationEvent(validationMessage)

        withTimeoutOrNull(SOCKET_TIMEOUT) {
            val verificationResult = socketService.verifyMessageChannel.consumeAsFlow().first()

            val decryptedSessionKey =
                cryptoService.decryptAes(verificationResult.getOrThrow().sessionKey)
            val verifiedUserId = sessions[decryptedSessionKey] ?: throw AuthorizationException
            val timestamp = System.currentTimeMillis().toString()

            val verificationDto = VerificationMessage(verifiedUserId, custom, timestamp)
            val message = Gson().toJson(verificationDto)

            val encryptedMessage = cryptoService.encryptAes(message)
            val publicKeyForVerification = if (isNewUser) cryptoService.getPublicKey() else null
            val initializationVector = cryptoService.getIV()
            val confirmationMessage =
                VerifyApproveMessage(
                    encryptedMessage,
                    publicKeyForVerification,
                    initializationVector
                )

            socketService.sendConfirmationEvent(confirmationMessage)
        } ?: throw AuthorizationException
    }

    companion object {
        private const val SOCKET_TIMEOUT = 5000L
    }
}
