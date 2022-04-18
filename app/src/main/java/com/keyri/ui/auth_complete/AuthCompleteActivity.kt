package com.keyri.ui.auth_complete

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.keyri.R
import com.keyri.databinding.ActivityAuthCompleteBinding

class AuthCompleteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthCompleteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthCompleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val resId = if (intent.getBooleanExtra(KEY_IS_SUCCESS, false)) {
            R.string.keyri_auth_complete
        } else {
            R.string.keyri_err_authorization
        }

        binding.tvResultText.setText(resId)
    }

    companion object {
        const val KEY_IS_SUCCESS = "KEY_IS_SUCCESS"
    }
}
