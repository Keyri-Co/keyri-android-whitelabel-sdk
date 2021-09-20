package com.keyri.auth

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.keyri.HomeActivity
import com.keyri.R
import com.keyri.accounts.AccountsActivity
import com.keyri.accounts.AccountsMode
import com.keyri.accounts.NewAccountActivity
import kotlinx.android.synthetic.main.activity_auth.*
import kotlinx.android.synthetic.main.layout_progress.*

class AuthActivity : AppCompatActivity() {

    private val viewModel by viewModels<AuthVM>()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        viewModel.message().observe(this, Observer(::onMessage))
        viewModel.loading().observe(this, Observer(::onLoading))
        viewModel.authenticated().observe(this, Observer{
            HomeActivity.openHomeActivity(
                this
            )
        })

        initializeUi()
    }

    private fun initializeUi() {
        initializeCodeScanner()

        btSignup.setOnClickListener { openScanner() }
        btLogin.setOnClickListener { openScanner() }

        btSignupMobile.setOnClickListener { NewAccountActivity.openNewAccountActivity(this) }
        btLoginMobile.setOnClickListener { openAccountsActivity(AccountsMode.LOGIN) }
        btAccounts.setOnClickListener { openAccountsActivity(AccountsMode.ACCOUNTS) }
    }

    private fun openAccountsActivity(mode: AccountsMode) {
        AccountsActivity.openAccountsActivity(this, mode)
    }

    private fun initializeCodeScanner() {
        codeScanner = CodeScanner(this, scannerView)

        codeScanner.decodeCallback = DecodeCallback {
            isCodeScannerActive = false
            runOnUiThread {
                scannerView.visibility = View.INVISIBLE
                actionsPanel.visibility = View.VISIBLE
                viewModel.authenticate(it.text)
            }
        }
        codeScanner.errorCallback = ErrorCallback {
            isCodeScannerActive = false
            runOnUiThread {
                scannerView.visibility = View.INVISIBLE
                actionsPanel.visibility = View.VISIBLE
                Log.d("Keyri", "Camera initialization error: ${it.message}")
            }
        }
    }

    private fun openScanner() {
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        isCodeScannerActive = true
        scannerView.visibility = View.VISIBLE
        actionsPanel.visibility = View.INVISIBLE
        codeScanner.startPreview()
    }

    override fun onResume() {
        super.onResume()

        if (isCodeScannerActive) {
            scannerView.visibility = View.VISIBLE
            actionsPanel.visibility = View.INVISIBLE
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

    override fun onBackPressed() {
        if (isCodeScannerActive) {
            codeScanner.releaseResources()
            scannerView.visibility = View.INVISIBLE
            actionsPanel.visibility = View.VISIBLE
        } else
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

}
