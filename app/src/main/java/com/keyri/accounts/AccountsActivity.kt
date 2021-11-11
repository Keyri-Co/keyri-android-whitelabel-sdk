package com.keyri.accounts

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.keyrisdk.entity.PublicAccount
import com.keyri.HomeActivity
import com.keyri.R
import kotlinx.android.synthetic.main.activity_accounts.*
import kotlinx.android.synthetic.main.layout_progress.*

class AccountsActivity : AppCompatActivity() {

    private val viewModel by viewModels<AccountsVM>()
    private val adapter by lazy { AccountsAdapter(this) { viewModel.processUserAccount(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)

        viewModel.message().observe(this, Observer(::onMessage))
        viewModel.loading().observe(this, Observer(::onLoading))
        viewModel.authenticated().observe(this) { openHomeActivity() }
        viewModel.accounts().observe(this, Observer(::renderAccounts))
        viewModel.initialize(intent?.extras)

        setupUi()
    }

    private fun setupUi() {
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
    }

    private fun renderAccounts(accounts: List<PublicAccount>) {
        if (accounts.isEmpty()) {
            tvAccount.text = getString(R.string.no_accounts_label)
            return
        }

        if (viewModel.mode == AccountsMode.ACCOUNTS) {
            tvAccount.text = getString(R.string.account_label)
        } else {
            tvAccount.text = getString(R.string.select_account_label)
        }

        adapter.setItems(accounts)
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
        const val EXTRA_MODE = "extra_mode"

        fun openAccountsActivity(activity: AppCompatActivity, mode: AccountsMode) {
            activity.startActivity(
                Intent(activity, AccountsActivity::class.java)
                    .putExtra(EXTRA_MODE, mode)
            )
        }
    }
}
