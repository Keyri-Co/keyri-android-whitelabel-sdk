package com.keyri.accounts

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.keyri.R
import com.keyri.databinding.ActivityNewAccountBinding
import com.keyri.home.HomeActivity
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class NewAccountActivity : AppCompatActivity() {

    private val viewModel by viewModel<NewAccountVM>()

    private lateinit var binding: ActivityNewAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewAccountBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setContentView(R.layout.activity_new_account)

        viewModel.message().observe(this, Observer(::onMessage))
        viewModel.loading().observe(this, Observer(::onLoading))
        viewModel.authenticated().observe(this) { openHomeActivity() }

        setupUi()
    }

    private fun setupUi() {
        binding.etUsername.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doMobileSignup()
            }
            false
        }

        binding.btSignupMobile.setOnClickListener { doMobileSignup() }
    }

    private fun doMobileSignup() {
        viewModel.mobileSignup(binding.etUsername.text.toString())
    }

    private fun onMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun onLoading(isLoading: Boolean) {
        with(binding) {
            if (isLoading) {
                panelContent.visibility = View.GONE
                flProgress.progress.visibility = View.VISIBLE
            } else {
                panelContent.visibility = View.VISIBLE
                flProgress.progress.visibility = View.GONE
            }
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
