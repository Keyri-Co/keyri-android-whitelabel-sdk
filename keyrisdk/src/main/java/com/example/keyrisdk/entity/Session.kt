package com.example.keyrisdk.entity

import com.google.gson.annotations.SerializedName

data class Session(

    @SerializedName("service")
    val service: Service,

    @SerializedName("username")
    val username: String,

    @SerializedName("isNewUser")
    val isNewUser: Boolean
)
