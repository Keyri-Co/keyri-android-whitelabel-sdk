package com.example.keyrisdk.services.socket

import android.util.Log
import com.example.keyrisdk.exception.AuthorizationException
import com.example.keyrisdk.services.socket.messages.ValidateMessage
import com.example.keyrisdk.services.socket.messages.VerifyApproveMessage
import com.example.keyrisdk.services.socket.messages.VerifyRequestMessage
import com.google.gson.JsonParser
import kotlinx.coroutines.channels.Channel
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

class SocketService(private val url: String) : WebSocketListener() {

    /**
     * Channel that receiving VerifyRequestMessage events or Throwable errors.
     */
    val verifyMessageChannel = Channel<Result<VerifyRequestMessage>>(1)

    private var extraHeader: Pair<String, String>? = null

    private val socketOkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS)
            .connectTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()
    }

    private val socketRequestBuilder by lazy { Request.Builder().url(url) }

    private var webSocket: WebSocket? = null

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d(TAG, "New message recieved")

        val data = JSONObject(JsonParser().parse(text).asJsonObject.toString())

        parseVerificationRequest(data)?.let { verifyMessageChannel.offer(Result.success(it)) }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "Error: ${t.message}")

        verifyMessageChannel.offer(Result.failure(AuthorizationException))
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "Opened")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "Closing...")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "Closed")
    }

    /**
     * Function for initialization [SocketService] with extra header and establish [webSocket] connection.
     *
     * @extraHeader needed for initialization socket extra header.
     */
    fun reconnect(extraHeader: String) {
        this.extraHeader = EXTRA_HEADER_NAME to extraHeader

        connectIfNeeded(true)
    }

    /**
     * Use this function to send verification event [ValidateMessage].
     *
     * @message validation message to send.
     */
    fun sendVerificationEvent(message: ValidateMessage) {
        Log.d(TAG, "Sending verification event...")

        connectIfNeeded()
        webSocket?.send(message.toSocketData().toString())
    }

    /**
     * Use this function to send confirmation event [VerifyApproveMessage].
     *
     * @message verification message to send.
     */
    fun sendConfirmationEvent(message: VerifyApproveMessage) {
        Log.d(TAG, "Sending confirmation event...")

        connectIfNeeded()
        webSocket?.send(message.toSocketData().toString())
    }

    private fun connectIfNeeded(forceReconnect: Boolean = false) {
        if (webSocket == null || forceReconnect) {
            Log.d(TAG, "Connecting...")

            val request = socketRequestBuilder.apply {
                extraHeader?.let { (key, value) -> addHeader(key, value) }
            }.build()

            webSocket = socketOkHttpClient.newWebSocket(request, this)
        }
    }

    private fun parseVerificationRequest(data: JSONObject): VerifyRequestMessage? {
        val action = data["action"] as String
        if (action != SocketAction.SESSION_VERIFY_REQUEST.name) return null

        val publicKey = data.opt("publicKey") as? String?
        val sessionKey = data["sessionKey"] as? String ?: return null

        return VerifyRequestMessage(publicKey, sessionKey)
    }

    companion object {
        private const val TAG = "Keyri > SocketService"
        private const val SOCKET_TIMEOUT = 5000L
        private const val EXTRA_HEADER_NAME = "userSuffix"
    }
}
