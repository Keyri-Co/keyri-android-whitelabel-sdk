package com.keyrico.keyrisdk.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.keyrico.keyrisdk.KeyriConfig
import com.keyrico.keyrisdk.KeyriSdk
import com.keyrico.keyrisdk.databinding.ActivityAuthWithScannerBinding
import com.keyrico.keyrisdk.entity.PublicAccount
import com.keyrico.keyrisdk.view.KeyriScannerViewParams

class AuthWithScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthWithScannerBinding

    private val viewModel by viewModels<AuthWithScannerVM>()

    private val adapter by lazy { KeyriAccountsAdapter(::onAccountClicked) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthWithScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeUi()
    }

    private fun initializeUi() {
        intent.getParcelableExtra<KeyriConfig?>(KEY_CONFIG)?.let { config ->
            binding.rvAccounts.adapter = adapter

            val params = KeyriScannerViewParams(
                activity = this,
                keyriSdk = KeyriSdk(this, config),
                customArgument = intent.getStringExtra(KEY_CUSTOM_ARG),
                onChooseAccount = { accounts, sessionId, service ->
                    viewModel.init(accounts, service, sessionId)
                    changeListVisibility(true)
                },
                onCompleted = {
                    changeListVisibility(false)
                    setResult(RESULT_OK)
                    finish()
                }
            )

            binding.vKeyriScanner.initView(params)

            viewModel.accountsLD.observe(this, adapter::submitList)
        } ?: finish()
    }

    private fun onAccountClicked(publicAccount: PublicAccount) {
        changeListVisibility(false)

        val sessionId = viewModel.sessionId
        val service = viewModel.service

        if (sessionId != null && service != null) {
            binding.vKeyriScanner.continueAuth(publicAccount, sessionId, service)
            viewModel.clear()
        }
    }

    private fun changeListVisibility(isVisible: Boolean) {
        binding.rlChooseAccount.isVisible = isVisible
        binding.vKeyriScanner.isVisible = !isVisible
    }

    companion object {
        const val KEY_CONFIG = "KEY_CONFIG"
        const val KEY_CUSTOM_ARG = "KEY_CUSTOM_ARG"
    }
}
