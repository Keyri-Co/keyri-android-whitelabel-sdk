package com.keyri.di

import com.keyri.BuildConfig
import com.keyrico.keyrisdk.KeyriSdk
import org.koin.dsl.module

/**
 * Koin module that provides Keyri.
 */
val keyriModule = module {
    single {
        KeyriConfig(
            appKey = BuildConfig.APP_KEY,
            rpPublicKey = BuildConfig.RP_PUBLIC_KEY,
            callbackUrl = BuildConfig.KEYRI_CALLBACK_URL,
            allowMultipleAccounts = true
        )
    }
    single { KeyriSdk(get(), get()) }
}
