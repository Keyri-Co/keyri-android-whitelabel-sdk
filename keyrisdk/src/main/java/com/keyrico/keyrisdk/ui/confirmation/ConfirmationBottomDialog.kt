package com.keyrico.keyrisdk.ui.confirmation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.keyrico.keyrisdk.R
import com.keyrico.keyrisdk.databinding.DialogConfirmationBinding
import com.keyrico.keyrisdk.entity.RiskMessageTypes
import com.keyrico.keyrisdk.entity.Session

class ConfirmationBottomDialog(
    override val session: Session,
    override val onResult: (isAccepted: Boolean) -> Unit
) : BaseConfirmationBottomDialog(session, onResult) {

    private lateinit var binding: DialogConfirmationBinding
    private val riskAnalyticsEnabled by lazy { session.riskAnalytics != null }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogConfirmationBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun initUI() {
        initWidgetLocation()
        initMobileLocation()
        initWidgetAgent()
        initButtons()
    }

    private fun initWidgetLocation() {
        with(binding) {
            val geoData = session.riskAnalytics?.geoData

            llWidgetLocation.isVisible = riskAnalyticsEnabled && geoData != null
            tvWidgetLocation.isVisible = true
            tvVPNDetected.isVisible = false

            geoData?.let {
                tvWidgetLocation.text = getString(
                    R.string.keyri_confirmation_screen_near,
                    "${it.city}, ${session.iPDataWidget?.countryName ?: it.countryCode}"
                )
            }
        }
    }

    private fun initMobileLocation() {
        with(binding) {
            llMobileLocation.isVisible = riskAnalyticsEnabled && session.iPDataMobile != null
            tvMobileLocation.isVisible = true

            session.iPDataMobile?.let {
                tvMobileLocation.text = getString(
                    R.string.keyri_confirmation_screen_near,
                    "${it.city}, ${it.countryName}"
                )
            }
        }
    }

    private fun initWidgetAgent() {
        with(binding) {
            llWidgetAgent.isVisible = riskAnalyticsEnabled && session.widgetUserAgent != null
            tvWidgetAgent.text = session.widgetUserAgent
        }
    }

    private fun initButtons() {
        with(binding) {
            val authenticationDenied =
                session.riskAnalytics?.getRiskStatusType() == RiskMessageTypes.DANGER

            tvErrorMessage.isVisible = authenticationDenied
            llButtons.isVisible = !authenticationDenied

            bNo.setOnClickListener {
                dismiss()
            }

            bYes.setOnClickListener {
                accepted = true
                dismiss()
            }
        }
    }
}
