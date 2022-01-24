package com.keyrico.keyrisdk.services.api

import com.google.gson.annotations.SerializedName
import com.keyrico.keyrisdk.entity.Service

data class InitResponse(

    @SerializedName("token")
    val token: String,

    @SerializedName("service")
    val service: Service
)
