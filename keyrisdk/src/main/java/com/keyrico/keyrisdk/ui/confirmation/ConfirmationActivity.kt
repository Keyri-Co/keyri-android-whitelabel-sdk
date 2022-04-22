package com.keyrico.keyrisdk.ui.confirmation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.keyrico.keyrisdk.R
import com.keyrico.keyrisdk.databinding.ActivityConfirmationBinding
import com.keyrico.keyrisdk.entity.RiskMessageTypes
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
                tvAvatar.text = it.username?.firstOrNull()?.toString()
                tvUsername.text = it.username

                val riskStatusType = it.riskAnalytics.getRiskStatusType()

                val colorRes = when (riskStatusType) {
                    RiskMessageTypes.FINE -> R.color.green
                    RiskMessageTypes.WARNING -> R.color.orange
                    RiskMessageTypes.DANGER -> R.color.red
                }

                tvRiskStatus.setTextColor(getColor(colorRes))
                tvRiskStatus.text = riskStatusType.type

                rvRiskAnalyticsInfo.adapter = riskAnalyticsInfoAdapted

                val geoData = it.riskAnalytics.geoData

                val characteristics = listOf(
                    RiskAnalyticsItem("Origin", it.widgetOrigin),
                    RiskAnalyticsItem("Agent", it.widgetUserAgent),
                    RiskAnalyticsItem("Widget IP", it.iPAddressWidget),
                    RiskAnalyticsItem("City", "${geoData.city}, ${geoData.country_code}")
                )

                riskAnalyticsInfoAdapted.submitList(characteristics)
            }
        }
    }

    companion object {
        const val KEY_AUTH_STATE = "KEY_AUTH_STATE"
        const val KEY_CONFIRMATION_RESULT = "KEY_CONFIRMATION_RESULT"
    }
}
