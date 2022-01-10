package com.keyri.di

import com.example.keyrisdk.KeyriConfig
import com.example.keyrisdk.KeyriSdk
import com.keyri.BuildConfig
import org.koin.dsl.module

/**
 * Koin module that provides Keyri.
 */
val keyriModule = module {
    single {
        KeyriConfig(
            appKey = BuildConfig.APP_KEY,
            publicKey = BuildConfig.PUBLIC_KEY,
            callbackUrl = BuildConfig.KEYRI_CALLBACK_URL,
            allowMultipleAccounts = true
        )
    }
    single { KeyriSdk(get(), get()) }
}
