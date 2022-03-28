package com.keyrico.keyrisdk.services.api

import com.google.gson.annotations.SerializedName

data class FirstResponse(
    @SerializedName("serviceDomain")
    val serviceDomain: String,
    @SerializedName("userAgent")
    val userAgent: String,
    @SerializedName("username")
    val username: String?,
    @SerializedName("riskCharacteristics")
    val riskCharacteristics: String
)
