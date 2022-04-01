package com.keyrico.keyrisdk.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.keyrico.keyrisdk.databinding.ActivityConfirmationBinding

class ConfirmationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeUi()
    }

    private fun initializeUi() {
        with(binding) {
            bDecline.setOnClickListener {
                finish()
            }

            bAccept.setOnClickListener {
                // TODO confirmed
            }
        }

        // TODO Add impl
    }
}
