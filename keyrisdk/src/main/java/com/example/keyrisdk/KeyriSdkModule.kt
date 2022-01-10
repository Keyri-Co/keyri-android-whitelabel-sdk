package com.example.keyrisdk

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.keyrisdk.db.AppDb
import com.example.keyrisdk.services.*
import com.example.keyrisdk.services.api.ApiService
import com.example.keyrisdk.services.crypto.CryptoService
import com.example.keyrisdk.services.socket.SocketService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class KeyriSdkModule(private val context: Context) {

    fun provideApiService(): ApiService {
        val okHttpClientBuilder = OkHttpClient.Builder()

        okHttpClientBuilder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(loggingInterceptor)
        }

        val client = okHttpClientBuilder.build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }

    fun provideStorageService() =
        StorageService(getSharedPreferences(), provideUserDao(), provideCryptoService())

    fun provideUserService() = UserService(
        provideStorageService(),
        provideSessionService(),
        provideApiService(),
        provideCryptoService()
    )

    fun provideCryptoService() = CryptoService(getSharedPreferences())

    private fun provideAppDb(): AppDb =
        Room.databaseBuilder(context, AppDb::class.java, DB_NAME).build()

    private fun provideUserDao() = provideAppDb().userDao()

    private fun provideSessionService() =
        SessionService(provideSocketService(), provideCryptoService())

    private fun provideSocketService() = SocketService(BuildConfig.WS_URL)

    private fun getSharedPreferences(): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "keyri_prefs"
        private const val DB_NAME = "db_keyri_sdk"
        private const val CONNECT_TIMEOUT = 15L
        private const val READ_TIMEOUT = 60L
    }
}
