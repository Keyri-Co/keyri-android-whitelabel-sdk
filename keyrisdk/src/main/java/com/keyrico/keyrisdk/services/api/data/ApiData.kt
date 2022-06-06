package com.keyrico.keyrisdk.services.api.data

import com.google.gson.annotations.SerializedName

data class ApiData(

    @SerializedName("publicUserId")
    val publicUserId: String?,

    @SerializedName("associationKey")
    val associationKey: String?
)
