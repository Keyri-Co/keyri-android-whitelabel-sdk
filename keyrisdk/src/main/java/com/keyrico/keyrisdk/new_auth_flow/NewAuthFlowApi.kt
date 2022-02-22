package com.keyrico.keyrisdk.new_auth_flow

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface NewAuthFlowApi {

    @POST
    suspend fun firstPost(
        @HeaderMap headers: Map<String, String>?,
        @Body request: FirstRequest
    ): Response<FirstResponse>

    @POST
    suspend fun secondPost(@Body request: SecondRequest): Response<String>
}

data class FirstRequest(
    @SerializedName("sessionId")
    val sessionId: String
)

data class FirstResponse(
    @SerializedName("serviceDomain")
    val serviceDomain: String,
    @SerializedName("userAgent")
    val userAgent: String,
    @SerializedName("username")
    val username: String?,
    @SerializedName("riskCharacteristics")
    val riskCharacteristics: String
)

data class SecondRequest(
    @SerializedName("publicObject")
    val publicObject: PublicObject,
    @SerializedName("cipher")
    val cipher: String,
    @SerializedName("sessionId")
    val sessionId: String
)

data class PublicObject(
    @SerializedName("username")
    val username: String,
    @SerializedName("publicKey")
    val publicKey: String?,
)

