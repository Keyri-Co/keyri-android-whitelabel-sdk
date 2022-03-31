package com.keyrico.keyrisdk.entity.session.ipdata

import com.google.gson.annotations.SerializedName

data class IpData(

    @SerializedName("ip")
    val ip: String?,

    @SerializedName("is_eu")
    val is_eu: Boolean?,

    @SerializedName("city")
    val city: String?,

    @SerializedName("region")
    val region: String?,

    @SerializedName("region_code")
    val region_code: String?,

    @SerializedName("country_name")
    val country_name: String?,

    @SerializedName("country_code")
    val country_code: String?,

    @SerializedName("continent_name")
    val continent_name: String?,

    @SerializedName("continent_code")
    val continent_code: String?,

    @SerializedName("latitude")
    val latitude: Double?,

    @SerializedName("longitude")
    val longitude: Double?,

    @SerializedName("postal")
    val postal: String?,

    @SerializedName("calling_code")
    val calling_code: String?,

    @SerializedName("flag")
    val flag: String?,

    @SerializedName("emoji_flag")
    val emoji_flag: String?,

    @SerializedName("emoji_unicode")
    val emoji_unicode: String?,

    @SerializedName("asn")
    val asn: Asn?,

    @SerializedName("languages")
    val languages: List<IpLanguage>?,

    @SerializedName("currency")
    val currency: IpCurrency?,

    @SerializedName("time_zone")
    val time_zone: IpTimeZone?,

    @SerializedName("threat")
    val threat: Threat?,

    @SerializedName("count")
    val count: String?,

    @SerializedName("status")
    val status: Int?
)
