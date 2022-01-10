package com.keyrico.keyrisdk.services.socket.messages

data class VerifyRequestMessage(
    val publicKey: String?,
    val sessionKey: String
)
