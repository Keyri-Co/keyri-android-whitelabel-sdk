package com.keyrico.keyrisdk.services.api

import com.google.gson.annotations.SerializedName

data class ChallengeSessionRequest(

    @SerializedName("sessionData")
    val serverData: ServerData,

    @SerializedName("publicObject")
    val publicObject: PublicObject,

    @SerializedName("__salt")
    val salt: String,

    @SerializedName("__hash")
    val hash: String
)
