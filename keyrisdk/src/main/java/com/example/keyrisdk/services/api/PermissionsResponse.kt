package com.example.keyrisdk.services.api

import com.google.gson.annotations.SerializedName

data class PermissionsResponse(

    @SerializedName("accounts")
    val accounts: Boolean?,

    @SerializedName("login")
    val login: Boolean?

)