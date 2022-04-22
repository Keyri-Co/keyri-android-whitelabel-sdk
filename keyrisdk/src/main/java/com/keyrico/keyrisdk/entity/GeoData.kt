package com.keyrico.keyrisdk.entity

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GeoData(

    @SerializedName("continent_code")
    val continent_code: String,

    @SerializedName("country_code")
    val country_code: String,

    @SerializedName("city")
    val city: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("region_code")
    val region_code: String
): Parcelable
