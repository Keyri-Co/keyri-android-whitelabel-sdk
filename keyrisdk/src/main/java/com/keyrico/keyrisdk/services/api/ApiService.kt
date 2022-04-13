package com.keyrico.keyrisdk.services.api

import com.keyrico.keyrisdk.entity.session.Session
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/session/{sessionId}")
    suspend fun getSession(
        @Path("sessionId") sessionId: String,
        @Query("appKey") appKey: String
    ): Response<Session>

    @POST("api/session/{sessionId}")
    suspend fun challengeSession(@Body request: ChallengeSessionRequest): Response<String>
}
