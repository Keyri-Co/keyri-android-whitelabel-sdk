package com.keyri.auth_with_scanner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.keyrisdk.KeyriSdk
import com.example.keyrisdk.view.KeyriScannerViewParams
import com.keyri.databinding.ActivityAuthWithScannerBinding
import com.keyri.home.HomeActivity
import org.koin.android.ext.android.inject

class AuthWithScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthWithScannerBinding

    private val keyriSdk by inject<KeyriSdk>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthWithScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeUi()
    }

    private fun initializeUi() {
        val customArg: String? = intent.getStringExtra(KEY_CUSTOM_ARG)

        val params = KeyriScannerViewParams(
            activity = this,
            keyriSdk = keyriSdk,
            customArgument = customArg,
            onCompleted = { HomeActivity.openHomeActivity(this) }
        )

        binding.vKeyriScanner.initView(params)
    }

    companion object {
        private const val KEY_CUSTOM_ARG = "KEY_CUSTOM_ARG"

        fun openAuthWithScannerActivity(activity: AppCompatActivity, customArg: String) {
            activity.startActivity(Intent(activity, AuthWithScannerActivity::class.java).apply {
                putExtra(KEY_CUSTOM_ARG, customArg)
            })
        }
    }
}
