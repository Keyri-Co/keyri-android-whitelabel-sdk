package com.example.keyrisdk.di

import android.content.SharedPreferences
import com.example.keyrisdk.services.*
import com.example.keyrisdk.services.api.ApiService
import com.example.keyrisdk.services.crypto.CryptoService
import com.example.keyrisdk.services.socket.SocketService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [KeyriSdkModule::class])
interface KeyriSdkGraph {
    fun getApiService(): ApiService
    fun getStorageService(): StorageService
    fun getCryptoService(): CryptoService
    fun getSessionService(): SessionService
    fun getUserService(): UserService
    fun getSocketService(): SocketService
    fun getSharedPreferences(): SharedPreferences
}
