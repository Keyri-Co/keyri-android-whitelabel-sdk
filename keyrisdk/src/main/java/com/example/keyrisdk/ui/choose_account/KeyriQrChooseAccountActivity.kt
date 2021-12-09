package com.example.keyrisdk.ui.choose_account

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.keyrisdk.R
import com.example.keyrisdk.entity.PublicAccount
import kotlinx.android.synthetic.main.keyri_activity_qr_choose_account.*
import kotlinx.android.synthetic.main.keyri_layout_progress.*

class KeyriQrChooseAccountActivity : AppCompatActivity() {

    private val viewModel by viewModels<KeyriQrChooseAccountVM>()

    private val adapter by lazy {
        KeyriQrAccountsAdapter {
            val username = it.username
            val custom = it.custom

            val result = Intent().apply {
                putExtra(KEY_SESSION_ID, intent.extras?.getString(KEY_SESSION_ID))
                putExtra(KEY_USERNAME, username)
                putExtra(KEY_CUSTOM, custom)
            }

            setResult(RESULT_OK, result)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.keyri_activity_qr_choose_account)
        initUI()
        viewModel.accounts().observe(this, ::setAccounts)
        viewModel.message().observe(this, ::onMessage)
        viewModel.loading().observe(this, ::onLoading)
    }

    private fun initUI() {
        rvAccounts.adapter = adapter
        viewModel.getAccounts()
    }

    private fun setAccounts(accounts: List<PublicAccount>) {
        adapter.submitList(accounts)
    }

    private fun onMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun onLoading(isLoading: Boolean) {
        if (isLoading) {
            rlRoot.visibility = View.INVISIBLE
            progress.visibility = View.VISIBLE
        } else {
            rlRoot.visibility = View.VISIBLE
            progress.visibility = View.INVISIBLE
        }
    }

    companion object {
        const val KEY_SESSION_ID = "KEY_SESSION_ID"
        const val KEY_USERNAME = "KEY_USERNAME"
        const val KEY_CUSTOM = "KEY_CUSTOM"
    }
}
