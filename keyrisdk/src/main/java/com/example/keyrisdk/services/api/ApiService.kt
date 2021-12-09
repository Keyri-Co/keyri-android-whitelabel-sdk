package com.example.keyrisdk.services.api

import com.example.keyrisdk.entity.Session
import retrofit2.Response
import retrofit2.http.*

/**
 * Keyri SDK REST API
 */
interface ApiService {

    /**
     * @GET Method for retrieving session by id
     */
    @GET("api/session/{sessionId}")
    suspend fun getSession(@Path("sessionId") sessionId: String): Response<Session>

    /**
     * @POST Method for mobile auth
     */
    @POST
    suspend fun authMobile(
        @HeaderMap headers: Map<String, String>?,
        @Url url: String,
        @Body request: AuthMobileRequest
    ): Response<AuthMobileResponse>

    /**
     * @POST Method for init SDK
     */
    @POST("api/sdk/whitelabel-init")
    suspend fun init(@Body request: InitRequest): Response<InitResponse>

    /**
     * @GET Method for retrieving permissions by service id
     */
    @GET("service/{serviceId}/permissions")
    suspend fun getPermissions(
        @Path("serviceId") serviceId: String,
        @Query("queryPermissions[]") permissions: List<String>
    ): Response<PermissionsResponse>
}
