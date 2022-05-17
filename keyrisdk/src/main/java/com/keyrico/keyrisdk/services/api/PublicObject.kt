package com.keyrico.keyrisdk.services.api

import com.google.gson.annotations.SerializedName

data class PublicObject(

    @SerializedName("username")
    val username: String?,

    @SerializedName("publicKey")
    val publicKey: String?,

    @SerializedName("customObject")
    val customObject: String?
)
