package com.keyrico.keyrisdk.entity.session

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class WidgetUserAgent(
    @SerializedName("Custom")
    val custom: String?
) : Parcelable
