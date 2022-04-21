package com.keyrico.keyrisdk.entity

import com.google.gson.annotations.SerializedName

data class AccountData(

    @SerializedName("ttl")
    val ttl: Double,

    @SerializedName("id")
    val id: String
)
