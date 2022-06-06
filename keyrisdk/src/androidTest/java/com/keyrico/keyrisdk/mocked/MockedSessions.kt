package com.keyrico.keyrisdk.mocked

import com.keyrico.keyrisdk.entity.session.GeoData
import com.keyrico.keyrisdk.entity.session.IPData
import com.keyrico.keyrisdk.entity.session.RiskAnalytics
import com.keyrico.keyrisdk.entity.session.RiskAttributes
import com.keyrico.keyrisdk.entity.session.Session
import com.keyrico.keyrisdk.entity.session.UserParameters
import com.keyrico.keyrisdk.entity.session.WidgetUserAgent

private const val EMPTY = ""

val ipData = IPData(
    continentCode = "NA",
    countryCode = "US",
    city = "Oakland",
    latitude = -122.26317872548013,
    longitude = 37.838860527596296,
    regionCode = EMPTY
)

val geoData = GeoData(mobile = ipData, browser = ipData)

val riskAttributes = RiskAttributes(
    isKnownAbuser = false,
    isIcloudRelay = false,
    isKnownAttacker = false,
    isAnonymous = false,
    isThreat = false,
    isBogon = false,
    blocklists = false,
    isDatacenter = false,
    isTor = false,
    isProxy = false
)

val sessionRegular = Session(
    widgetOrigin = EMPTY,
    sessionId = EMPTY,
    widgetUserAgent = WidgetUserAgent(isDesktop = true, os = "Windows", browser = "Chrome"),
    userParameters = UserParameters(EMPTY, EMPTY, EMPTY),
    iPAddressMobile = EMPTY,
    iPAddressWidget = EMPTY,
    riskAnalytics = RiskAnalytics(
        riskAttributes = riskAttributes,
        riskStatus = "good",
        riskFlagString = EMPTY,
        geoData = geoData
    ),
    EMPTY,
    EMPTY,
    EMPTY,
    EMPTY,
    EMPTY
)

val sessionDenied =
    sessionRegular.copy(
        riskAnalytics = RiskAnalytics(
            riskAttributes = riskAttributes,
            riskStatus = "danger",
            riskFlagString = EMPTY,
            geoData = null
        )
    )

val sessionWarning =
    sessionRegular.copy(
        riskAnalytics = RiskAnalytics(
            riskAttributes = riskAttributes.copy(isAnonymous = true),
            riskStatus = "warn",
            riskFlagString = EMPTY,
            geoData = geoData
        )
    )

val sessionNoIpData =
    sessionRegular.copy(
        riskAnalytics = RiskAnalytics(
            riskAttributes,
            "good",
            EMPTY,
            geoData = null
        )
    )

val sessionWithoutRiskPermission = sessionRegular.copy(riskAnalytics = null)
