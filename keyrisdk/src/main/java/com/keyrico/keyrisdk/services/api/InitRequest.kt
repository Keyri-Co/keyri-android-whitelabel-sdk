package com.keyrico.keyrisdk.services.api

import com.google.gson.annotations.SerializedName

data class InitRequest(

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("mobileAppKey")
    val mobileAppKey: String
)
