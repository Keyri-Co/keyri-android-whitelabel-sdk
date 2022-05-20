package com.keyrico.keyrisdk.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.keyrico.keyrisdk.BuildConfig
import com.keyrico.keyrisdk.entity.SessionConfirmationResponse
import com.keyrico.keyrisdk.exception.AuthorizationException
import com.keyrico.keyrisdk.exception.InternalServerException
import com.keyrico.keyrisdk.exception.NetworkException
import com.keyrico.keyrisdk.services.api.ApiService
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val CONNECT_TIMEOUT = 15L
private const val READ_TIMEOUT = 15L

internal suspend fun <T : Any> makeApiCall(call: suspend () -> Response<T>): Result<T> {
    try {
        val response = call.invoke()

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()
            val type = object : TypeToken<SessionConfirmationResponse>() {}.type

            val errorResponse: SessionConfirmationResponse? =
                Gson().fromJson(errorBody?.charStream(), type)

            errorBody?.close()

            val error = InternalServerException(errorResponse?.status ?: "Unable to authorize")

            return Result.failure(error)
        }

        return response.body()?.let { Result.success(it) }
            ?: throw AuthorizationException("Unable to authorize")
    } catch (e: Exception) {
        val error = when (e) {
            is IOException -> {
                when (e) {
                    is UnknownHostException,
                    is SocketTimeoutException,
                    is ConnectException -> NetworkException("No internet connection")
                    else -> e
                }
            }
            else -> e
        }

        return Result.failure(error)
    }
}

internal fun provideApiService(): ApiService {
    val okHttpClientBuilder = OkHttpClient.Builder()

    okHttpClientBuilder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)

    if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }.let(okHttpClientBuilder::addInterceptor)
    }

    return Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClientBuilder.build())
        .build()
        .create(ApiService::class.java)
}
