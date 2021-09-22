package com.example.keyrisdk.services.socket

import android.util.Log
import com.example.keyrisdk.exception.NetworkException
import com.example.keyrisdk.services.socket.messages.ValidateMessage
import com.example.keyrisdk.services.socket.messages.VerifyApproveMessage
import com.example.keyrisdk.services.socket.messages.VerifyRequestMessage
import io.socket.client.IO
import io.socket.client.Socket.EVENT_CONNECT
import io.socket.client.Socket.EVENT_CONNECT_ERROR
import io.socket.client.SocketOptionBuilder
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class SocketService(url: String) {

    private val socket = IO.socket(
        url, SocketOptionBuilder
            .builder()
            .setTransports(arrayOf(WebSocket.NAME))
            .build()
    )

    /**
     * Establishes socket connection if not already connected
     */
    private suspend fun connectIfNeeded() {
        if (socket.connected()) return

        return withTimeout(SOCKET_TIMEOUT) {
            suspendCoroutine { continuation ->
                Log.d(TAG, "Connecting...")

                socket.disconnect()
                socket.connect()

                socket.on(EVENT_CONNECT) {
                    Log.d(TAG, "Connected")
                    try {
                        continuation.resume(Unit)
                    } catch (e: Throwable) {
                        /* do nothing */
                    }
                }
                socket.on(EVENT_CONNECT_ERROR) {
                    Log.d(TAG, "Failed to connect")
                    continuation.resumeWithException(NetworkException)
                }
            }
        }
    }

    suspend fun sendVerificationEvent(message: ValidateMessage): VerifyRequestMessage {
        connectIfNeeded()
        return withTimeout(SOCKET_TIMEOUT) {
            suspendCoroutine { continuation ->
                Log.d(TAG, "Sending verification request")

                socket.emit(SocketAction.SESSION_VALIDATE.name, message.toSocketData())
                socket.on(SocketAction.SESSION_VERIFY_REQUEST.name) { data: Array<out Any>? ->
                    Log.d(TAG, "verification response received")

                    val payload = data?.first() as? JSONObject
                    if (payload != null) {
                        parseVerificationRequest(payload)?.let { continuation.resume(it) }
                    }
                }
            }
        }
    }

    private fun parseVerificationRequest(data: JSONObject): VerifyRequestMessage? {
        val action = data["action"] as String
        if (action != SocketAction.SESSION_VERIFY_REQUEST.name) return null

        val publicKey = data["publicKey"] as? String ?: return null
        val sessionKey = data["sessionKey"] as? String ?: return null
        return VerifyRequestMessage(publicKey, sessionKey)
    }

    suspend fun sendConfirmationEvent(message: VerifyApproveMessage) {
        connectIfNeeded()
        withTimeout(SOCKET_TIMEOUT) {
            suspendCoroutine<Void?> { continuation ->
                Log.d(TAG, "Sending confirmation")

                socket.emit(CONFIRMATION_EVENT_NAME, message.toSocketData())
                continuation.resume(null)
            }
        }
    }

    companion object {
        private const val TAG = "Keyri > SocketService"
        private const val SOCKET_TIMEOUT = 5000L
        private const val CONFIRMATION_EVENT_NAME = "message"
    }

}