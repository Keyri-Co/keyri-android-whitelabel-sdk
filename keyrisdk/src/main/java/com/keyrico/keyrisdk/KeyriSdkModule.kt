package com.keyrico.keyrisdk

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.keyrico.keyrisdk.db.AppDb
import com.keyrico.keyrisdk.services.StorageService
import com.keyrico.keyrisdk.services.UserService
import com.keyrico.keyrisdk.services.api.ApiService
import com.keyrico.keyrisdk.services.crypto.CryptoService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class KeyriSdkModule(private val context: Context, private val rpPublicKey: String) {

    // TODO How to handle isDebug?
    private val isDebug = true

    fun provideApiService(): ApiService {
        val okHttpClientBuilder = OkHttpClient.Builder()

        okHttpClientBuilder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(loggingInterceptor)
        }

        return Retrofit.Builder()
            .baseUrl(if (isDebug) DEV_API_URL else API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClientBuilder.build())
            .build()
            .create(ApiService::class.java)
    }

    fun provideStorageService() =
        StorageService(getSharedPreferences(), provideUserDao(), provideCryptoService())

    fun provideUserService() = UserService(
        provideStorageService(),
        provideApiService(),
        provideCryptoService(),
        rpPublicKey
    )

    fun provideCryptoService() = CryptoService(getSharedPreferences())

    private fun provideAppDb(): AppDb =
        Room.databaseBuilder(context, AppDb::class.java, DB_NAME + context.packageName).build()

    private fun provideUserDao() = provideAppDb().userDao()

    private fun getSharedPreferences(): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val DEV_API_URL = "https://dev-api.keyri.co"
        private const val API_URL = "https://api.keyri.co"

        private const val PREFS_NAME = "keyri_prefs"
        private const val DB_NAME = "db_keyri_sdk"
        private const val CONNECT_TIMEOUT = 15L
        private const val READ_TIMEOUT = 60L
    }
}
