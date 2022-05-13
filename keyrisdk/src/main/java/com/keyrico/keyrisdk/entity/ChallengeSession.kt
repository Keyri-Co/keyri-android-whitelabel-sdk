package com.keyrico.keyrisdk.entity

import com.google.gson.annotations.SerializedName
import com.keyrico.keyrisdk.services.api.PublicObject

data class ChallengeSession(

    @SerializedName("WidgetEndPoint")
    val widgetEndPoint: String,

    @SerializedName("action")
    val action: String,

    @SerializedName("WidgetSocket")
    val widgetSocket: String,

    @SerializedName("ttl")
    val ttl: Double,

    @SerializedName("publicObject")
    val publicObject: PublicObject?,

    @SerializedName("IPDataWidget")
    val iPDataWidget: IPData,

    @SerializedName("IPDataMobile")
    val iPDataMobile: IPData,

    @SerializedName("WidgetOrigin")
    val widgetOrigin: String,

    @SerializedName("WidgetUserAgent")
    val widgetUserAgent: String,

    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("used")
    val used: Boolean,

    @SerializedName("accountData")
    val accountData: AccountData,

    @SerializedName("cipher")
    val cipher: String
)
