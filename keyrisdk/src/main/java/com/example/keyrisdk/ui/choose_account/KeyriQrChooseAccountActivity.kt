package com.example.keyrisdk.ui.choose_account

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.keyrisdk.databinding.KeyriActivityQrChooseAccountBinding
import com.example.keyrisdk.entity.PublicAccount

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

    private lateinit var binding: KeyriActivityQrChooseAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = KeyriActivityQrChooseAccountBinding.inflate(layoutInflater)

        setContentView(binding.root)
        initUI()
        viewModel.accounts().observe(this, ::setAccounts)
        viewModel.message().observe(this, ::onMessage)
        viewModel.loading().observe(this, ::onLoading)
    }

    private fun initUI() {
        binding.rvAccounts.adapter = adapter
        viewModel.getAccounts()
    }

    private fun setAccounts(accounts: List<PublicAccount>) {
        adapter.submitList(accounts)
    }

    private fun onMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun onLoading(isLoading: Boolean) {
        with(binding) {
            if (isLoading) {
                rlRoot.visibility = View.INVISIBLE
                flProgress.progress.visibility = View.VISIBLE
            } else {
                rlRoot.visibility = View.VISIBLE
                flProgress.progress.visibility = View.INVISIBLE
            }
        }
    }

    companion object {
        const val KEY_SESSION_ID = "KEY_SESSION_ID"
        const val KEY_USERNAME = "KEY_USERNAME"
        const val KEY_CUSTOM = "KEY_CUSTOM"
    }
}
