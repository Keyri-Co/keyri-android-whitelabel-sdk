package com.keyrico.keyrisdk.services.api

import com.google.gson.annotations.SerializedName

data class ServerData(

    @SerializedName("publicKey")
    val publicKey: String,

    @SerializedName("ciphertext")
    val ciphertext: String,

    @SerializedName("salt")
    val salt: String,

    @SerializedName("iv")
    val iv: String
)
