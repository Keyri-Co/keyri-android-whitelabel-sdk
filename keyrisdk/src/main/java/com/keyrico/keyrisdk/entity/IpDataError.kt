package com.keyrico.keyrisdk.entity

import com.google.gson.annotations.SerializedName

data class IpDataError(

    @SerializedName("riskErrors")
    val riskErrors: List<String>
)
