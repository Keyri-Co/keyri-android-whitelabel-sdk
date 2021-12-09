package com.example.keyrisdk.utils

import com.example.keyrisdk.KeyriSdk
import com.example.keyrisdk.exception.*
import retrofit2.Response
import java.io.IOException

suspend fun <T : Any> makeApiCall(call: suspend () -> Response<T>): Response<T> {
    try {
        val response = call.invoke()
        if (!response.isSuccessful) {
            if (response.code().toString().startsWith("5")) {
                throw InternalServerException(response.code())
            } else {
                throw ServerErrorException(response.code(), response.errorBody()?.string())
            }
        }
        return response
    } catch (e: Exception) {
        if (e is KeyriSdkException) {
            throw e
        }
        if (e is IOException) {
            if (Utils.isInternetAvailable(KeyriSdk.app)) {
                throw ServerUnreachableException
            } else {
                throw NetworkException
            }
        } else {
            throw InternalServerException()
        }
    }
}
