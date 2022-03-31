package com.keyrico.keyrisdk.entity.session.ipdata

import com.google.gson.annotations.SerializedName

data class Asn(

    @SerializedName("asn")
    val asn: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("domain")
    val domain: String?,

    @SerializedName("route")
    val route: String?,

    @SerializedName("type")
    val type: String?
)
