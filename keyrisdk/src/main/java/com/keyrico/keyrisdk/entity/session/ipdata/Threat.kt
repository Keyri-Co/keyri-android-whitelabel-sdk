package com.keyrico.keyrisdk.entity.session.ipdata

import com.google.gson.annotations.SerializedName

data class Threat(

    @SerializedName("is_tor")
    val is_tor: Boolean?,

    @SerializedName("is_proxy")
    val is_proxy: Boolean?,

    @SerializedName("is_anonymous")
    val is_anonymous: Boolean?,

    @SerializedName("is_known_attacker")
    val is_known_attacker: Boolean?,

    @SerializedName("is_known_abuser")
    val is_known_abuser: Boolean?,

    @SerializedName("is_threat")
    val is_threat: Boolean?,

    @SerializedName("is_bogon")
    val is_bogon: Boolean?
)
