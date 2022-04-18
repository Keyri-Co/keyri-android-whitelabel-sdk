package com.keyrico.keyrisdk.ui.confirmation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.keyrico.keyrisdk.databinding.ActivityConfirmationBinding

internal class ConfirmationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmationBinding

    private val riskAnalyticsInfoAdapted by lazy {
        RiskAnalyticsInfoAdapter()
    }

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

            // TODO Change
            tvAvatar.text = "M"
            tvEmail.text = "Mocked username"

            rvRiskAnalyticsInfo.adapter = riskAnalyticsInfoAdapted

            riskAnalyticsInfoAdapted.submitList(
                listOf(
                    RiskAnalyticsItem("Device", "Intel Mac OS X 10_15_7"),
                    RiskAnalyticsItem("Near", "Hillsboro OR, USA"),
                    RiskAnalyticsItem("Time", "Just now")
                )
            )
        }
    }

    companion object {
        const val KEY_CONFIRMATION_RESULT = "KEY_CONFIRMATION_RESULT"
    }
}
