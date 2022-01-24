package com.keyrico.keyrisdk.services.socket.messages

import com.google.gson.JsonObject
import com.keyrico.keyrisdk.services.socket.SocketAction

data class ValidateMessage(val sessionId: String, val sessionKey: String) {

    /**
     * Create [JsonObject] for representing [ValidateMessage].
     *
     * @return [JsonObject] validation message.
     */
    fun toSocketData() = JsonObject().also {
        it.addProperty("sessionId", sessionId)
        it.addProperty("sessionKey", sessionKey)
        it.addProperty("action", SocketAction.SESSION_VALIDATE.name)
    }
}
