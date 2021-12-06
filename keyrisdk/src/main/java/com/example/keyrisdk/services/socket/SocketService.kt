package com.example.keyrisdk.services.socket

import android.util.Log
import com.example.keyrisdk.exception.NetworkException
import com.example.keyrisdk.services.socket.messages.ValidateMessage
import com.example.keyrisdk.services.socket.messages.VerifyApproveMessage
import com.example.keyrisdk.services.socket.messages.VerifyRequestMessage
import com.google.gson.JsonParser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import okhttp3.*
import org.json.JSONObject
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SocketService(private val url: String) {

    val verifyMessageChannel = Channel<VerifyRequestMessage>(Channel.BUFFERED)
    val errorChannel = Channel<Throwable>(Channel.BUFFERED)

    private var extraHeaders: Map<String, List<String>>? = null

    private var extraHeader: Pair<String, String>? = null

    private val socketOkHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS)
            .connectTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()
    }

    private val socketRequestBuilder by lazy { Request.Builder().url(url) }

    private var webSocket: WebSocket? = null

    suspend fun reconnectNew(extraHeader: String) {
        this.extraHeader = EXTRA_HEADER_NAME to extraHeader

        connectIfNeededNew(true)
    }

    private fun connectIfNeededNew(forceReconnect: Boolean = false) {
        if (webSocket == null || forceReconnect) {
            val socketRequest = socketRequestBuilder
                .apply {
                    extraHeader?.let { (header, value) -> header(header, value) }
                }
                .build()

            val webSocketListener = object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    val data = JSONObject(JsonParser().parse(text).asJsonObject.toString())

                    parseVerificationRequest(data)?.let { verifyMessageChannel.offer(it) }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    errorChannel.offer(NetworkException)
                }
            }

            webSocket = socketOkHttpClient.newWebSocket(socketRequest, webSocketListener)
        }
    }

    suspend fun sendVerificationEventNew(message: ValidateMessage) {
        connectIfNeededNew()
        webSocket?.send(message.toSocketData().toString())
    }

    suspend fun sendConfirmationEventNew(message: VerifyApproveMessage) {
        connectIfNeededNew()
        webSocket?.send(message.toSocketData().toString())
    }


    // TODO Remove
    private val socket = Socket(url, 5000)

    suspend fun reconnect(extraHeader: String) {
        extraHeaders = mapOf(EXTRA_HEADER_NAME to listOf(extraHeader))
        connectIfNeeded(true)
    }

    /**
     * Establishes socket connection if not already connected
     */
    private suspend fun connectIfNeeded(forceReconnect: Boolean = false) {
//        val connected = ::socket.isInitialized && socket.connected()
//        if (connected && !forceReconnect) return
//
//        return withTimeout(SOCKET_TIMEOUT) {
//            suspendCoroutine { continuation ->
//                Log.d(TAG, "Connecting...")
//
//                socket = IO.socket(
//                    url, SocketOptionBuilder
//                        .builder()
//                        .setTransports(arrayOf(WebSocket.NAME))
//                        .setExtraHeaders(extraHeaders)
//                        .build()
//                )
//
//                socket.disconnect()
//                socket.connect()
//
//                socket.on(EVENT_DISCONNECT) {
//                    Log.d(TAG, "Disconnect")
//
//                    if (continuation.context.isActive) {
//                        continuation.resumeWithException(NetworkException)
//                    }
//                }
//
//                socket.on(EVENT_CONNECT) {
//                    Log.d(TAG, "Connected")
//
//                    if (continuation.context.isActive) {
//                        continuation.resume(Unit)
//                    }
//                }
//
//                socket.on(EVENT_CONNECT_ERROR) {
//                    Log.d(TAG, "Failed to connect")
//
//                    if (continuation.context.isActive) {
//                        continuation.resumeWithException(NetworkException)
//                    }
//                }
//            }
//        }
    }

    suspend fun sendVerificationEvent(message: ValidateMessage): VerifyRequestMessage {
        connectIfNeeded()
        return withTimeout(SOCKET_TIMEOUT) {
            suspendCoroutine { continuation ->
                Log.d(TAG, "Sending verification request")

//                socket.emit(SocketAction.SESSION_VALIDATE.name, message.toSocketData())
//                socket.on(SocketAction.SESSION_VERIFY_REQUEST.name) { data: Array<out Any>? ->
//                    Log.d(TAG, "verification response received")
//
//                    val payload = data?.first() as? JSONObject
//                    if (payload != null) {
//                        parseVerificationRequest(payload)
//                            ?.takeIf { continuation.context.isActive }
//                            ?.let { continuation.resume(it) }
//                    }
//                }
            }
        }
    }

    private fun parseVerificationRequest(data: JSONObject): VerifyRequestMessage? {
        val action = data["action"] as String
        if (action != SocketAction.SESSION_VERIFY_REQUEST.name) return null

        val publicKey = data.opt("publicKey") as? String?
        val sessionKey = data["sessionKey"] as? String ?: return null
        return VerifyRequestMessage(publicKey, sessionKey)
    }

    suspend fun sendConfirmationEvent(message: VerifyApproveMessage) {
        connectIfNeeded()
        withTimeout(SOCKET_TIMEOUT) {
            suspendCoroutine<Void?> { continuation ->
                Log.d(TAG, "Sending confirmation")

//                socket.emit(CONFIRMATION_EVENT_NAME, message.toSocketData())

                if (continuation.context.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    companion object {
        private const val TAG = "Keyri > SocketService"
        private const val SOCKET_TIMEOUT = 5000L
        private const val CONFIRMATION_EVENT_NAME = "message"
        private const val EXTRA_HEADER_NAME = "userSuffix"

        const val NORMAL_CLOSURE_STATUS = 1_000
    }
}
