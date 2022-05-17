package com.keyrico.keyrisdk

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.result.ActivityResultLauncher
import com.google.gson.JsonObject
import com.keyrico.keyrisdk.entity.Session
import com.keyrico.keyrisdk.exception.AuthorizationException
import com.keyrico.keyrisdk.exception.WrongOriginDomainException
import com.keyrico.keyrisdk.services.CryptoService
import com.keyrico.keyrisdk.services.api.ApiService
import com.keyrico.keyrisdk.services.api.ChallengeSessionRequest
import com.keyrico.keyrisdk.services.api.PublicObject
import com.keyrico.keyrisdk.services.api.ServerData
import com.keyrico.keyrisdk.utils.makeApiCall
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class KeyriSdk(
    private val context: Context,
    private val appKey: String,
    private val serviceDomain: String
) {
    private val apiService by lazy { provideApiService() }
    private val cryptoService by lazy { provideCryptoService() }

    private var sessionSalt = ""
    private var sessionHash = ""

    init {
        cryptoService.createSignatureKeypair()
    }

    fun generateAssociationKey(publicUserId: String) {
        cryptoService.generateAssociationKey(publicUserId)
    }

    fun getAssociationKey(publicUserId: String): String? {
        return cryptoService.getAssociationKey(publicUserId)
    }

    fun createSignature(publicUserId: String, message: String): String {
        return cryptoService.signMessage(publicUserId, message)
    }

    suspend fun initiateSession(sessionId: String): Session {
        val session = makeApiCall { apiService.getSession(sessionId, appKey) }.body()
            ?: throw AuthorizationException("Unable to authorize")

        if (session.widgetOrigin != serviceDomain) throw WrongOriginDomainException("Wrong Origin domain")

        sessionSalt = session.salt
        sessionHash = session.hash

        return session
    }

    suspend fun approveSession(
        publicUserId: String,
        username: String?,
        browserPublicKey: String,
        sessionId: String,
        secureCustom: String?,
        publicCustom: String?
    ) {
        val toEncrypt = JsonObject().also {
            it.addProperty("publicUserId", publicUserId)
            it.addProperty("timestamp", System.currentTimeMillis())
            it.addProperty("secureCustom", secureCustom)
        }.toString()

        val cipher = cryptoService.encryptHkdf(browserPublicKey, toEncrypt)
        val signaturePublicKey = cryptoService.getSignaturePublicKey()

        val publicObject = PublicObject(username, signaturePublicKey, publicCustom)
        val serverData = ServerData(cipher.publicKey, cipher.cipherText, cipher.salt, cipher.iv)
        val request = ChallengeSessionRequest(serverData, publicObject, sessionSalt, sessionHash)

        apiService.challengeSession(sessionId, request)
    }

    fun easyKeyriAuth(
        launcher: ActivityResultLauncher<EasyKeyriAuthParams>,
        publicUserId: String,
        username: String?,
        secureCustom: String?,
        publicCustom: String?
    ) {
        EasyKeyriAuthParams(
            appKey,
            serviceDomain,
            publicUserId,
            username,
            publicCustom,
            secureCustom
        ).let(launcher::launch)
    }

    private fun provideApiService(): ApiService {
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

    private fun provideCryptoService() = CryptoService(getSharedPreferences())

    private fun getSharedPreferences(): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "keyri_prefs"
        private const val CONNECT_TIMEOUT = 15L
        private const val READ_TIMEOUT = 15L
    }
}
