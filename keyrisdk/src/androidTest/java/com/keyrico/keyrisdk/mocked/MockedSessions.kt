package com.keyrico.keyrisdk.mocked

import com.google.gson.JsonArray
import com.keyrico.keyrisdk.entity.GeoData
import com.keyrico.keyrisdk.entity.IPData
import com.keyrico.keyrisdk.entity.RiskAnalytics
import com.keyrico.keyrisdk.entity.Session

private const val EMPTY = ""

val geoData = GeoData(
    "NA",
    "US",
    "Oakland",
    37.838860527596296,
    -122.26317872548013,
    "CA"
)

val ipData = IPData(
    "North America",
    "+1",
    "Oakland",
    "255.255.255.255",
    37.838860527596296,
    EMPTY,
    "NA",
    "US",
    false,
    "United States",
    "94501",
    "California",
    -122.26317872548013
)

val sessionRegular = Session(
    EMPTY,
    "Chrome/101.0.4951.67",
    EMPTY,
    iPDataMobile = ipData,
    iPDataWidget = ipData,
    riskAnalytics = RiskAnalytics(geoData, JsonArray(), "fine"),
    EMPTY,
    EMPTY,
    EMPTY
)

val sessionDenied =
    sessionRegular.copy(
        riskAnalytics = RiskAnalytics(
            geoData = null,
            JsonArray(),
            riskStatus = "danger"
        )
    )

val sessionWarning =
    sessionRegular.copy(
        riskAnalytics = RiskAnalytics(
            geoData = geoData,
            JsonArray(),
            riskStatus = "warn"
        )
    )

val sessionNoIpData = sessionRegular.copy(iPDataWidget = null, iPDataMobile = null)

val sessionWithoutRiskPermission =
    sessionRegular.copy(iPDataWidget = null, iPDataMobile = null, riskAnalytics = null)
