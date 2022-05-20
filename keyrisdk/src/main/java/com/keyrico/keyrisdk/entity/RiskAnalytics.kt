package com.keyrico.keyrisdk.entity

import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class RiskAnalytics(

    @SerializedName("geoData")
    val geoData: GeoData?,

    @SerializedName("riskAttributes")
    val riskAttributes: @RawValue JsonArray?,

    @SerializedName("riskStatus")
    val riskStatus: String
) : Parcelable {

    fun getRiskStatusType(): RiskMessageTypes {
        return when (riskStatus) {
            "fine" -> RiskMessageTypes.FINE
            "warn" -> RiskMessageTypes.WARNING
            else -> RiskMessageTypes.DANGER
        }
    }
}

enum class RiskMessageTypes {
    FINE,
    WARNING,
    DANGER;
}
