package com.keyrico.keyrisdk.entity.session.service

import com.google.gson.annotations.SerializedName

data class AndroidAppSettings(

    @SerializedName("androidPackageName")
    val androidPackageName: String?,

    @SerializedName("sha256CertFingerprints")
    val sha256CertFingerprints: String?,

    @SerializedName("androidGooglePlayLink")
    val androidGooglePlayLink: String?
)
