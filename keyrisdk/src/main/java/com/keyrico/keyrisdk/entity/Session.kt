package com.keyrico.keyrisdk.entity

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Session(

    @SerializedName("WidgetEndPoint")
    val widgetEndPoint: String,

    @SerializedName("WidgetOrigin")
    val widgetOrigin: String,

    @SerializedName("WidgetUserAgent")
    val widgetUserAgent: String?,

    @SerializedName("action")
    val action: String,

    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("ttl")
    val ttl: String,

    @SerializedName("logo")
    val logo: String,

    @SerializedName("IPDataMobile")
    val iPDataMobile: IPData?,

    @SerializedName("IPDataWidget")
    val iPDataWidget: IPData?,

    @SerializedName("riskAnalytics")
    val riskAnalytics: RiskAnalytics?,

    @SerializedName("browserPublicKey")
    val browserPublicKey: String,

    @SerializedName("__salt")
    val salt: String,

    @SerializedName("__hash")
    val hash: String
) : Parcelable
