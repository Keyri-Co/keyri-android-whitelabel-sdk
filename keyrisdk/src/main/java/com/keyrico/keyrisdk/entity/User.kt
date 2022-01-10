package com.keyrico.keyrisdk.entity

import com.google.gson.annotations.SerializedName

data class User(

    @SerializedName("_id")
    val userId: String,

    @SerializedName("name")
    val name: String
)
