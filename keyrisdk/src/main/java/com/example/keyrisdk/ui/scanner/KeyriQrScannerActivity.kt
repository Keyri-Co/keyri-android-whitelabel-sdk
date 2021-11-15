package com.example.keyrisdk.ui.scanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.example.keyrisdk.R
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.ui.choose_account.KeyriQrChooseAccountActivity
import kotlinx.android.synthetic.main.keyri_activity_qr_scanner.*
import kotlinx.android.synthetic.main.keyri_layout_progress.*

class KeyriQrScannerActivity : AppCompatActivity() {

    private val viewModel by viewModels<KeyriQrScannerVM>()

    private var isCodeScannerActive = false
    private lateinit var codeScanner: CodeScanner

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openScanner()
            } else finish()
        }

    private val accountChooser =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data

                val sessionId = intent?.getStringExtra(KeyriQrChooseAccountActivity.KEY_SESSION_ID)
                val username = intent?.getStringExtra(KeyriQrChooseAccountActivity.KEY_USERNAME)
                val custom = intent?.getStringExtra(KeyriQrChooseAccountActivity.KEY_CUSTOM)

                if (sessionId != null && username != null) {
                    viewModel.authenticate(sessionId, PublicAccount(username, custom))
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.keyri_activity_qr_scanner)

        viewModel.message().observe(this, ::onMessage)
        viewModel.loading().observe(this, ::onLoading)
        viewModel.completed().observe(this) { finish() }
        viewModel.accountAlreadyExists().observe(this) { sessionId ->
            AlertDialog.Builder(this)
                .setTitle(R.string.keyri_you_already_have_account)
                .setMessage(R.string.keyri_do_you_want_to_replace_account)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    viewModel.removeExistingAccountAndInitNewSession(sessionId)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
                .show()
        }
        viewModel.chooseAccount().observe(this, ::onAccountReceived)
        viewModel.initialize(intent?.extras)

        initializeCodeScanner()
    }

    private fun initializeCodeScanner() {
        codeScanner = CodeScanner(this, scannerView)

        codeScanner.decodeCallback = DecodeCallback {
            isCodeScannerActive = false
            runOnUiThread {
                scannerView.visibility = View.INVISIBLE
                viewModel.authenticate(it.text)
            }
        }
        codeScanner.errorCallback = ErrorCallback {
            isCodeScannerActive = false
            runOnUiThread {
                scannerView.visibility = View.INVISIBLE
                Log.d("Keyri", "Camera initialization error: ${it.message}")
            }
        }

        openScanner()
    }

    private fun openScanner() {
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        isCodeScannerActive = true
        scannerView.visibility = View.VISIBLE
        codeScanner.startPreview()
    }

    override fun onResume() {
        super.onResume()

        if (isCodeScannerActive) {
            scannerView.visibility = View.VISIBLE
            codeScanner.startPreview()
        }
    }

    override fun onPause() {
        if (isCodeScannerActive) codeScanner.releaseResources()
        super.onPause()
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

    private fun onAccountReceived(sessionId: String) {
        val intent = Intent(this, KeyriQrChooseAccountActivity::class.java)
            .apply { putExtra(KeyriQrChooseAccountActivity.KEY_SESSION_ID, sessionId) }

        accountChooser.launch(intent)
    }

    override fun onBackPressed() {
        codeScanner.releaseResources()
        viewModel.cancelAuth()
        super.onBackPressed()
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED


    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    companion object {
        const val ARG_CUSTOM = "ARG_CUSTOM"
    }
}
