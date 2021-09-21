package com.example.keyrisdk.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.keyrisdk.BuildConfig
import com.example.keyrisdk.db.AppDb
import com.example.keyrisdk.db.UserDao
import com.example.keyrisdk.services.*
import com.example.keyrisdk.services.api.ApiService
import com.example.keyrisdk.services.crypto.CryptoService
import com.example.keyrisdk.services.socket.SocketService
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class KeyriSdkModule(private val app: Application) {

    @Provides
    @Singleton
    fun provideContext(): Context {
        return app
    }

    @Provides
    @Singleton
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

    @Provides
    @Singleton
    fun provideAppDb(appContext: Context): AppDb =
        Room.databaseBuilder(appContext, AppDb::class.java, DB_NAME)
            .allowMainThreadQueries()
            .build()

    @Provides
    @Singleton
    fun provideUserDao(db: AppDb) = db.userDao()

    @Provides
    @Singleton
    fun provideCryptoService() =
        CryptoService(app)

    @Provides
    @Singleton
    fun provideStorageService(userDao: UserDao, cryptoService: CryptoService) =
        StorageService(app, userDao, cryptoService)

    @Provides
    @Singleton
    fun provideSessionService(storageService: StorageService, socketService: SocketService, cryptoService: CryptoService) = SessionService(socketService, cryptoService)

    @Provides
    @Singleton
    fun provideUserService(
        storageService: StorageService,
        sessionService: SessionService,
        apiService: ApiService,
        cryptoService: CryptoService
    ) = UserService(storageService, sessionService, apiService, cryptoService)

    @Provides
    @Singleton
    fun provideSocketService() =
        SocketService(BuildConfig.WS_URL)

    companion object {
        private const val DB_NAME = "db_keyri_sdk"
        private const val CONNECT_TIMEOUT = 15L
        private const val READ_TIMEOUT = 60L
    }

}