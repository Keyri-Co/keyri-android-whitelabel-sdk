package com.keyrico.keyrisdk.entity

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class IPData(

    @SerializedName("country")
    val country: String,

    @SerializedName("city")
    val city: String,

    @SerializedName("timezone")
    val timezone: String,

    @SerializedName("countryCode")
    val countryCode: String
): Parcelable
