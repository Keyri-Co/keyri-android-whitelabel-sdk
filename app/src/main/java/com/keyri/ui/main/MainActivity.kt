package com.keyri.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.keyri.BuildConfig
import com.keyri.databinding.ActivityAuthBinding
import com.keyri.ui.auth_complete.AuthCompleteActivity
import com.keyrico.keyrisdk.KeyriSdk
import com.keyrico.keyrisdk.ShowEasyKeyriAuth

class MainActivity : AppCompatActivity() {

    private val keyriSdk by lazy {
        KeyriSdk(this, BuildConfig.APP_KEY, BuildConfig.DOMAIN_NAME)
    }

    private val easyKeyriAuthLauncher = registerForActivityResult(ShowEasyKeyriAuth()) {
        val intent = Intent(this, AuthCompleteActivity::class.java).apply {
            putExtra(AuthCompleteActivity.KEY_IS_SUCCESS, it)
        }

        startActivity(intent)
    }

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bAuthQr.setOnClickListener {
            keyriSdk.easyKeyriAuth(
                easyKeyriAuthLauncher,
                "mocked-public-user-id",
                "mocked-username",
                "secure custom",
                "public custom"
            )
        }
    }
}
