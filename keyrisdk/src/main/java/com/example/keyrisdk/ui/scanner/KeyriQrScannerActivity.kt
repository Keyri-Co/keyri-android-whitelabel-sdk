package com.example.keyrisdk.ui.scanner

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
import com.example.keyrisdk.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.keyri_activity_qr_scanner)

        viewModel.message().observe(this, Observer(::onMessage))
        viewModel.loading().observe(this, Observer(::onLoading))
        viewModel.completed().observe(this, Observer { finish() })
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
