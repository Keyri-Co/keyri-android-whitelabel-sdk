package com.keyrico.keyrisdk.utils

import android.util.MalformedJsonException
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.keyrico.keyrisdk.entity.IpDataError
import com.keyrico.keyrisdk.exception.AuthorizationException
import com.keyrico.keyrisdk.exception.InternalServerException
import com.keyrico.keyrisdk.exception.NetworkException
import com.keyrico.keyrisdk.exception.RiskErrorsException
import com.keyrico.keyrisdk.exception.ServerErrorException
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
                in 400..599 -> InternalServerException(response.code())
                else -> try {
                    val errorBody = response.errorBody()

                    val errorResponse: IpDataError? = Gson().fromJson(
                        errorBody?.charStream(),
                        object : TypeToken<IpDataError?>() {}.type
                    )

                    errorBody?.close()
                    RiskErrorsException(errorResponse?.riskErrors ?: emptyList())
                } catch (e: JsonSyntaxException) {
                    val errorBody = response.errorBody()

                    val errorResponse: String? = Gson().fromJson(
                        errorBody?.charStream(),
                        object : TypeToken<String?>() {}.type
                    )

                    errorBody?.close()
                    ServerErrorException(response.code(), errorResponse)
                }
            }
        }

        return response
    } catch (e: Exception) {
        throw when (e) {
            is IOException -> {
                when (e) {
                    is UnknownHostException,
                    is SocketTimeoutException,
                    is ConnectException -> NetworkException
                    else -> e
                }
            }
            is MalformedJsonException, is JsonSyntaxException -> AuthorizationException
            else -> e
        }
    }
}
