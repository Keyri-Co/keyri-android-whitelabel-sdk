package com.keyrico.keyrisdk.services.api

import com.google.gson.annotations.SerializedName

data class ChallengeSessionRequest(

    @SerializedName("sessionId")
    val serverDataRequest: ServerDataRequest,

    @SerializedName("__salt")
    val salt: String,

    @SerializedName("__hash")
    val hash: String
)
