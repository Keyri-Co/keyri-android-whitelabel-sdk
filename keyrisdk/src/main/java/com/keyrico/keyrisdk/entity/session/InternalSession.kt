package com.keyrico.keyrisdk.entity.session

import com.google.gson.annotations.SerializedName

internal data class InternalSession(
    @SerializedName("WidgetOrigin")
    val widgetOrigin: String,

    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("WidgetUserAgent")
    val widgetUserAgent: WidgetUserAgent?,

    @SerializedName("userParameters")
    val userParameters: UserParameters?,

    @SerializedName("IPAddressMobile")
    val iPAddressMobile: String,

    @SerializedName("IPAddressWidget")
    val iPAddressWidget: String,

    @SerializedName("riskAnalytics")
    val riskAnalytics: RiskAnalytics?,

    @SerializedName("browserPublicKey")
    val browserPublicKey: String,

    @SerializedName("__salt")
    val salt: String,

    @SerializedName("__hash")
    val hash: String
) {
    fun toSession(payload: String, publicUserId: String): Session {
        return Session(
            widgetOrigin = widgetOrigin,
            sessionId = sessionId,
            widgetUserAgent = widgetUserAgent,
            userParameters = userParameters,
            iPAddressMobile = iPAddressMobile,
            iPAddressWidget = iPAddressWidget,
            riskAnalytics = riskAnalytics,
            browserPublicKey = browserPublicKey,
            salt = salt,
            hash = hash,
            payload = payload,
            publicUserId = publicUserId
        )
    }
}
