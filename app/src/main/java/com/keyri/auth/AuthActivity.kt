package com.keyri.auth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.keyri.R
import com.keyri.accounts.AccountsActivity
import com.keyri.accounts.AccountsMode
import com.keyri.accounts.NewAccountActivity
import com.keyri.auth_with_scanner.AuthWithScannerActivity
import com.keyri.databinding.ActivityAuthBinding
import com.keyri.home.HomeActivity
import com.keyri.secure_custom.InputSecureCustomActivity
import com.keyrico.keyrisdk.KeyriSdk
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.Executors
import kotlin.math.abs

class AuthActivity : AppCompatActivity() {

    private val viewModel by viewModel<AuthVM>()

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
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) openScanner() else finish()
        }

    private val requestPermissionCustomLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) getSecureCustom() else finish()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                KeyriSdk.AUTH_REQUEST_CODE -> onMessage(getString(R.string.message_authenticated))
                REQUEST_SECURE_CUSTOM -> initCamera(data?.getStringExtra(SECURE_CUSTOM))
            }
        if (resultCode == Activity.RESULT_OK && requestCode == AUTH_REQUEST_CODE) {
            onMessage(getString(R.string.message_authenticated))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    private fun initializeUi() {
        with(binding) {
            bAuthQr.setOnClickListener { viewModel.authWithScanner(this@AuthActivity, AUTH_REQUEST_CODE) }
            bKeyriView.setOnClickListener {
                AuthWithScannerActivity.openAuthWithScannerActivity(this@AuthActivity, CUSTOM)
            }
            bSignup.setOnClickListener { openScanner() }
            bLogin.setOnClickListener { openScanner() }
            bWhitelabelAuth.setOnClickListener {
                if (!hasCameraPermission()) {
                    requestPermissionCustomLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    getSecureCustom()
                }
            }
            bSignupMobile.setOnClickListener { NewAccountActivity.openNewAccountActivity(this@AuthActivity) }
            bLoginMobile.setOnClickListener { openAccountsActivity(AccountsMode.LOGIN) }
            bAccounts.setOnClickListener { openAccountsActivity(AccountsMode.ACCOUNTS) }
            bRemoveAccount.setOnClickListener { openAccountsActivity(AccountsMode.REMOVE) }
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

    private fun getSecureCustom() {
        startActivityForResult(
            Intent(this@AuthActivity, InputSecureCustomActivity::class.java),
            REQUEST_SECURE_CUSTOM
        )
    }

    private fun initCamera(secureCustom: String? = null) {
        binding.scannerPreview.isGone = false
        binding.actionsPanel.isGone = true

        displayManager.registerDisplayListener(displayListener, null)

        binding.scannerPreview.post {
            displayId = binding.scannerPreview.display.displayId

            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()

                bindCameraUseCases(secureCustom)
            }, ContextCompat.getMainExecutor(this))
        }
    }

    private fun bindCameraUseCases(secureCustom: String?) {
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
            .setTargetResolution(Size(320, 320))
            .setTargetRotation(rotation)
            .build()

        val analyzer = if (secureCustom != null) {
            initWhitelabelAnalyzer(secureCustom)
        } else {
            initQrAnalyzer()
        }

        imageAnalyzer?.setAnalyzer(cameraExecutor, analyzer)
        cameraProvider?.unbindAll()

        try {
            camera = cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            preview?.setSurfaceProvider(binding.scannerPreview.surfaceProvider)
        } catch (exc: Exception) {
            Log.d("Keyri", "Failed to init Camera")
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun initQrAnalyzer(): ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { imageProxy ->
        imageProxy.image?.takeIf { viewModel.loading().value != true }?.let { mediaImage ->
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            BarcodeScanning.getClient(options).process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()
                        ?.displayValue
                        ?.let(::processScannedData)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } ?: imageProxy.close()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun initWhitelabelAnalyzer(secureCustom: String?): ImageAnalysis.Analyzer =
        ImageAnalysis.Analyzer { imageProxy ->
            imageProxy.image?.takeIf { viewModel.loading().value != true }?.let { mediaImage ->
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                BarcodeScanning.getClient(options).process(image)
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()
                            ?.displayValue
                            ?.let { processScannedData(it, secureCustom) }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } ?: imageProxy.close()
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

    private fun processScannedData(scannedData: String, secureCustom: String? = null) {
        Log.d("Keyri", "QR processed: $scannedData")

        try {
            // Try to parse link and process it
            processLink(scannedData.toUri(), secureCustom)
        } catch (e: java.lang.Exception) {
            Log.d("Keyri", "Not valid link: $scannedData")
        }
    }

    private fun processLink(uri: Uri?, secureCustom: String? = null) {
        uri?.getQueryParameters("sessionId")?.firstOrNull()?.let { sessionId ->
            val extensionKey = uri.getQueryParameters("aesKey")?.firstOrNull()

            cameraProvider?.unbindAll()
            viewModel.authenticate(sessionId, secureCustom, extensionKey)
        } ?: Log.e("Keyri", "Failed to process link")
    }

    companion object {
        const val REQUEST_SECURE_CUSTOM = 125

        const val SECURE_CUSTOM = "SECURE_CUSTOM"
        private const val CUSTOM = "test custom data"

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        const val AUTH_REQUEST_CODE = 953
    }
}
