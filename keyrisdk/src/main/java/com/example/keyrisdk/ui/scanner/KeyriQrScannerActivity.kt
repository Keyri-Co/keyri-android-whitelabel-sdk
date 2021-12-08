package com.example.keyrisdk.ui.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.keyrisdk.R
import com.example.keyrisdk.entity.PublicAccount
import com.example.keyrisdk.ui.choose_account.KeyriQrChooseAccountActivity
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.android.synthetic.main.keyri_activity_qr_scanner.*
import kotlinx.android.synthetic.main.keyri_layout_progress.*
import java.util.concurrent.Executors
import kotlin.math.abs

class KeyriQrScannerActivity : AppCompatActivity() {

    private val viewModel by viewModels<KeyriQrScannerVM>()

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
            } else {
                openScanner()
            }
        }

    private val displayManager by lazy { getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = window.decorView.let { view ->
            if (displayId == this@KeyriQrScannerActivity.displayId) {
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        }
    }

    private var displayId: Int = -1
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null

    private val options by lazy {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
            .build()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private val qrAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
        imageProxy.image?.let { mediaImage ->
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            BarcodeScanning.getClient(options).process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()
                        ?.displayValue
                        ?.takeIf { viewModel.loading().value != true }
                        ?.let { sessionId ->
                            Log.d("Keyri", "QR processed: $sessionId")

                            cameraProvider?.unbindAll()
                            viewModel.authenticate(sessionId)
                        }
                }
                .addOnCompleteListener {
                    imageProxy.close()
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

        openScanner()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onBackPressed() {
        viewModel.cancelAuth()
        super.onBackPressed()
    }

    private fun initCamera() {
        displayManager.registerDisplayListener(displayListener, null)

        scannerPreview.post {
            displayId = scannerPreview.display.displayId

            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()

                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(this))
        }
    }

    private fun bindCameraUseCases() {
        val metrics = DisplayMetrics().also { scannerPreview.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = scannerPreview.display.rotation
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(480, 480))
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor, qrAnalyzer)

        cameraProvider?.unbindAll()

        try {
            camera = cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            preview?.setSurfaceProvider(scannerPreview.surfaceProvider)
        } catch (exc: Exception) {
            Log.d("Keyri", "Failed to init Camera")
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = width.coerceAtLeast(height).toDouble() / width.coerceAtMost(height)

        return if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    private fun openScanner() {
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        initCamera()
    }

    private fun onMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun onLoading(isLoading: Boolean) {
        if (isLoading) {
            panelContent.visibility = View.GONE
            progress.visibility = View.VISIBLE

            imageAnalyzer?.clearAnalyzer()
        } else {
            panelContent.visibility = View.VISIBLE
            progress.visibility = View.GONE

            imageAnalyzer?.setAnalyzer(cameraExecutor, qrAnalyzer)
        }
    }

    private fun onAccountReceived(sessionId: String) {
        val intent = Intent(this, KeyriQrChooseAccountActivity::class.java)
            .apply { putExtra(KeyriQrChooseAccountActivity.KEY_SESSION_ID, sessionId) }

        accountChooser.launch(intent)
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

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
