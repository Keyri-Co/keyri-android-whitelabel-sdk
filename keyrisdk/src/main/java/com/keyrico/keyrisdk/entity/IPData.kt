package com.keyrico.keyrisdk.entity

import com.google.gson.annotations.SerializedName

data class IPData(

    @SerializedName("country")
    val country: String,

    @SerializedName("city")
    val city: String,

    @SerializedName("timezone")
    val timezone: String,

    @SerializedName("countryCode")
    val countryCode: String
)
