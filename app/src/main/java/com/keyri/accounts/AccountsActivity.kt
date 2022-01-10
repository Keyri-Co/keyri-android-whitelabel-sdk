package com.keyri.accounts

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.keyrico.keyrisdk.entity.PublicAccount
import com.keyri.home.HomeActivity
import com.keyri.R
import com.keyri.databinding.ActivityAccountsBinding
import org.koin.android.viewmodel.ext.android.viewModel

class AccountsActivity : AppCompatActivity() {

    private val viewModel by viewModel<AccountsVM>()
    private val adapter by lazy { AccountsAdapter(this) { viewModel.processUserAccount(it) } }

    private lateinit var binding: ActivityAccountsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        viewModel.message().observe(this, Observer(::onMessage))
        viewModel.loading().observe(this, Observer(::onLoading))
        viewModel.authenticated().observe(this) { openHomeActivity() }
        viewModel.accounts().observe(this, Observer(::renderAccounts))
        viewModel.initialize(intent?.extras)

        setupUi()
    }

    private fun setupUi() {
        with(binding) {
            list.layoutManager = LinearLayoutManager(this@AccountsActivity)
            list.adapter = adapter
        }
    }

    private fun renderAccounts(accounts: List<PublicAccount>) {
        if (accounts.isEmpty()) {
            binding.tvAccount.text = getString(R.string.no_accounts_label)
            return
        }

        if (viewModel.mode == AccountsMode.ACCOUNTS) {
            binding.tvAccount.text = getString(R.string.account_label)
        } else {
            binding.tvAccount.text = getString(R.string.select_account_label)
        }

        adapter.submitList(accounts)
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
        const val EXTRA_MODE = "extra_mode"

        fun openAccountsActivity(activity: AppCompatActivity, mode: AccountsMode) {
            activity.startActivity(
                Intent(activity, AccountsActivity::class.java)
                    .putExtra(EXTRA_MODE, mode)
            )
        }
    }
}
