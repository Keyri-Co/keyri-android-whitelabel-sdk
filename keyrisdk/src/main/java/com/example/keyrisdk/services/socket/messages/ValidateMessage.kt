package com.example.keyrisdk.services.socket.messages

import com.example.keyrisdk.services.socket.SocketAction
import com.google.gson.JsonObject

data class ValidateMessage(val sessionId: String, val sessionKey: String) {

    fun toSocketData() = JsonObject()
        .also {
            it.addProperty("sessionId", sessionId)
            it.addProperty("sessionKey", sessionKey)
            it.addProperty("action", SocketAction.SESSION_VALIDATE.name)
        }
}
