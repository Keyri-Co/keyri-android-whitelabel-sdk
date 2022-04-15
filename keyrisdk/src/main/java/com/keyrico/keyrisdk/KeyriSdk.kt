package com.keyrico.keyrisdk

import android.content.Context
import android.content.SharedPreferences
import com.keyrico.keyrisdk.entity.Session
import com.keyrico.keyrisdk.exception.AuthorizationException
import com.keyrico.keyrisdk.services.CryptoService
import com.keyrico.keyrisdk.services.UserService
import com.keyrico.keyrisdk.services.api.ApiService
import com.keyrico.keyrisdk.utils.makeApiCall
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class KeyriSdk(
    private val context: Context,
    private val rpPublicKey: String,
    private val serviceDomain: String
) {

    private val apiService by lazy { provideApiService() }
    private val userService by lazy { provideUserService() }
    private val cryptoService by lazy { provideCryptoService() }

    // TODO Remove
    private var salt = ""
    private var hash = ""

    // TODO Add Throws to all public methods
    // TODO Remove all unused methods
    suspend fun generateAssociationKey(publicUserId: String) {
        cryptoService.generateAssociationKey(publicUserId)
    }

    suspend fun getAssociationKey(publicUserId: String): String? {
        return cryptoService.getAssociationKey(publicUserId)
    }

    @Throws(AuthorizationException::class)
    suspend fun handleSessionId(sessionId: String): Session {
        val session = makeApiCall {
            apiService.getSession(sessionId, "IT7VrTQ0r4InzsvCNJpRCRpi1qzfgpaj")
        }.body() ?: throw AuthorizationException

        salt = session.salt
        hash = session.hash

        // TODO Compare returned serviceDomain from Keyri API to serviceDomain from SDK setup
        serviceDomain == session.widgetOrigin

        return session

        // TODO Show confirmation screen and risk characteristics (if allowed and outer of SDK)
        // TODO Auth with scanner - Show confirmation screen
    }

    suspend fun challengeSession(
        publicUserId: String,
        sessionId: String,
        secureCustom: String?,
        publicCustom: String?
    ) {
        userService.challengeSession(publicUserId, sessionId, secureCustom, publicCustom, salt, hash)
    }

    suspend fun easyKeyriAuth(
        publicUserId: String,
        secureCustom: String?,
        publicCustom: String?
    ) {

        // TODO Add Implementation
    }

    private fun provideApiService(): ApiService {
        val okHttpClientBuilder = OkHttpClient.Builder()

        okHttpClientBuilder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(loggingInterceptor)
        }

        return Retrofit.Builder()
            .baseUrl("https://test.api.keyri.com")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClientBuilder.build())
            .build()
            .create(ApiService::class.java)
    }

    private fun provideUserService() = UserService(apiService, cryptoService, rpPublicKey)

    private fun provideCryptoService() = CryptoService(getSharedPreferences())

    private fun getSharedPreferences(): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "keyri_prefs"
        private const val CONNECT_TIMEOUT = 15L
        private const val READ_TIMEOUT = 60L
    }
}
