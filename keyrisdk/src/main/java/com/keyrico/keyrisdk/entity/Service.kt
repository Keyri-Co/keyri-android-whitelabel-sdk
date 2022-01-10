package com.keyrico.keyrisdk.entity

import com.google.gson.annotations.SerializedName

data class Service(

    @SerializedName("_id")
    val serviceId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("logo")
    val logo: String
)
