package com.keyrico.keyrisdk.entity.session.ipdata

import com.google.gson.annotations.SerializedName

data class IpLanguage(

    @SerializedName("name")
    val name: String,

    @SerializedName("native")
    val native: String,

    @SerializedName("code")
    val code: String
)
