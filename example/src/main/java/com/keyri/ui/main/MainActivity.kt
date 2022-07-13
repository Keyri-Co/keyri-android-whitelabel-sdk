package com.keyri.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.keyri.databinding.ActivityAuthBinding
import com.keyri.ui.complete.AuthCompleteActivity
import com.keyrico.keyrisdk.ui.auth.AuthWithScannerActivity

class MainActivity : AppCompatActivity() {

    private val easyKeyriAuthLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val intent = Intent(this, AuthCompleteActivity::class.java).apply {
                putExtra(AuthCompleteActivity.KEY_IS_SUCCESS, it.resultCode == Activity.RESULT_OK)
            }

            startActivity(intent)
        }

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bAuthQr.setOnClickListener {
            val intent = Intent(this, AuthWithScannerActivity::class.java).apply {
                putExtra(AuthWithScannerActivity.APP_KEY, "IT7VrTQ0r4InzsvCNJpRCRpi1qzfgpaj")
                putExtra(AuthWithScannerActivity.PUBLIC_USER_ID, "public-User-ID")
                putExtra(AuthWithScannerActivity.PAYLOAD, "Custom payload here")
            }

            easyKeyriAuthLauncher.launch(intent)
        }
    }
}
