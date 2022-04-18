package com.keyri.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.keyri.BuildConfig
import com.keyri.databinding.ActivityAuthBinding
import com.keyri.ui.auth_complete.AuthCompleteActivity
import com.keyrico.keyrisdk.KeyriSdk

class MainActivity : AppCompatActivity() {

    private val keyriSdk by lazy {
        KeyriSdk(
            this,
            BuildConfig.RP_PUBLIC_KEY,
            BuildConfig.DOMAIN_NAME
        )
    }

    private lateinit var binding: ActivityAuthBinding

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AUTH_REQUEST_CODE) {
            val intent = Intent(this, AuthCompleteActivity::class.java).apply {
                putExtra(AuthCompleteActivity.KEY_IS_SUCCESS, resultCode == Activity.RESULT_OK)
            }

            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bAuthQr.setOnClickListener {
            keyriSdk.easyKeyriAuth(
                "mocked-public-user-id",
                this,
                AUTH_REQUEST_CODE,
                "secure custom",
                "public custom"
            )
        }
    }

    companion object {
        private const val AUTH_REQUEST_CODE = 2123
    }
}
