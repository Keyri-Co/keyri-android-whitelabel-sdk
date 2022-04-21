package com.keyrico.keyrisdk.entity

import com.google.gson.annotations.SerializedName

data class Session(

    @SerializedName("WidgetEndPoint")
    val widgetEndPoint: String,

    @SerializedName("WidgetOrigin")
    val widgetOrigin: String,

    @SerializedName("WidgetUserAgent")
    val widgetUserAgent: String,

    @SerializedName("action")
    val action: String,

    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("sessionType")
    val sessionType: String,

    @SerializedName("logo")
    val logo: String,

    @SerializedName("__salt")
    val salt: String,

    @SerializedName("__hash")
    val hash: String,

    @SerializedName("username")
    val username: String?
)
