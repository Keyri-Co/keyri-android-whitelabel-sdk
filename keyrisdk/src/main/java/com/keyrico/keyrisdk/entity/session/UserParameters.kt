package com.keyrico.keyrisdk.entity.session

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserParameters(
    @SerializedName("Origin")
    val origin: String,

    @SerializedName("method")
    val method: String,

    @SerializedName("Environment")
    val Environment: String
) : Parcelable
