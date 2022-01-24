package com.keyri.auth_with_scanner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.keyri.databinding.ActivityAuthWithScannerBinding
import com.keyri.home.HomeActivity
import com.keyrico.keyrisdk.KeyriSdk
import com.keyrico.keyrisdk.entity.PublicAccount
import com.keyrico.keyrisdk.view.KeyriScannerViewParams
import com.keyri.databinding.ActivityAuthWithScannerBinding
import com.keyri.home.HomeActivity
import com.keyrico.keyrisdk.ui.KeyriAccountsAdapter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthWithScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthWithScannerBinding

    private val keyriSdk by inject<KeyriSdk>()
    private val viewModel by viewModel<AuthWithScannerVM>()

    private val adapter by lazy { KeyriAccountsAdapter(::onAccountClicked) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthWithScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeUi()
    }

    private fun initializeUi() {
        binding.rvAccounts.adapter = adapter

        val customArg: String? = intent.getStringExtra(KEY_CUSTOM_ARG)

        val params = KeyriScannerViewParams(
            activity = this,
            keyriSdk = keyriSdk,
            customArgument = customArg,
            onChooseAccount = { accounts, sessionId, service ->
                viewModel.init(accounts, service, sessionId)
                changeListVisibility(true)
            },
            onCompleted = { HomeActivity.openHomeActivity(this) }
        )

        binding.vKeyriScanner.initView(params)

        viewModel.accountsLD.observe(this, adapter::submitList)
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
        private const val KEY_CUSTOM_ARG = "KEY_CUSTOM_ARG"

        fun openAuthWithScannerActivity(activity: AppCompatActivity, customArg: String) {
            activity.startActivity(
                Intent(activity, AuthWithScannerActivity::class.java).apply {
                    putExtra(KEY_CUSTOM_ARG, customArg)
                }
            )
        }
    }
}
