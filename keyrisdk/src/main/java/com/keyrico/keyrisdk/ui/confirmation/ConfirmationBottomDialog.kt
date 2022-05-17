package com.keyrico.keyrisdk.ui.confirmation

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
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
    private val authenticationDenied by lazy { session.riskAnalytics?.getRiskStatusType() == RiskMessageTypes.DANGER }
    private val authenticationWarning by lazy { session.riskAnalytics?.getRiskStatusType() == RiskMessageTypes.WARNING }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogConfirmationBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun initUI() {
        val colorRes = if (authenticationWarning) {
            R.color.orange.getColor() to R.color.light_orange.getColor()
        } else if (authenticationDenied) {
            R.color.red.getColor() to R.color.vpn_red.getColor()
        } else null

        initWidgetLocation(colorRes)
        initMobileLocation(colorRes)
        initWidgetAgent()
        initButtons()
    }

    private fun initWidgetLocation(colorRes: Pair<Int, Int>?) {
        with(binding) {
            val iPDataMobile = session.iPDataWidget
            val city = iPDataMobile?.city
            val countryName = iPDataMobile?.countryName ?: iPDataMobile?.countryCode

            llWidgetLocation.isVisible = riskAnalyticsEnabled && city != null && countryName != null
            tvWidgetLocation.isVisible = true
            tvVPNDetected.isVisible = false

            tvWidgetLocation.text =
                getString(R.string.keyri_confirmation_screen_near, "$city, $countryName")

            colorRes?.let {
                tvWidgetLocation.setTextColor(it.first)
                ivWidgetLocation.setColorFilter(it.first)
                tvVPNDetected.setTextColor(it.second)
                tvVPNDetected.setDrawableColor(it.second)
            }
        }
    }

    private fun initMobileLocation(colorRes: Pair<Int, Int>?) {
        with(binding) {
            val iPDataMobile = session.iPDataMobile
            val city = iPDataMobile?.city
            val countryName = iPDataMobile?.countryName ?: iPDataMobile?.countryCode

            llMobileLocation.isVisible = riskAnalyticsEnabled && city != null && countryName != null
            tvMobileLocation.isVisible = true

            tvMobileLocation.text =
                getString(R.string.keyri_confirmation_screen_near, "$city, $countryName")

            colorRes?.let {
                tvMobileLocation.setTextColor(it.first)
                ivMobileLocation.setColorFilter(it.first)
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
            tvErrorMessage.isVisible = authenticationDenied
            llButtons.isVisible = !authenticationDenied

            bNo.setOnClickListener { dismiss() }

            bYes.setOnClickListener {
                accepted = true
                dismiss()
            }
        }
    }

    private fun TextView.setDrawableColor(color: Int) {
        this.compoundDrawables.filterNotNull().forEach {
            it.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun Int.getColor(): Int {
        return ContextCompat.getColor(requireContext(), this)
    }
}
