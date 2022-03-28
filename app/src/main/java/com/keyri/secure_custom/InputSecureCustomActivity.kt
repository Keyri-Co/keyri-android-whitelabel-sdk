package com.keyri.secure_custom

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.keyri.auth.AuthActivity
import com.keyri.databinding.ActivityInputSecureCustomBinding

class InputSecureCustomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInputSecureCustomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputSecureCustomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUi()
    }

    private fun setupUi() {
        binding.etSecureCustom.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveCustom()
                true
            } else {
                false
            }
        }

        binding.btSend.setOnClickListener { saveCustom() }
    }

    private fun saveCustom() {
        val resultIntent = Intent()

        resultIntent.putExtra(AuthActivity.SECURE_CUSTOM, binding.etSecureCustom.text.toString())

        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
