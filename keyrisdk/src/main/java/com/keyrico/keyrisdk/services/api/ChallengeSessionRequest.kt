package com.keyrico.keyrisdk.services.api

import com.google.gson.annotations.SerializedName

data class ChallengeSessionRequest(

    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("publicObject")
    val publicObject: String,

    @SerializedName("cipher")
    val cipher: String
)
