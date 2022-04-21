package com.keyrico.keyrisdk.ui.confirmation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.keyrico.keyrisdk.databinding.ActivityConfirmationBinding
import com.keyrico.keyrisdk.ui.auth.AuthWithScannerState

internal class ConfirmationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmationBinding

    private val riskAnalyticsInfoAdapted by lazy { RiskAnalyticsInfoAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeUi()
    }

    private fun initializeUi() {
        with(binding) {
            bAccept.setOnClickListener {
                val intent = Intent().apply {
                    putExtra(KEY_CONFIRMATION_RESULT, true)
                }

                setResult(RESULT_OK, intent)
                finish()
            }

            bDecline.setOnClickListener { finish() }
            ivClose.setOnClickListener { finish() }

            intent.getParcelableExtra<AuthWithScannerState.Confirmation>(KEY_AUTH_STATE)?.let {
                tvAvatar.text = it.username.firstOrNull()?.toString()
                tvUsername.text = it.username

                if (it.characteristics != null) {
                    makeListVisible(true)

                    rvRiskAnalyticsInfo.adapter = riskAnalyticsInfoAdapted

                    val characteristics = it.characteristics.map { characteristic ->
                        RiskAnalyticsItem(characteristic.key, characteristic.value)
                    }

                    riskAnalyticsInfoAdapted.submitList(characteristics)
                } else if (it.message != null) {
                    makeListVisible(false)

                    tvMessage.text = it.message
                }
            }
        }
    }

    private fun makeListVisible(isListVisible: Boolean) {
        with(binding) {
            rvRiskAnalyticsInfo.isVisible = isListVisible
            tvMessage.isVisible = !isListVisible
        }
    }

    companion object {
        const val KEY_AUTH_STATE = "KEY_AUTH_STATE"
        const val KEY_CONFIRMATION_RESULT = "KEY_CONFIRMATION_RESULT"
    }
}
