package com.keyrico.keyrisdk.services.socket.messages

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.keyrico.keyrisdk.services.socket.SocketAction

data class CustomAuthChallengeMessage(

    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("sessionId")
    val timestamp: String,

    @SerializedName("sessionId")
    val cipher: String,

    @SerializedName("iv")
    val iv: String,

    @SerializedName("isWhitelabel")
    val isWhitelabel: Boolean
) {

    fun toSocketData() = JsonObject()
        .also {
            it.addProperty("sessionId", sessionId)
            it.addProperty("timestamp", timestamp)
            it.addProperty("cipher", cipher)
            it.addProperty("iv", iv)
            it.addProperty("isWhitelabel", isWhitelabel)
            it.addProperty("action", SocketAction.CUSTOM_AUTH_CHALLENGE.name)
        }
}
