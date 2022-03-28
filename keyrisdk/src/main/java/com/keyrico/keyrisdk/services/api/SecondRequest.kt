package com.keyrico.keyrisdk.services.api

import com.google.gson.annotations.SerializedName

data class SecondRequest(
    @SerializedName("publicObject")
    val publicObject: PublicObject,
    @SerializedName("cipher")
    val cipher: String,
    @SerializedName("sessionId")
    val sessionId: String
)
