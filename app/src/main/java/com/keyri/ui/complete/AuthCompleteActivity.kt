package com.keyri.ui.complete

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.keyri.databinding.ActivityAuthCompleteBinding

class AuthCompleteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthCompleteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthCompleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val resultMessage = if (intent.getBooleanExtra(KEY_IS_SUCCESS, false)) {
            "You have been successfully authenticated"
        } else {
            "Unable to authorize"
        }

        binding.tvResultText.text = resultMessage
    }

    companion object {
        const val KEY_IS_SUCCESS = "KEY_IS_SUCCESS"
    }
}
