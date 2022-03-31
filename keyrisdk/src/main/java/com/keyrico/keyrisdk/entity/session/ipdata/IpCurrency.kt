package com.keyrico.keyrisdk.entity.session.ipdata

import com.google.gson.annotations.SerializedName

data class IpCurrency(

    @SerializedName("name")
    val name: String?,

    @SerializedName("code")
    val code: String?,

    @SerializedName("symbol")
    val symbol: String?,

    @SerializedName("native")
    val native: String?,

    @SerializedName("plural")
    val plural: String?
)
