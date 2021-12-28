package com.keyri.auth

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.lifecycle.Observer
import com.example.keyrisdk.KeyriConfig
import com.example.keyrisdk.KeyriSdk
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.keyri.BuildConfig
import com.keyri.HomeActivity
import com.keyri.R
import com.keyri.accounts.AccountsActivity
import com.keyri.accounts.AccountsMode
import com.keyri.accounts.NewAccountActivity
import com.keyri.databinding.ActivityAuthBinding
import java.util.concurrent.Executors
import kotlin.math.abs

class AuthActivity : AppCompatActivity() {

    private val viewModel by viewModels<AuthVM>()

    private val displayManager by lazy { getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = window.decorView.let { view ->
            if (displayId == this@AuthActivity.displayId) {
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openScanner()
            } else finish()
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
                        ?.let(::processScannedData)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent.data?.let(::processLink)

        viewModel.message().observe(this, Observer(::onMessage))
        viewModel.loading().observe(this, Observer(::onLoading))
        viewModel.authenticated().observe(this) { HomeActivity.openHomeActivity(this) }

        initializeUi()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processLink(intent.data)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    private fun initializeUi() {
        with(binding) {
            btAuthQr.setOnClickListener {
                val keyriSdk = KeyriSdk(
                    application, KeyriConfig(
                        appKey = BuildConfig.APP_KEY,
                        publicKey = BuildConfig.PUBLIC_KEY,
                        callbackUrl = BuildConfig.KEYRI_CALLBACK_URL,
                        allowMultipleAccounts = true
                    )
                )

                keyriSdk.authWithScanner(
                    this@AuthActivity, CUSTOM,
                    KeyriSdk.QrAuthCallbacks({
                        HomeActivity.openHomeActivity(this@AuthActivity)
                    }, {
                        onMessage(getString(R.string.not_authenticated))
                    })
                )
            }
            btSignup.setOnClickListener { openScanner() }
            btLogin.setOnClickListener { openScanner() }

            btSignupMobile.setOnClickListener { NewAccountActivity.openNewAccountActivity(this@AuthActivity) }
            btLoginMobile.setOnClickListener { openAccountsActivity(AccountsMode.LOGIN) }
            btAccounts.setOnClickListener { openAccountsActivity(AccountsMode.ACCOUNTS) }
        }
    }

    private fun openAccountsActivity(mode: AccountsMode) {
        AccountsActivity.openAccountsActivity(this, mode)
    }

    private fun openScanner() {
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }

        initCamera()
    }

    private fun initCamera() {
        binding.scannerPreview.isGone = false
        binding.actionsPanel.isGone = true

        displayManager.registerDisplayListener(displayListener, null)

        binding.scannerPreview.post {
            displayId = binding.scannerPreview.display.displayId

            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()

                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(this))
        }
    }

    private fun bindCameraUseCases() {
        val metrics = DisplayMetrics().also { binding.scannerPreview.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = binding.scannerPreview.display.rotation
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

            preview?.setSurfaceProvider(binding.scannerPreview.surfaceProvider)
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

    private fun onMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        with(binding) {
            scannerPreview.isGone = true
            actionsPanel.isGone = false
        }

        imageAnalyzer?.clearAnalyzer()
    }

    private fun onLoading(isLoading: Boolean) {
        with(binding) {
            scannerPreview.isGone = true
            actionsPanel.isGone = false

            imageAnalyzer?.clearAnalyzer()

            if (isLoading) {
                panelContent.visibility = View.GONE
                flProgress.progress.visibility = View.VISIBLE
            } else {
                panelContent.visibility = View.VISIBLE
                flProgress.progress.visibility = View.GONE
            }
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED


    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun processScannedData(scannedData: String) {
        Log.d("Keyri", "QR processed: $scannedData")

        try {
            // Try to parse link and process it
            processLink(scannedData.toUri())
        } catch (e: java.lang.Exception) {
            Log.d("Keyri", "Not valid link: $scannedData")
        }
    }

    private fun processLink(uri: Uri?) {
        uri?.getQueryParameters("sessionId")?.firstOrNull()?.let { sessionId ->
            cameraProvider?.unbindAll()
            viewModel.authenticate(sessionId)
        } ?: Log.e("Keyri", "Failed to process link")
    }

    companion object {
        private const val CUSTOM = "test custom data"

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
