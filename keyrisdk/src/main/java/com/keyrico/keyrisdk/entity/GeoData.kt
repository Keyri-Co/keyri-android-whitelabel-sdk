package com.keyrico.keyrisdk.entity

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GeoData(

    @SerializedName("continent_code")
    val continentCode: String,

    @SerializedName("country_code")
    val countryCode: String,

    @SerializedName("city")
    val city: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("region_code")
    val regionCode: String
) : Parcelable
