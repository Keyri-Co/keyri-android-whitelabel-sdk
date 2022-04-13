package com.keyri.di

import com.keyri.BuildConfig
import com.keyrico.keyrisdk.KeyriSdk
import org.koin.dsl.module

val keyriModule = module {
    single { KeyriSdk(get(), BuildConfig.RP_PUBLIC_KEY, "example.com") }
}
