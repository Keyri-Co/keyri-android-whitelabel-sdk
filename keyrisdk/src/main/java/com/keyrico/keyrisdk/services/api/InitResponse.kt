package com.keyrico.keyrisdk.services.api

import com.keyrico.keyrisdk.entity.Service
import com.google.gson.annotations.SerializedName

data class InitResponse(

    @SerializedName("token")
    val token: String,

    @SerializedName("service")
    val service: Service
)
