package com.keyrico.keyrisdk.entity

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class RiskAnalytics(

    @SerializedName("geoData")
    val geoData: GeoData?,

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

enum class RiskMessageTypes(val type: String) {
    FINE("Fine"),
    WARNING("Warning!"),
    DANGER("Danger!");
}
