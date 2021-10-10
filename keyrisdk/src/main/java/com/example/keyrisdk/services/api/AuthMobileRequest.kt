package com.example.keyrisdk.services.api

import com.google.gson.annotations.SerializedName

data class AuthMobileRequest(

    @SerializedName("userId")
    val userId: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("clientPublicKey")
    val clientPublicKey: String?

)