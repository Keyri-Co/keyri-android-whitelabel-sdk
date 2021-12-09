package com.example.keyrisdk.services.api

import com.google.gson.annotations.SerializedName

data class PermissionsResponse(

    @SerializedName("getSession")
    val session: Boolean?,

    @SerializedName("accounts")
    val accounts: Boolean?,

    @SerializedName("login")
    val login: Boolean?,

    @SerializedName("signUp")
    val signup: Boolean?,

    @SerializedName("mobileLogin")
    val mobileLogin: Boolean?,

    @SerializedName("mobileSignUp")
    val mobileSignup: Boolean?
)
