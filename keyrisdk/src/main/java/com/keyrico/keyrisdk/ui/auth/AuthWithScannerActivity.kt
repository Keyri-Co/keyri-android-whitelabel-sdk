package com.keyrico.keyrisdk.ui.auth

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.keyrico.keyrisdk.EasyKeyriAuthParams
import com.keyrico.keyrisdk.KeyriSdk
import com.keyrico.keyrisdk.databinding.ActivityAuthWithScannerBinding
import com.keyrico.keyrisdk.ui.confirmation.ConfirmationBottomDialog
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import kotlin.math.abs

internal class AuthWithScannerActivity : AppCompatActivity() {

    private val displayManager by lazy { getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = window.decorView.let { view ->
            if (displayId == this@AuthWithScannerActivity.displayId) {
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

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openScanner()
            }
        }

    private lateinit var binding: ActivityAuthWithScannerBinding

    private val viewModel by viewModels<AuthWithScannerVM>()

    private val keyriSdk by lazy {
        val params = intent.getParcelableExtra<EasyKeyriAuthParams>(KEY_AUTH_PARAMS)
        val appKey = params?.appKey ?: ""
        val rpPublicKey = params?.rpPublicKey ?: ""
        val serviceDomain = params?.serviceDomain ?: ""

        KeyriSdk(this, appKey, rpPublicKey, serviceDomain)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthWithScannerBinding.inflate(layoutInflater)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        initUI()
        observeViewModel()
        openScanner()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    private fun initUI() {
        val ivClose = binding.fabClose
        val topCloseMargin = ivClose.marginTop

        ViewCompat.setOnApplyWindowInsetsListener(ivClose) { v, windowInsets ->
            val topInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val bottomInsets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            v.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = topCloseMargin + topInsets
            }

            binding.vInsetPlaceholder.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = bottomInsets
            }

            windowInsets
        }

        ivClose.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect { uiState ->
                    val isLoading = when (uiState) {
                        is AuthWithScannerState.Confirmation -> processConfirmationMessage(uiState)
                        is AuthWithScannerState.Authenticated -> {
                            setResult(RESULT_OK)
                            finish()

                            false
                        }
                        is AuthWithScannerState.Error -> {
                            Toast.makeText(
                                this@AuthWithScannerActivity,
                                uiState.message,
                                Toast.LENGTH_LONG
                            ).show()

                            false
                        }
                        is AuthWithScannerState.Loading -> true
                        is AuthWithScannerState.Empty -> false
                    }

                    binding.vProgress.isVisible = isLoading
                }
            }
        }
    }

    private fun processConfirmationMessage(uiState: AuthWithScannerState.Confirmation): Boolean {
        ConfirmationBottomDialog(uiState.session) { isAccepted ->
            if (isAccepted) {
                val params = intent.getParcelableExtra<EasyKeyriAuthParams>(KEY_AUTH_PARAMS)
                val publicUserId = params?.publicUserId ?: ""
                val publicCustom = params?.publicCustom ?: ""
                val secureCustom = params?.secureCustom ?: ""

                viewModel.challengeSession(publicUserId, publicCustom, secureCustom, keyriSdk)
            } else {
                setResult(RESULT_CANCELED)
                finish()
            }
        }.show(supportFragmentManager, ConfirmationBottomDialog::class.java.name)

        return true
    }

    private fun openScanner() {
        if (!hasCameraPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        initCamera()
    }

    private fun initCamera() {
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
        val displayMetrics = Resources.getSystem().displayMetrics
        val widthPixels = displayMetrics.widthPixels
        val heightPixels = displayMetrics.heightPixels

        val screenAspectRatio = aspectRatio(widthPixels, heightPixels)
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

        imageAnalyzer?.setAnalyzer(cameraExecutor, initQrAnalyzer())
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
        imageProxy.image?.takeIf { viewModel.uiState.value is AuthWithScannerState.Empty }?.let {
            val image =
                InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees)

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

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = width.coerceAtLeast(height).toDouble() / width.coerceAtMost(height)

        return if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun processScannedData(scannedData: String) {
        Log.d("Keyri", "QR processed: $scannedData")

        try {
            processLink(scannedData.toUri())
        } catch (e: java.lang.Exception) {
            Log.d("Keyri", "Not valid link: $scannedData")
        }
    }

    private fun processLink(uri: Uri?) {
        uri?.getQueryParameters("sessionId")?.firstOrNull()?.let { sessionId ->
            viewModel.handleSessionId(sessionId, keyriSdk)
        } ?: Log.e("Keyri", "Failed to process link")
    }

    companion object {
        const val KEY_AUTH_PARAMS = "KEY_AUTH_PARAMS"

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
