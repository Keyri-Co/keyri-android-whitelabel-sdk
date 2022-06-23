package com.keyrico.keyrisdk.entity.session

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class RiskAnalytics(
    @SerializedName("riskAttributes")
    val riskAttributes: RiskAttributes,

    @SerializedName("riskStatus")
    val riskStatus: String?,

    @SerializedName("riskFlagString")
    val riskFlagString: String?,

    @SerializedName("geoData")
    val geoData: GeoData?
) : Parcelable {

    fun getRiskStatusType(): RiskMessageTypes {
        return when (riskStatus) {
            "good" -> RiskMessageTypes.GOOD
            "warn" -> RiskMessageTypes.WARNING
            else -> RiskMessageTypes.DANGER
        }
    }

    enum class RiskMessageTypes {
        GOOD,
        WARNING,
        DANGER;
    }
}
