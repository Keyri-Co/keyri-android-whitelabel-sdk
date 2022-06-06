package com.keyrico.keyrisdk.entity.session

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GeoData(
    @SerializedName("mobile")
    val mobile: IPData?,

    @SerializedName("browser")
    val browser: IPData?
) : Parcelable
