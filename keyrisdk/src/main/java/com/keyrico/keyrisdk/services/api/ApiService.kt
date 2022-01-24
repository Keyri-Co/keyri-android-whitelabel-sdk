package com.keyrico.keyrisdk.services.api

import com.keyrico.keyrisdk.entity.Session
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

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

    /**
     * @GET Method for retrieving deep links prefix
     */
    @GET("application/deep-link")
    suspend fun getDeepLinksPrefix(@Query("appKey") appKey: String): Response<PrefixesResponse>
}
