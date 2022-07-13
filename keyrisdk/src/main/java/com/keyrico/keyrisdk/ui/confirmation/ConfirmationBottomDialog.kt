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
import com.keyrico.keyrisdk.entity.session.RiskAnalytics
import com.keyrico.keyrisdk.entity.session.Session

class ConfirmationBottomDialog(
    override val session: Session,
    override val payload: String,
    override val onResult: ((Result<Boolean>) -> Unit)?
) : BaseConfirmationBottomDialog(session, payload, onResult) {

    private lateinit var binding: DialogConfirmationBinding

    private val riskAnalytics by lazy(session::riskAnalytics)
    private val authenticationDenied by lazy { riskAnalytics?.riskStatus != null && riskAnalytics?.getRiskStatusType() == DENY }
    private val authenticationWarning by lazy {
        riskAnalytics?.getRiskStatusType() == WARNING || riskAnalytics?.riskAttributes?.isAnonymous == true || riskAnalytics?.riskAttributes?.isProxy == true
    }

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
            getColor(R.color.orange) to getColor(R.color.light_orange)
        } else if (authenticationDenied) {
            getColor(R.color.red) to getColor(R.color.vpn_red)
        } else null

        initWidgetLocation(colorRes)
        initMobileLocation(colorRes)
        initWidgetAgent()
        initButtons()
    }

    private fun initWidgetLocation(colorRes: Pair<Int, Int>?) {
        with(binding) {
            val iPDataBrowser = riskAnalytics?.geoData?.browser
            val city = iPDataBrowser?.city
            val countryCode = iPDataBrowser?.countryCode

            llWidgetLocation.isVisible = city != null && countryCode != null
            tvVPNDetected.isVisible =
                riskAnalytics?.riskAttributes?.isAnonymous ?: authenticationDenied

            tvWidgetLocation.text =
                getString(
                    R.string.keyri_confirmation_screen_near,
                    listOf(city, countryCode).joinToString()
                )

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
            val iPDataMobile = riskAnalytics?.geoData?.mobile
            val city = iPDataMobile?.city
            val countryCode = iPDataMobile?.countryCode

            llMobileLocation.isVisible = city != null && countryCode != null

            tvMobileLocation.text =
                getString(
                    R.string.keyri_confirmation_screen_near,
                    listOf(city, countryCode).joinToString()
                )

            colorRes?.let {
                tvMobileLocation.setTextColor(it.first)
                ivMobileLocation.setColorFilter(it.first)
            }
        }
    }

    private fun initWidgetAgent() {
        with(binding) {
            val widgetUserAgent = session.widgetUserAgent

            llWidgetAgent.isVisible = widgetUserAgent != null
            tvWidgetAgent.text =
                listOf(widgetUserAgent?.os, widgetUserAgent?.browser).joinToString()
        }
    }

    private fun initButtons() {
        with(binding) {
            tvErrorMessage.isVisible = authenticationDenied
            llButtons.isVisible = !authenticationDenied

            bNo.setOnClickListener {
                isAccepted = false
                dismiss()
            }

            bYes.setOnClickListener {
                isAccepted = true
                dismiss()
            }
        }
    }

    private fun TextView.setDrawableColor(color: Int) {
        this.compoundDrawables.filterNotNull().forEach {
            it.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun getColor(resId: Int): Int {
        return ContextCompat.getColor(requireContext(), resId)
    }

    companion object {
        private val WARNING = RiskAnalytics.RiskMessageTypes.WARNING
        private val DENY = RiskAnalytics.RiskMessageTypes.DENY
    }
}
