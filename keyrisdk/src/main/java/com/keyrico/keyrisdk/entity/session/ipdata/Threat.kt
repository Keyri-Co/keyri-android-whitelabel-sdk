package com.keyrico.keyrisdk.entity.session.ipdata

import com.google.gson.annotations.SerializedName

data class Threat(

    @SerializedName("is_tor")
    val isTor: Boolean,

    @SerializedName("is_proxy")
    val isProxy: Boolean,

    @SerializedName("is_anonymous")
    val isAnonymous: Boolean,

    @SerializedName("is_known_attacker")
    val isKnownAttacker: Boolean,

    @SerializedName("is_known_abuser")
    val isKnownAbuser: Boolean,

    @SerializedName("is_threat")
    val isThreat: Boolean,

    @SerializedName("is_bogon")
    val isBogon: Boolean
)
