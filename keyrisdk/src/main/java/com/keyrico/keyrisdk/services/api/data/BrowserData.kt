package com.keyrico.keyrisdk.services.api.data

import com.google.gson.annotations.SerializedName

data class BrowserData(

    @SerializedName("publicKey")
    val publicKey: String,

    @SerializedName("ciphertext")
    val cipherText: String,

    @SerializedName("salt")
    val salt: String,

    @SerializedName("iv")
    val iv: String
)
