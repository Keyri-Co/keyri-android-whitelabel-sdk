package com.keyri.accounts

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.keyri.HomeActivity
import com.keyri.R
import kotlinx.android.synthetic.main.activity_accounts.panelContent
import kotlinx.android.synthetic.main.activity_new_account.*
import kotlinx.android.synthetic.main.layout_progress.*

class NewAccountActivity : AppCompatActivity() {

    private val viewModel by viewModels<NewAccountVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_account)

        viewModel.message().observe(this, Observer(::onMessage))
        viewModel.loading().observe(this, Observer(::onLoading))
        viewModel.authenticated().observe(this, Observer { openHomeActivity() })

        setupUi()
    }

    private fun setupUi() {
        etUsername.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doMobileSignup()
            }
            false
        }

        btSignupMobile.setOnClickListener { doMobileSignup() }
    }

    private fun doMobileSignup() {
        viewModel.mobileSignup(etUsername.text.toString())
    }

    private fun onMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun onLoading(isLoading: Boolean) {
        if (isLoading) {
            panelContent.visibility = View.GONE
            progress.visibility = View.VISIBLE
        } else {
            panelContent.visibility = View.VISIBLE
            progress.visibility = View.GONE
        }
    }

    private fun openHomeActivity() {
        HomeActivity.openHomeActivity(this)
    }

    companion object {
        fun openNewAccountActivity(activity: AppCompatActivity) {
            activity.startActivity(Intent(activity, NewAccountActivity::class.java))
        }
    }

}
