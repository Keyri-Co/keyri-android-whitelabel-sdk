package com.keyrico.keyrisdk

import android.content.Context
import android.content.SharedPreferences
import com.keyrico.keyrisdk.entity.Service
import com.keyrico.keyrisdk.services.api.InitRequest
import com.keyrico.keyrisdk.services.crypto.NewFlowCryptoService
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

    // TODO Remove sessionL/S and add ...session
    // TODO Чекнуть схему и отправить Андрею айос актуальную схему

    private val api = createNetworkApi()
    private val cryptoService = NewFlowCryptoService(getSharedPreferences())
    private var service: Service? = null

    suspend fun handleSessionId(sessionId: String, custom: String = "TEST CUSTOM") {
        initSdk()

        service ?: throw IllegalStateException()

        val response = makeApiCall { api.getSession(sessionId, config.appKey) }.body()

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

            val secondResponse = makeApiCall { api.secondPost(sessionId, request) }.body()

            // TODO Need authenticated status based on response
            if (secondResponse == "success") {
                // TODO Show success
            } else {
                // TODO Show failure
            }
        } else {
            // TODO Show user decline auth dialog
        }
    }

    private suspend fun initSdk() {
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
