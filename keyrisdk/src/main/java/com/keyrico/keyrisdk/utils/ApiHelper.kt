package com.keyrico.keyrisdk.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.keyrico.keyrisdk.BuildConfig
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

internal suspend fun <T : Any> makeApiCall(call: suspend () -> Response<T>): Response<T> {
    try {
        val response = call.invoke()

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()
            val type = object : TypeToken<String?>() {}.type

            val errorResponse: String? = Gson().fromJson(errorBody?.charStream(), type)

            errorBody?.close()

            throw InternalServerException(errorResponse ?: "Unable to authorize")
        }

        return response
    } catch (e: Exception) {
        throw when (e) {
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
