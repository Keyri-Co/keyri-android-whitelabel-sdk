package com.keyrico.keyrisdk.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.keyrico.keyrisdk.BuildConfig
import com.keyrico.keyrisdk.entity.SessionConfirmationResponse
import com.keyrico.keyrisdk.entity.session.RiskAttributes
import com.keyrico.keyrisdk.exception.AuthorizationException
import com.keyrico.keyrisdk.exception.InternalServerException
import com.keyrico.keyrisdk.exception.NetworkException
import com.keyrico.keyrisdk.services.api.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.reflect.Type
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val TIMEOUT = 15L

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

    okHttpClientBuilder.connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)

    if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }.let(okHttpClientBuilder::addInterceptor)
    }

    val type = object : TypeToken<RiskAttributes>() {}.type
    val gson = GsonBuilder().registerTypeAdapter(type, RiskAttributesDeserializer()).create()

    return Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClientBuilder.build())
        .build()
        .create(ApiService::class.java)
}

internal class RiskAttributesDeserializer : JsonDeserializer<RiskAttributes> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): RiskAttributes {
        val jsonArray = json?.asJsonArray

        var isKnownAbuser = false
        var isIcloudRelay = false
        var isKnownAttacker = false
        var isAnonymous = false
        var isThreat = false
        var isBogon = false
        var blocklists = false
        var isDatacenter = false
        var isTor = false
        var isProxy = false

        jsonArray?.forEach { jsonElement ->
            val jsObject = jsonElement.asJsonObject

            when {
                jsObject?.has("is_known_abuser") == true ->
                    isKnownAbuser =
                        jsObject["is_known_abuser"]?.asBoolean ?: false
                jsObject?.has("is_icloud_relay") == true ->
                    isIcloudRelay =
                        jsObject["is_icloud_relay"]?.asBoolean ?: false
                jsObject?.has("is_known_attacker") == true ->
                    isKnownAttacker =
                        jsObject["is_known_attacker"]?.asBoolean ?: false
                jsObject?.has("is_anonymous") == true ->
                    isAnonymous =
                        jsObject["is_anonymous"]?.asBoolean ?: false
                jsObject?.has("is_threat") == true ->
                    isThreat =
                        jsObject["is_threat"]?.asBoolean ?: false
                jsObject?.has("is_bogon") == true ->
                    isBogon =
                        jsObject["is_bogon"]?.asBoolean ?: false
                jsObject?.has("blocklists") == true ->
                    blocklists =
                        jsObject["blocklists"]?.asBoolean ?: false
                jsObject?.has("is_datacenter") == true ->
                    isDatacenter =
                        jsObject["is_datacenter"]?.asBoolean ?: false
                jsObject?.has("is_tor") == true -> isTor = jsObject["is_tor"]?.asBoolean ?: false
                jsObject?.has("is_proxy") == true ->
                    isProxy =
                        jsObject["is_proxy"]?.asBoolean ?: false
            }
        }

        return RiskAttributes(
            isKnownAbuser = isKnownAbuser,
            isIcloudRelay = isIcloudRelay,
            isKnownAttacker = isKnownAttacker,
            isAnonymous = isAnonymous,
            isThreat = isThreat,
            isBogon = isBogon,
            blocklists = blocklists,
            isDatacenter = isDatacenter,
            isTor = isTor,
            isProxy = isProxy
        )
    }
}
