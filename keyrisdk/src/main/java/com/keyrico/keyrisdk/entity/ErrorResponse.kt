package com.keyrico.keyrisdk.entity

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("status")
    val message: String
)
