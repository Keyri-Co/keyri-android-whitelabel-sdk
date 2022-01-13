package com.keyrico.keyrisdk.services.api

import com.google.gson.annotations.SerializedName

data class PrefixesResponse(

    @SerializedName("androidPrefix")
    val androidPrefix: String,

    @SerializedName("iosPrefix")
    val iosPrefix: String
)
