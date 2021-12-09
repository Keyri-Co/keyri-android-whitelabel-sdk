package com.example.keyrisdk.services.api

import com.example.keyrisdk.entity.Service
import com.google.gson.annotations.SerializedName

data class InitResponse(

    @SerializedName("token")
    val token: String,

    @SerializedName("service")
    val service: Service
)
