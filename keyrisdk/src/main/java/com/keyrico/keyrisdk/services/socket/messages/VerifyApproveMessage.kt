package com.keyrico.keyrisdk.services.socket.messages

import com.google.gson.JsonObject
import com.keyrico.keyrisdk.services.socket.SocketAction

data class VerifyApproveMessage(
    val cipher: String,
    val publicKey: String?,
    val initializationVector: String?
) {

    /**
     * Create [JsonObject] for representing [VerifyApproveMessage].
     *
     * @return [JsonObject] verifying approve message.
     */
    fun toSocketData() = JsonObject()
        .also {
            it.addProperty("cipher", cipher)
            it.addProperty("publicKey", publicKey)
            it.addProperty("iv", initializationVector)
            it.addProperty("action", SocketAction.SESSION_VERIFY_APPROVE.name)
        }
}
