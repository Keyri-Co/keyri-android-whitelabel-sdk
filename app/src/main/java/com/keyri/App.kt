package com.keyri

import android.app.Application
import com.keyri.di.viewModelsModule
import com.keyri.di.keyriModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin

class App : Application(), KoinComponent {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(keyriModule, viewModelsModule)
        }
    }
}
