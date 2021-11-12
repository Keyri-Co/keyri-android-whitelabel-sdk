package com.keyri

import android.app.Application
import com.example.keyrisdk.KeyriConfig
import com.example.keyrisdk.KeyriSdk

class KeyriApp : Application() {

    override fun onCreate() {
        super.onCreate()

        KeyriSdk.initialize(this, getKeyriConfig())
    }

    private fun getKeyriConfig() =
        KeyriConfig(
            appKey = BuildConfig.APP_KEY,
            publicKey = BuildConfig.PUBLIC_KEY,
            callbackUrl = BuildConfig.KEYRI_CALLBACK_URL,
            allowMultipleAccounts = true
        )
}
