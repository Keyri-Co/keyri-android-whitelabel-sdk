package com.keyrico.keyrisdk.entity

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class IPData(

    @SerializedName("continent_name")
    val continentName: String,

    @SerializedName("calling_code")
    val callingCode: String,

    @SerializedName("city")
    val city: String,

    @SerializedName("ip")
    val ip: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("emoji_unicode")
    val emojiUnicode: String,

    @SerializedName("continent_code")
    val continentCode: String,

    @SerializedName("country_code")
    val countryCode: String,

    @SerializedName("is_eu")
    val isEu: Boolean,

    @SerializedName("country_name")
    val countryName: String,

    @SerializedName("postal")
    val postal: String,

    @SerializedName("region")
    val region: String,

    @SerializedName("longitude")
    val longitude: Double
) : Parcelable
