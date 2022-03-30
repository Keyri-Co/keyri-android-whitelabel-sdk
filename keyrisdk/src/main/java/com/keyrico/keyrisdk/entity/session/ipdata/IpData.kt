package com.keyrico.keyrisdk.entity.session.ipdata

import com.google.gson.annotations.SerializedName

data class IpData(

    @SerializedName("ip")
    val ip: String,

    @SerializedName("is_eu")
    val isEu: Boolean,

    @SerializedName("city")
    val city: String,

    @SerializedName("region")
    val region: String,

    @SerializedName("region_code")
    val regionCode: String,

    @SerializedName("country_name")
    val countryName: String,

    @SerializedName("country_code")
    val countryCode: String,

    @SerializedName("continent_name")
    val continentName: String,

    @SerializedName("continent_code")
    val continentCode: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("postal")
    val postal: String,

    @SerializedName("calling_code")
    val callingCode: String,

    @SerializedName("flag")
    val flag: String,

    @SerializedName("emoji_flag")
    val emojiFlag: String,

    @SerializedName("emoji_unicode")
    val emojiUnicode: String,

    @SerializedName("asn")
    val asn: Asn,

    @SerializedName("languages")
    val languages: List<IpLanguage>,

    @SerializedName("currency")
    val currency: IpCurrency,

    @SerializedName("time_zone")
    val timeZone: IpTimeZone,

    @SerializedName("threat")
    val threat: Threat,

    @SerializedName("count")
    val count: String,

    @SerializedName("status")
    val status: Int
)
