package com.keyrico.keyrisdk.services.api

import com.keyrico.keyrisdk.entity.SessionConfirmationResponse
import com.keyrico.keyrisdk.entity.session.InternalSession
import com.keyrico.keyrisdk.entity.session.Session
import com.keyrico.keyrisdk.services.api.data.SessionConfirmationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface ApiService {

    @GET("api/session/{sessionId}")
    suspend fun getSession(
        @Path("sessionId") sessionId: String,
        @Query("appKey") appKey: String
    ): Response<InternalSession>

    @POST("api/session/{sessionId}")
    suspend fun approveSession(
        @Path("sessionId") sessionId: String,
        @Body request: SessionConfirmationRequest
    ): Response<SessionConfirmationResponse>
}
