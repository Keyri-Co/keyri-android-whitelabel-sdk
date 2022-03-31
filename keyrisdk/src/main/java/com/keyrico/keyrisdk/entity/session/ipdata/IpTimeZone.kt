package com.keyrico.keyrisdk.entity.session.ipdata

import com.google.gson.annotations.SerializedName

data class IpTimeZone(

    @SerializedName("name")
    val name: String?,

    @SerializedName("abbr")
    val abbr: String?,

    @SerializedName("offset")
    val offset: String?,

    @SerializedName("is_dst")
    val is_dst: Boolean?,

    @SerializedName("current_time")
    val current_time: String?
)
