package com.keyrico.keyrisdk.entity.session.service

import com.google.gson.annotations.SerializedName

data class IosAppSettings(

    @SerializedName("iosAppId")
    val iosAppId: String?,

    @SerializedName("iosAppStoreLink")
    val iosAppStoreLink: String?,

    @SerializedName("teamId")
    val teamId: String?,

    @SerializedName("bundleId")
    val bundleId: String?
)
