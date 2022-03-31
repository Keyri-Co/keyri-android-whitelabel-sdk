package com.keyrico.keyrisdk.entity.session.service

import com.google.gson.annotations.SerializedName

data class Service(

    @SerializedName("isValid")
    val isValid: Boolean?,

    @SerializedName("qrCodeType")
    val qrCodeType: String?,

    @SerializedName("androidAppSettings")
    val androidAppSettings: AndroidAppSettings?,

    @SerializedName("iosAppSettings")
    val iosAppSettings: IosAppSettings?,

    @SerializedName("subDomainName")
    val subDomainName: String?,

    @SerializedName("originalDomain")
    val originalDomain: OriginalDomain?,

    @SerializedName("_id")
    val id: String,

    @SerializedName("name")
    val name: String?,

    @SerializedName("logo")
    val logo: String?,

    @SerializedName("createdAt")
    val createdAt: String?,

    @SerializedName("updatedAt")
    val updatedAt: String?,

    @SerializedName("ironPlansUUID")
    val ironPlansUUID: String?,

    @SerializedName("qrLogo")
    val qrLogo: String?
)
