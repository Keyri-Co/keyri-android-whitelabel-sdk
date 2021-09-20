package com.example.keyrisdk.services.socket.messages

import com.example.keyrisdk.services.socket.SocketAction
import com.google.gson.JsonObject

data class VerifyApproveMessage(
    val cipher: String,
    val nonce: String,
    val publicKey: String
) {

    fun toSocketData() = JsonObject()
        .also {
            it.addProperty("cipher", cipher)
            it.addProperty("nonce", nonce)
            it.addProperty("publicKey", publicKey)
            it.addProperty("action", SocketAction.SESSION_VERIFY_APPROVE.name)
        }

}