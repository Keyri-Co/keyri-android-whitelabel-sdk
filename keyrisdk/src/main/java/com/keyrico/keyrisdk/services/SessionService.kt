package com.keyrico.keyrisdk.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.keyrico.keyrisdk.exception.AuthorizationException
import com.keyrico.keyrisdk.services.crypto.CryptoService
import com.keyrico.keyrisdk.services.socket.SocketService
import com.keyrico.keyrisdk.services.socket.messages.CustomAuthChallengeMessage
import com.keyrico.keyrisdk.services.socket.messages.ValidateMessage
import com.keyrico.keyrisdk.services.socket.messages.VerificationMessage
import com.keyrico.keyrisdk.services.socket.messages.VerifyApproveMessage
import com.keyrico.keyrisdk.utils.Utils
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class SessionService(
    private val socketService: SocketService,
    private val cryptoService: CryptoService
) {

    private val sessions = mutableMapOf<String, String>()

    /**
     * Function for user session verification.
     *
     * @isNewUser represents is it signup or login.
     * @userId user id to verify.
     * @sessionId id of the session to verify.
     * @custom custom argument.
     */
    suspend fun verifyUserSession(
        isNewUser: Boolean,
        userId: String,
        sessionId: String,
        custom: String?,
        isTestEnv: Boolean
    ) {
        val sessionKey = Utils.getRandomString(32)
        sessions[sessionKey] = userId

        val encryptedSessionKey = cryptoService.encryptAes(sessionKey)
        val validationMessage = ValidateMessage(sessionId, encryptedSessionKey, isTestEnv)
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

    suspend fun whitelabelAuth(sessionId: String, custom: String) {
        val sessionKey = Utils.getRandomString(32)
        val encryptedSessionKey = cryptoService.encryptAes(sessionKey)
        val validationMessage = ValidateMessage(sessionId, encryptedSessionKey, false)

        socketService.reconnect()
        socketService.sendVerificationEvent(validationMessage)

        withTimeoutOrNull(SOCKET_TIMEOUT) {
            socketService.verifyMessageChannel.consumeAsFlow().first()

            val timestamp = System.currentTimeMillis().toString()
            val verificationDto = VerificationMessage(null, custom, timestamp)
            val message = Gson().toJson(verificationDto)

            val encryptedMessage = cryptoService.encryptAes(message)
            val publicKeyForVerification = cryptoService.getPublicKey()
            val initializationVector = cryptoService.getIV()

            val confirmationMessage = VerifyApproveMessage(
                encryptedMessage,
                publicKeyForVerification,
                initializationVector
            )

            socketService.sendConfirmationEvent(confirmationMessage)
        } ?: throw AuthorizationException
    }

    suspend fun customAuthChallengeRequest(
        sessionId: String,
        custom: String,
        extensionKey: String
    ) {
        val timestamp = System.currentTimeMillis()

        val message = JsonObject().also {
            it.addProperty("timestamp", timestamp)
            it.addProperty("custom", custom)
        }.toString()

        val iv = cryptoService.getIV()
        val cipher = cryptoService.encryptAes(message, extensionKey)

        val customAuthChallengeMessage = CustomAuthChallengeMessage(
            sessionId,
            timestamp.toString(),
            cipher,
            iv ?: throw AuthorizationException,
            true
        )

        withTimeoutOrNull(SOCKET_TIMEOUT) {
            socketService.reconnect()
            socketService.sendCustomAuthChallengeEvent(customAuthChallengeMessage)
        } ?: throw AuthorizationException
    }

    companion object {
        private const val SOCKET_TIMEOUT = 5000L
    }
}
