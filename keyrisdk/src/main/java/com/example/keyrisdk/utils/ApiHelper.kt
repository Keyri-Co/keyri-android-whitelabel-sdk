package com.example.keyrisdk.utils

import com.example.keyrisdk.exception.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

suspend fun <T : Any> makeApiCall(call: suspend () -> Response<T>): Response<T> {
    try {
        val response = call.invoke()

        if (!response.isSuccessful) {
            throw when (response.code()) {
                in 500..599 -> InternalServerException(response.code())
                else -> {
                    val errorResponse: String? = Gson().fromJson(
                        response.errorBody()?.charStream(),
                        object : TypeToken<String?>() {}.type
                    )

                    ServerErrorException(response.code(), errorResponse)
                }
            }
        }

        return response
    } catch (e: Exception) {
        throw when (e) {
            is KeyriSdkException -> e
            is IOException -> {
                when (e) {
                    is UnknownHostException,
                    is SocketTimeoutException,
                    is ConnectException -> NetworkException
                    else -> ServerUnreachableException
                }
            }
            else -> InternalServerException()
        }
    }
}