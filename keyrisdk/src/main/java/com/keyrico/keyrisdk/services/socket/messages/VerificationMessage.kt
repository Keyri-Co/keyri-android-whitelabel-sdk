package com.keyrico.keyrisdk.services.socket.messages

import com.google.gson.annotations.SerializedName

data class VerificationMessage(

    @SerializedName("userId")
    val userId: String,

    @SerializedName("custom")
    val custom: String?,

    @SerializedName("timestamp")
    val timestamp: String
)
