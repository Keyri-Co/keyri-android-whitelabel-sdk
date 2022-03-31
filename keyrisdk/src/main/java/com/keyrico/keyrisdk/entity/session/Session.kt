package com.keyrico.keyrisdk.entity.session

import com.google.gson.annotations.SerializedName
import com.keyrico.keyrisdk.entity.session.ipdata.IpData
import com.keyrico.keyrisdk.entity.session.service.Service

data class Session(

    @SerializedName("service")
    val service: Service,

    @SerializedName("username")
    val username: String?,

    @SerializedName("isNewUser")
    val isNewUser: Boolean,

    @SerializedName("widgetIPData")
    val widgetIPData: IpData?,

    @SerializedName("mobileIPData")
    val mobileIPData: IpData?,

    @SerializedName("sessionType")
    val sessionType: String?,

    @SerializedName("custom")
    val custom: String?
)
