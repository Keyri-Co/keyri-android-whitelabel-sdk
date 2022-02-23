package com.keyrico.keyrisdk.new_auth_flow

import android.content.Context
import android.content.SharedPreferences
import com.keyrico.keyrisdk.BuildConfig
import com.keyrico.keyrisdk.KeyriSdkModule
import com.keyrico.keyrisdk.entity.Service
import com.keyrico.keyrisdk.services.api.InitRequest
import com.keyrico.keyrisdk.utils.makeApiCall
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NewAuthFlow(
    private val context: Context,
    private val config: NewFlowConfig,
    private val keyriSdkModule: KeyriSdkModule
) {

    private val api = createNetworkApi()

    private val cryptoService = NewFlowCryptoService(getSharedPreferences())

    private var service: Service? = null

    suspend fun startNewAuthFlow(sessionId: String, custom: String = "TEST CUSTOM") {
        initSdk()

        val headers = mapOf("appKey" to config.appKey)
        val response = makeApiCall { api.firstPost(headers, FirstRequest(sessionId)) }.body()

        if (response?.username != null) {
            // TODO Generate keypair for Username
            cryptoService.generateSecretKey(response.username, custom, config.publicKey)
        }

        if (config.domainName != response?.serviceDomain) throw IllegalStateException()

        // TODO Display confirmation with following fields:
        response.userAgent
        response.riskCharacteristics

        val userConfirmation = true

        if (userConfirmation) {
            // TODO If user confirms confirmation screen:
            val cipher =
                cryptoService.encryptAes("${response.username}, ${custom}, ${System.currentTimeMillis()}")

            val request = SecondRequest(
                PublicObject(response.username ?: "", config.publicKey),
                cipher,
                sessionId
            )

            api.secondPost(request)
            // TODO Return authenticated status based on response
        } else {
            // TODO Show user decline auth dialog
        }
    }

    private suspend fun initSdk() {
        // TODO Init with [RP public key, appKey, service domain]

        if (service != null) return
        val deviceId =
            keyriSdkModule.provideStorageService().getDeviceId() ?: throw IllegalStateException()

        val request = InitRequest(deviceId, config.appKey)
        val response = makeApiCall { keyriSdkModule.provideApiService().init(request) }.body()
        service = response?.service
    }

    private fun createNetworkApi(): NewAuthFlowApi {
        val okHttpClientBuilder = OkHttpClient.Builder().apply {
            connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        }

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(loggingInterceptor)
        }

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClientBuilder.build())
            .build()
            .create(NewAuthFlowApi::class.java)
    }

    private fun getSharedPreferences(): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val CONNECT_TIMEOUT = 15L
        private const val READ_TIMEOUT = 60L

        private const val PREFS_NAME = "keyri_prefs"
    }
}
