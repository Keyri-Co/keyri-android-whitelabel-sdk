package com.keyrico.keyrisdk.services.api

import com.keyrico.keyrisdk.entity.User
import com.google.gson.annotations.SerializedName

data class AuthMobileResponse(

    @SerializedName("user")
    val user: User,

    @SerializedName("token")
    val token: String,

    @SerializedName("refreshToken")
    val refreshToken: String
)
