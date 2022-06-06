package com.keyrico.keyrisdk.entity.session

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class WidgetUserAgent(
    @SerializedName("isDesktop")
    val isDesktop: Boolean,

    @SerializedName("os")
    val os: String,

    @SerializedName("browser")
    val browser: String
) : Parcelable
