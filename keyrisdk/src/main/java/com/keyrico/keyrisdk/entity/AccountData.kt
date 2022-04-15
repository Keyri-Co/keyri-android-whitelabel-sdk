package com.keyrico.keyrisdk.entity

import com.google.gson.annotations.SerializedName

data class AccountData(

    @SerializedName("ttl")
    val ttl: Long,

    @SerializedName("id")
    val id: String
)
